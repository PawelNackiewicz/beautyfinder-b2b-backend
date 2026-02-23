package com.beautyfinder.b2b.application.service

import com.beautyfinder.b2b.domain.appointment.AppointmentStatus
import com.beautyfinder.b2b.domain.service.CannotArchiveServiceWithActiveAppointmentsException
import com.beautyfinder.b2b.domain.service.Duration
import com.beautyfinder.b2b.domain.service.DuplicateCategoryNameException
import com.beautyfinder.b2b.domain.service.DuplicateServiceNameException
import com.beautyfinder.b2b.domain.service.PriceRange
import com.beautyfinder.b2b.domain.service.Service
import com.beautyfinder.b2b.domain.service.ServiceCategory
import com.beautyfinder.b2b.domain.service.ServiceCategoryNotFoundException
import com.beautyfinder.b2b.domain.service.ServiceNotFoundException
import com.beautyfinder.b2b.domain.service.ServiceNotInSalonException
import com.beautyfinder.b2b.domain.service.ServiceStatus
import com.beautyfinder.b2b.domain.service.ServiceValidator
import com.beautyfinder.b2b.domain.service.ServiceVariant
import com.beautyfinder.b2b.domain.service.ServiceVariantNotFoundException
import com.beautyfinder.b2b.domain.service.VariantNotInSalonException
import com.beautyfinder.b2b.domain.service.VariantStatus
import com.beautyfinder.b2b.infrastructure.service.ServiceCacheService
import com.beautyfinder.b2b.infrastructure.service.ServiceCategoryRepository
import com.beautyfinder.b2b.infrastructure.service.ServiceRepository
import com.beautyfinder.b2b.infrastructure.service.ServiceVariantRepository
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@org.springframework.stereotype.Service
class ServiceService(
    private val serviceRepository: ServiceRepository,
    private val variantRepository: ServiceVariantRepository,
    private val categoryRepository: ServiceCategoryRepository,
    private val cacheService: ServiceCacheService,
) {

    private val log = LoggerFactory.getLogger(ServiceService::class.java)

    fun listServices(salonId: UUID, includeInactive: Boolean = false): List<ServiceWithVariantsDto> {
        val services = if (includeInactive) {
            serviceRepository.findAllBySalonIdOrderByDisplayOrderAsc(salonId)
        } else {
            serviceRepository.findAllBySalonIdAndStatusNotOrderByDisplayOrderAsc(salonId, ServiceStatus.ARCHIVED)
                .filter { it.status == ServiceStatus.ACTIVE }
        }

        return services.map { service ->
            val variants = if (includeInactive) {
                variantRepository.findAllByServiceIdOrderByDisplayOrderAsc(service.id!!)
            } else {
                variantRepository.findAllByServiceIdAndStatusOrderByDisplayOrderAsc(service.id!!, VariantStatus.ACTIVE)
            }
            service.toDto(variants)
        }
    }

    fun getService(serviceId: UUID, salonId: UUID): ServiceWithVariantsDto {
        val service = serviceRepository.findByIdAndSalonId(serviceId, salonId)
            ?: throw ServiceNotFoundException(serviceId)
        val variants = variantRepository.findAllByServiceIdOrderByDisplayOrderAsc(serviceId)
        return service.toDto(variants)
    }

    fun getVariant(variantId: UUID, salonId: UUID): ServiceVariantDto {
        cacheService.getVariant(variantId)?.let { cached ->
            if (cached.salonId == salonId) return cached
        }

        val variant = variantRepository.findByIdAndSalonId(variantId, salonId)
            ?: throw ServiceVariantNotFoundException(variantId)

        val dto = variant.toDto()
        cacheService.putVariant(variantId, dto)
        return dto
    }

    @Transactional
    fun createService(request: CreateServiceRequest, salonId: UUID): ServiceWithVariantsDto {
        ServiceValidator.validateServiceName(request.name).getOrThrow()

        if (serviceRepository.existsByNameAndSalonId(request.name, salonId)) {
            throw DuplicateServiceNameException(request.name, salonId)
        }

        request.categoryId?.let { catId ->
            categoryRepository.findByIdAndSalonId(catId, salonId)
                ?: throw ServiceCategoryNotFoundException(catId)
        }

        val maxOrder = serviceRepository.findMaxDisplayOrder(salonId) ?: -1

        val service = Service(
            salonId = salonId,
            name = request.name,
            category = request.category,
            description = request.description,
            displayOrder = maxOrder + 1,
            status = ServiceStatus.ACTIVE,
            isOnlineBookable = request.isOnlineBookable,
            categoryId = request.categoryId,
        )
        val savedService = serviceRepository.save(service)

        val variants = request.variants?.mapIndexed { index, variantRequest ->
            ServiceValidator.validateVariantDuration(variantRequest.durationMinutes).getOrThrow()
            ServiceValidator.validatePrice(variantRequest.price, variantRequest.priceMax).getOrThrow()

            val variant = ServiceVariant(
                serviceId = savedService.id!!,
                salonId = salonId,
                name = variantRequest.name,
                description = variantRequest.description,
                durationMinutes = variantRequest.durationMinutes,
                price = variantRequest.price,
                priceMax = variantRequest.priceMax,
                displayOrder = index,
                status = VariantStatus.ACTIVE,
                isOnlineBookable = variantRequest.isOnlineBookable,
            )
            variantRepository.save(variant)
        } ?: emptyList()

        log.info("Created service {} '{}' for salon {} with {} variants", savedService.id, request.name, salonId, variants.size)
        return savedService.toDto(variants)
    }

    @Transactional
    fun updateService(serviceId: UUID, request: UpdateServiceRequest, salonId: UUID): ServiceWithVariantsDto {
        val service = serviceRepository.findByIdAndSalonId(serviceId, salonId)
            ?: throw ServiceNotFoundException(serviceId)

        request.name?.let { name ->
            ServiceValidator.validateServiceName(name).getOrThrow()
            if (serviceRepository.existsByNameAndSalonIdAndIdNot(name, salonId, serviceId)) {
                throw DuplicateServiceNameException(name, salonId)
            }
            service.name = name
        }
        request.category?.let { service.category = it }
        request.categoryId?.let { catId ->
            categoryRepository.findByIdAndSalonId(catId, salonId)
                ?: throw ServiceCategoryNotFoundException(catId)
            service.categoryId = catId
        }
        request.description?.let { service.description = it }
        request.imageUrl?.let { service.imageUrl = it }
        request.displayOrder?.let {
            ServiceValidator.validateDisplayOrder(it).getOrThrow()
            service.displayOrder = it
        }
        request.isOnlineBookable?.let { service.isOnlineBookable = it }

        val saved = serviceRepository.save(service)
        val variants = variantRepository.findAllByServiceIdOrderByDisplayOrderAsc(serviceId)
        return saved.toDto(variants)
    }

    @Transactional
    fun archiveService(serviceId: UUID, salonId: UUID) {
        val service = serviceRepository.findByIdAndSalonId(serviceId, salonId)
            ?: throw ServiceNotFoundException(serviceId)

        val variants = variantRepository.findAllByServiceIdOrderByDisplayOrderAsc(serviceId)
        val activeStatuses = listOf(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED)
        val now = OffsetDateTime.now()

        var totalFutureAppointments = 0L
        for (variant in variants) {
            totalFutureAppointments += variantRepository.countFutureAppointments(variant.id!!, activeStatuses, now)
        }

        if (totalFutureAppointments > 0) {
            throw CannotArchiveServiceWithActiveAppointmentsException(serviceId, totalFutureAppointments)
        }

        service.status = ServiceStatus.ARCHIVED
        serviceRepository.save(service)

        for (variant in variants) {
            variant.status = VariantStatus.INACTIVE
            variantRepository.save(variant)
            cacheService.evictVariant(variant.id!!)
        }

        log.info("Archived service {} for salon {}", serviceId, salonId)
    }

    @Transactional
    fun createVariant(serviceId: UUID, request: CreateVariantRequest, salonId: UUID): ServiceVariantDto {
        serviceRepository.findByIdAndSalonId(serviceId, salonId)
            ?: throw ServiceNotInSalonException(serviceId, salonId)

        ServiceValidator.validateVariantDuration(request.durationMinutes).getOrThrow()
        ServiceValidator.validatePrice(request.price, request.priceMax).getOrThrow()

        val maxOrder = variantRepository.findMaxDisplayOrder(serviceId) ?: -1

        val variant = ServiceVariant(
            serviceId = serviceId,
            salonId = salonId,
            name = request.name,
            description = request.description,
            durationMinutes = request.durationMinutes,
            price = request.price,
            priceMax = request.priceMax,
            displayOrder = maxOrder + 1,
            status = VariantStatus.ACTIVE,
            isOnlineBookable = request.isOnlineBookable,
        )
        val saved = variantRepository.save(variant)
        cacheService.evictVariant(saved.id!!)

        log.info("Created variant {} for service {} salon {}", saved.id, serviceId, salonId)
        return saved.toDto()
    }

    @Transactional
    fun updateVariant(variantId: UUID, request: UpdateVariantRequest, salonId: UUID): ServiceVariantDto {
        val variant = variantRepository.findByIdAndSalonId(variantId, salonId)
            ?: throw VariantNotInSalonException(variantId, salonId)

        request.name?.let { variant.name = it }
        request.description?.let { variant.description = it }
        request.durationMinutes?.let {
            ServiceValidator.validateVariantDuration(it).getOrThrow()
            variant.durationMinutes = it
        }
        request.price?.let { newPrice ->
            val newMax = request.priceMax ?: variant.priceMax
            ServiceValidator.validatePrice(newPrice, newMax).getOrThrow()
            variant.price = newPrice
        }
        request.priceMax?.let { variant.priceMax = it }
        request.isOnlineBookable?.let { variant.isOnlineBookable = it }
        request.status?.let { variant.status = it }

        val saved = variantRepository.save(variant)
        cacheService.evictVariant(variantId)
        return saved.toDto()
    }

    @Transactional
    fun deactivateVariant(variantId: UUID, salonId: UUID) {
        val variant = variantRepository.findByIdAndSalonId(variantId, salonId)
            ?: throw VariantNotInSalonException(variantId, salonId)

        val activeStatuses = listOf(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED)
        val futureCount = variantRepository.countFutureAppointments(variantId, activeStatuses, OffsetDateTime.now())
        if (futureCount > 0) {
            throw CannotArchiveServiceWithActiveAppointmentsException(variant.serviceId, futureCount)
        }

        variant.status = VariantStatus.INACTIVE
        variantRepository.save(variant)
        cacheService.evictVariant(variantId)

        log.info("Deactivated variant {} for salon {}", variantId, salonId)
    }

    @Transactional
    fun reorderServices(salonId: UUID, orderedIds: List<UUID>) {
        val services = serviceRepository.findAllBySalonIdOrderByDisplayOrderAsc(salonId)
        val serviceIds = services.map { it.id!! }.toSet()

        require(orderedIds.toSet().all { it in serviceIds }) {
            "All IDs must belong to salon $salonId"
        }

        orderedIds.forEachIndexed { index, id ->
            serviceRepository.updateDisplayOrder(id, index)
        }
    }

    @Transactional
    fun reorderVariants(serviceId: UUID, salonId: UUID, orderedIds: List<UUID>) {
        serviceRepository.findByIdAndSalonId(serviceId, salonId)
            ?: throw ServiceNotInSalonException(serviceId, salonId)

        val variants = variantRepository.findAllByServiceIdOrderByDisplayOrderAsc(serviceId)
        val variantIds = variants.map { it.id!! }.toSet()

        require(orderedIds.toSet().all { it in variantIds }) {
            "All variant IDs must belong to service $serviceId"
        }

        orderedIds.forEachIndexed { index, id ->
            variantRepository.updateDisplayOrder(id, index)
        }
    }

    // --- Category operations ---

    fun listCategories(salonId: UUID): List<ServiceCategoryDto> {
        return categoryRepository.findAllBySalonIdOrderByDisplayOrderAsc(salonId)
            .map { it.toDto() }
    }

    @Transactional
    fun createCategory(request: CreateCategoryRequest, salonId: UUID): ServiceCategoryDto {
        if (categoryRepository.existsByNameAndSalonId(request.name, salonId)) {
            throw DuplicateCategoryNameException(request.name, salonId)
        }

        val category = ServiceCategory(
            salonId = salonId,
            name = request.name,
            displayOrder = request.displayOrder ?: 0,
            colorHex = request.colorHex,
            iconName = request.iconName,
        )
        return categoryRepository.save(category).toDto()
    }

    @Transactional
    fun updateCategory(categoryId: UUID, request: UpdateCategoryRequest, salonId: UUID): ServiceCategoryDto {
        val category = categoryRepository.findByIdAndSalonId(categoryId, salonId)
            ?: throw ServiceCategoryNotFoundException(categoryId)

        request.name?.let { name ->
            if (categoryRepository.existsByNameAndSalonIdAndIdNot(name, salonId, categoryId)) {
                throw DuplicateCategoryNameException(name, salonId)
            }
            category.name = name
        }
        request.displayOrder?.let { category.displayOrder = it }
        request.colorHex?.let { category.colorHex = it }
        request.iconName?.let { category.iconName = it }

        return categoryRepository.save(category).toDto()
    }

    @Transactional
    fun deleteCategory(categoryId: UUID, salonId: UUID) {
        val category = categoryRepository.findByIdAndSalonId(categoryId, salonId)
            ?: throw ServiceCategoryNotFoundException(categoryId)

        serviceRepository.clearCategoryForServices(categoryId)
        categoryRepository.delete(category)

        log.info("Deleted category {} for salon {}", categoryId, salonId)
    }

    // --- Mappers ---

    private fun Service.toDto(variants: List<ServiceVariant>): ServiceWithVariantsDto = ServiceWithVariantsDto(
        id = id!!,
        salonId = salonId,
        name = name,
        category = category,
        categoryId = categoryId,
        description = description,
        imageUrl = imageUrl,
        displayOrder = displayOrder,
        status = status,
        isOnlineBookable = isOnlineBookable,
        variants = variants.map { it.toDto() },
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun ServiceVariant.toDto(): ServiceVariantDto {
        val duration = try { Duration(durationMinutes) } catch (_: Exception) { null }
        val priceRange = try { PriceRange(price, priceMax) } catch (_: Exception) { null }

        return ServiceVariantDto(
            id = id!!,
            serviceId = serviceId,
            salonId = salonId,
            name = name,
            description = description,
            durationMinutes = durationMinutes,
            durationFormatted = duration?.formatted() ?: "$durationMinutes min",
            price = price,
            priceMax = priceMax,
            priceFormatted = priceRange?.formatted() ?: "$price PLN",
            displayOrder = displayOrder,
            status = status,
            isOnlineBookable = isOnlineBookable,
        )
    }

    private fun ServiceCategory.toDto(): ServiceCategoryDto = ServiceCategoryDto(
        id = id!!,
        salonId = salonId,
        name = name,
        displayOrder = displayOrder,
        colorHex = colorHex,
        iconName = iconName,
    )
}
