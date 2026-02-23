package com.beautyfinder.b2b.application.service

import com.beautyfinder.b2b.domain.appointment.AppointmentStatus
import com.beautyfinder.b2b.domain.service.CannotArchiveServiceWithActiveAppointmentsException
import com.beautyfinder.b2b.domain.service.DuplicateServiceNameException
import com.beautyfinder.b2b.domain.service.InvalidDurationException
import com.beautyfinder.b2b.domain.service.InvalidPriceException
import com.beautyfinder.b2b.domain.service.Service
import com.beautyfinder.b2b.domain.service.ServiceCategory
import com.beautyfinder.b2b.domain.service.ServiceStatus
import com.beautyfinder.b2b.domain.service.ServiceVariant
import com.beautyfinder.b2b.domain.service.VariantStatus
import com.beautyfinder.b2b.infrastructure.service.ServiceCacheService
import com.beautyfinder.b2b.infrastructure.service.ServiceCategoryRepository
import com.beautyfinder.b2b.infrastructure.service.ServiceRepository
import com.beautyfinder.b2b.infrastructure.service.ServiceVariantRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.UUID

class ServiceServiceTest {

    private val serviceRepository = mockk<ServiceRepository>(relaxed = true)
    private val variantRepository = mockk<ServiceVariantRepository>(relaxed = true)
    private val categoryRepository = mockk<ServiceCategoryRepository>(relaxed = true)
    private val cacheService = mockk<ServiceCacheService>(relaxed = true)

    private val serviceService = ServiceService(
        serviceRepository = serviceRepository,
        variantRepository = variantRepository,
        categoryRepository = categoryRepository,
        cacheService = cacheService,
    )

    private val salonId = UUID.randomUUID()
    private val serviceId = UUID.randomUUID()
    private val variantId = UUID.randomUUID()
    private val categoryId = UUID.randomUUID()

    // --- Helpers ---

    private fun setId(entity: com.beautyfinder.b2b.domain.BaseEntity, id: UUID) {
        val field = com.beautyfinder.b2b.domain.BaseEntity::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(entity, id)
    }

    private fun buildService(
        id: UUID = serviceId,
        name: String = "Strzyżenie",
        status: ServiceStatus = ServiceStatus.ACTIVE,
    ): Service {
        val service = Service(
            salonId = salonId,
            name = name,
            category = "Fryzjerstwo",
            status = status,
        )
        setId(service, id)
        return service
    }

    private fun buildVariant(
        id: UUID = variantId,
        serviceId: UUID = this.serviceId,
        durationMinutes: Int = 30,
        price: BigDecimal = BigDecimal("50.00"),
        status: VariantStatus = VariantStatus.ACTIVE,
    ): ServiceVariant {
        val variant = ServiceVariant(
            serviceId = serviceId,
            salonId = salonId,
            name = "Standard",
            durationMinutes = durationMinutes,
            price = price,
            status = status,
        )
        setId(variant, id)
        return variant
    }

    private fun buildCategory(
        id: UUID = categoryId,
        name: String = "Włosy",
    ): ServiceCategory {
        val cat = ServiceCategory(
            salonId = salonId,
            name = name,
        )
        setId(cat, id)
        return cat
    }

    // --- Tests ---

    @Test
    fun `listServices onlyActive filters archived and inactive`() {
        val active = buildService(status = ServiceStatus.ACTIVE)
        val archived = buildService(id = UUID.randomUUID(), status = ServiceStatus.ARCHIVED)

        every {
            serviceRepository.findAllBySalonIdAndStatusNotOrderByDisplayOrderAsc(salonId, ServiceStatus.ARCHIVED)
        } returns listOf(active)
        every { variantRepository.findAllByServiceIdAndStatusOrderByDisplayOrderAsc(any(), VariantStatus.ACTIVE) } returns emptyList()

        val result = serviceService.listServices(salonId, includeInactive = false)
        assertEquals(1, result.size)
        assertEquals(active.id, result[0].id)
    }

    @Test
    fun `listServices includeInactive returns all`() {
        val active = buildService(status = ServiceStatus.ACTIVE)
        val inactive = buildService(id = UUID.randomUUID(), status = ServiceStatus.INACTIVE)

        every { serviceRepository.findAllBySalonIdOrderByDisplayOrderAsc(salonId) } returns listOf(active, inactive)
        every { variantRepository.findAllByServiceIdOrderByDisplayOrderAsc(any()) } returns emptyList()

        val result = serviceService.listServices(salonId, includeInactive = true)
        assertEquals(2, result.size)
    }

    @Test
    fun `getVariant cache hit does not query db`() {
        val variant = buildVariant()
        val dto = ServiceVariantDto(
            id = variantId, serviceId = serviceId, salonId = salonId,
            name = "Standard", description = null, durationMinutes = 30,
            durationFormatted = "30 min", price = BigDecimal("50.00"), priceMax = null,
            priceFormatted = "50 PLN", displayOrder = 0, status = VariantStatus.ACTIVE,
            isOnlineBookable = true,
        )
        every { cacheService.getVariant(variantId) } returns dto

        val result = serviceService.getVariant(variantId, salonId)
        assertEquals(dto, result)
        verify(exactly = 0) { variantRepository.findByIdAndSalonId(any(), any()) }
    }

    @Test
    fun `getVariant cache miss queries and caches`() {
        val variant = buildVariant()
        every { cacheService.getVariant(variantId) } returns null
        every { variantRepository.findByIdAndSalonId(variantId, salonId) } returns variant

        serviceService.getVariant(variantId, salonId)

        verify(exactly = 1) { cacheService.putVariant(variantId, any()) }
    }

    @Test
    fun `createService success sets active and display order`() {
        every { serviceRepository.existsByNameAndSalonId(any(), any()) } returns false
        every { serviceRepository.findMaxDisplayOrder(salonId) } returns 2
        every { serviceRepository.save(any()) } answers {
            val s = firstArg<Service>()
            setId(s, UUID.randomUUID())
            s
        }

        val request = CreateServiceRequest(name = "Koloryzacja", category = "Fryzjerstwo")
        val result = serviceService.createService(request, salonId)

        assertEquals(ServiceStatus.ACTIVE, result.status)
        assertEquals(3, result.displayOrder)
    }

    @Test
    fun `createService duplicate name throws exception`() {
        every { serviceRepository.existsByNameAndSalonId("Strzyżenie", salonId) } returns true

        val request = CreateServiceRequest(name = "Strzyżenie", category = "Fryzjerstwo")
        assertThrows<DuplicateServiceNameException> {
            serviceService.createService(request, salonId)
        }
    }

    @Test
    fun `createService with variants creates all`() {
        every { serviceRepository.existsByNameAndSalonId(any(), any()) } returns false
        every { serviceRepository.findMaxDisplayOrder(salonId) } returns null
        every { serviceRepository.save(any()) } answers {
            val s = firstArg<Service>()
            setId(s, UUID.randomUUID())
            s
        }
        every { variantRepository.save(any()) } answers {
            val v = firstArg<ServiceVariant>()
            setId(v, UUID.randomUUID())
            v
        }

        val request = CreateServiceRequest(
            name = "Manicure",
            category = "Paznokcie",
            variants = listOf(
                CreateVariantRequest(name = "Basic", durationMinutes = 30, price = BigDecimal("50")),
                CreateVariantRequest(name = "Gel", durationMinutes = 60, price = BigDecimal("100")),
            ),
        )
        val result = serviceService.createService(request, salonId)
        assertEquals(2, result.variants.size)
    }

    @Test
    fun `updateService changes name checks duplicate`() {
        val service = buildService()
        every { serviceRepository.findByIdAndSalonId(serviceId, salonId) } returns service
        every { serviceRepository.existsByNameAndSalonIdAndIdNot("Nowa nazwa", salonId, serviceId) } returns false
        every { serviceRepository.save(any()) } answers { firstArg() }
        every { variantRepository.findAllByServiceIdOrderByDisplayOrderAsc(serviceId) } returns emptyList()

        val result = serviceService.updateService(serviceId, UpdateServiceRequest(name = "Nowa nazwa"), salonId)
        assertEquals("Nowa nazwa", result.name)
    }

    @Test
    fun `archiveService no future appointments archives all`() {
        val service = buildService()
        val variant1 = buildVariant(id = UUID.randomUUID())
        val variant2 = buildVariant(id = UUID.randomUUID())

        every { serviceRepository.findByIdAndSalonId(serviceId, salonId) } returns service
        every { variantRepository.findAllByServiceIdOrderByDisplayOrderAsc(serviceId) } returns listOf(variant1, variant2)
        every { variantRepository.countFutureAppointments(any(), any(), any()) } returns 0L
        every { serviceRepository.save(any()) } answers { firstArg() }
        every { variantRepository.save(any()) } answers { firstArg() }

        serviceService.archiveService(serviceId, salonId)

        assertEquals(ServiceStatus.ARCHIVED, service.status)
        assertEquals(VariantStatus.INACTIVE, variant1.status)
        assertEquals(VariantStatus.INACTIVE, variant2.status)
    }

    @Test
    fun `archiveService has future appointments throws exception`() {
        val service = buildService()
        val variant = buildVariant()

        every { serviceRepository.findByIdAndSalonId(serviceId, salonId) } returns service
        every { variantRepository.findAllByServiceIdOrderByDisplayOrderAsc(serviceId) } returns listOf(variant)
        every { variantRepository.countFutureAppointments(any(), any(), any()) } returns 3L

        assertThrows<CannotArchiveServiceWithActiveAppointmentsException> {
            serviceService.archiveService(serviceId, salonId)
        }
    }

    @Test
    fun `createVariant invalid duration throws exception`() {
        val service = buildService()
        every { serviceRepository.findByIdAndSalonId(serviceId, salonId) } returns service

        val request = CreateVariantRequest(name = "Bad", durationMinutes = 17, price = BigDecimal("50"))
        assertThrows<InvalidDurationException> {
            serviceService.createVariant(serviceId, request, salonId)
        }
    }

    @Test
    fun `createVariant invalid price throws exception`() {
        val service = buildService()
        every { serviceRepository.findByIdAndSalonId(serviceId, salonId) } returns service

        val request = CreateVariantRequest(name = "Bad", durationMinutes = 30, price = BigDecimal("-1"))
        assertThrows<InvalidPriceException> {
            serviceService.createVariant(serviceId, request, salonId)
        }
    }

    @Test
    fun `createVariant evicts cache`() {
        val service = buildService()
        every { serviceRepository.findByIdAndSalonId(serviceId, salonId) } returns service
        every { variantRepository.findMaxDisplayOrder(serviceId) } returns null
        every { variantRepository.save(any()) } answers {
            val v = firstArg<ServiceVariant>()
            setId(v, UUID.randomUUID())
            v
        }

        val request = CreateVariantRequest(name = "Standard", durationMinutes = 30, price = BigDecimal("50"))
        serviceService.createVariant(serviceId, request, salonId)

        verify { cacheService.evictVariant(any()) }
    }

    @Test
    fun `updateVariant evicts cache`() {
        val variant = buildVariant()
        every { variantRepository.findByIdAndSalonId(variantId, salonId) } returns variant
        every { variantRepository.save(any()) } answers { firstArg() }

        serviceService.updateVariant(variantId, UpdateVariantRequest(name = "Premium"), salonId)

        verify { cacheService.evictVariant(variantId) }
    }

    @Test
    fun `deactivateVariant has future appointments throws exception`() {
        val variant = buildVariant()
        every { variantRepository.findByIdAndSalonId(variantId, salonId) } returns variant
        every { variantRepository.countFutureAppointments(variantId, any(), any()) } returns 2L

        assertThrows<CannotArchiveServiceWithActiveAppointmentsException> {
            serviceService.deactivateVariant(variantId, salonId)
        }
    }

    @Test
    fun `reorderServices updates display order`() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val id3 = UUID.randomUUID()
        val services = listOf(buildService(id = id1), buildService(id = id2), buildService(id = id3))

        every { serviceRepository.findAllBySalonIdOrderByDisplayOrderAsc(salonId) } returns services

        serviceService.reorderServices(salonId, listOf(id3, id1, id2))

        verify { serviceRepository.updateDisplayOrder(id3, 0) }
        verify { serviceRepository.updateDisplayOrder(id1, 1) }
        verify { serviceRepository.updateDisplayOrder(id2, 2) }
    }

    @Test
    fun `reorderServices foreign id throws exception`() {
        val id1 = UUID.randomUUID()
        val services = listOf(buildService(id = id1))
        every { serviceRepository.findAllBySalonIdOrderByDisplayOrderAsc(salonId) } returns services

        val foreignId = UUID.randomUUID()
        assertThrows<IllegalArgumentException> {
            serviceService.reorderServices(salonId, listOf(foreignId))
        }
    }

    @Test
    fun `deleteCategory with services sets services category to null`() {
        val category = buildCategory()
        every { categoryRepository.findByIdAndSalonId(categoryId, salonId) } returns category

        serviceService.deleteCategory(categoryId, salonId)

        verify { serviceRepository.clearCategoryForServices(categoryId) }
        verify { categoryRepository.delete(category) }
    }
}
