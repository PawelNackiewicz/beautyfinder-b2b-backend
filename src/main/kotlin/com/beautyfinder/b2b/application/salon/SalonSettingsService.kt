package com.beautyfinder.b2b.application.salon

import com.beautyfinder.b2b.domain.Salon
import com.beautyfinder.b2b.domain.client.EncryptionService
import com.beautyfinder.b2b.domain.salon.BusinessHours
import com.beautyfinder.b2b.domain.salon.InvalidLoyaltyConfigException
import com.beautyfinder.b2b.domain.salon.LoyaltyConfig
import com.beautyfinder.b2b.domain.salon.SalonIntegrationSettings
import com.beautyfinder.b2b.domain.salon.SalonNotFoundException
import com.beautyfinder.b2b.domain.salon.SalonNotificationSettings
import com.beautyfinder.b2b.domain.salon.SalonOpeningHours
import com.beautyfinder.b2b.domain.salon.SalonSettingsValidator
import com.beautyfinder.b2b.domain.salon.SalonSlugAlreadyExistsException
import com.beautyfinder.b2b.infrastructure.salon.SalonCacheService
import com.beautyfinder.b2b.infrastructure.salon.SalonIntegrationSettingsRepository
import com.beautyfinder.b2b.infrastructure.salon.SalonNotificationSettingsRepository
import com.beautyfinder.b2b.infrastructure.salon.SalonOpeningHoursRepository
import com.beautyfinder.b2b.infrastructure.salon.SalonRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class SalonSettingsService(
    private val salonRepository: SalonRepository,
    private val openingHoursRepository: SalonOpeningHoursRepository,
    private val notificationSettingsRepository: SalonNotificationSettingsRepository,
    private val integrationSettingsRepository: SalonIntegrationSettingsRepository,
    private val cacheService: SalonCacheService,
    private val storageService: StorageService,
    private val eventPublisher: ApplicationEventPublisher,
    private val encryptionService: EncryptionService,
) {
    private val log = LoggerFactory.getLogger(SalonSettingsService::class.java)

    private val allowedImageTypes = setOf("image/jpeg", "image/png", "image/webp")
    private val maxLogoSize = 2 * 1024 * 1024L // 2MB

    @Transactional(readOnly = true)
    fun getSalonSettings(salonId: UUID): SalonSettingsDto {
        cacheService.get(salonId)?.let { return it }

        val salon = findSalonOrThrow(salonId)
        val openingHours = openingHoursRepository.findAllBySalonIdOrderByDayOfWeek(salonId)
        val notifications = notificationSettingsRepository.findBySalonId(salonId)
        val integrations = integrationSettingsRepository.findBySalonId(salonId)

        val dto = mapToSettingsDto(salon, openingHours, notifications, integrations)
        cacheService.put(salonId, dto)
        return dto
    }

    @Transactional
    fun updateGeneralSettings(salonId: UUID, request: UpdateGeneralSettingsRequest): SalonSettingsDto {
        val salon = findSalonOrThrow(salonId)
        val changedFields = mutableListOf<String>()

        request.slug?.let { slug ->
            if (slug != salon.slug && salonRepository.existsBySlugAndIdNot(slug, salonId)) {
                throw SalonSlugAlreadyExistsException(slug)
            }
            salon.slug = slug
            changedFields.add("slug")
        }
        request.name?.let { salon.name = it; changedFields.add("name") }
        request.timezone?.let {
            SalonSettingsValidator.validateTimezone(it).getOrThrow()
            salon.timezone = it; changedFields.add("timezone")
        }
        request.cancellationWindowHours?.let {
            SalonSettingsValidator.validateCancellationWindow(it).getOrThrow()
            salon.cancellationWindowHours = it; changedFields.add("cancellationWindowHours")
        }
        request.slotIntervalMinutes?.let {
            SalonSettingsValidator.validateSlotInterval(it).getOrThrow()
            salon.slotIntervalMinutes = it; changedFields.add("slotIntervalMinutes")
        }
        request.bookingWindowDays?.let {
            SalonSettingsValidator.validateBookingWindow(it).getOrThrow()
            salon.bookingWindowDays = it; changedFields.add("bookingWindowDays")
        }
        request.currency?.let { salon.currency = it; changedFields.add("currency") }
        request.defaultAppointmentBufferMinutes?.let {
            salon.defaultAppointmentBufferMinutes = it; changedFields.add("defaultAppointmentBufferMinutes")
        }
        request.phone?.let { salon.phone = it; changedFields.add("phone") }
        request.email?.let { salon.email = it; changedFields.add("email") }
        request.description?.let { salon.description = it; changedFields.add("description") }
        request.websiteUrl?.let { salon.websiteUrl = it; changedFields.add("websiteUrl") }
        request.street?.let { salon.street = it; changedFields.add("street") }
        request.city?.let { salon.city = it; changedFields.add("city") }
        request.postalCode?.let { salon.postalCode = it; changedFields.add("postalCode") }

        salonRepository.save(salon)
        cacheService.evict(salonId)

        if (changedFields.isNotEmpty()) {
            eventPublisher.publishEvent(SalonSettingsUpdatedEvent(salonId, changedFields))
        }

        return getSalonSettings(salonId)
    }

    @Transactional
    fun updateOpeningHours(salonId: UUID, request: List<UpdateOpeningHoursRequest>): List<SalonOpeningHoursDto> {
        findSalonOrThrow(salonId)

        val hours = request.map { req ->
            SalonOpeningHours(
                salonId = salonId,
                dayOfWeek = req.dayOfWeek,
                isOpen = req.isOpen,
                openTime = req.openTime,
                closeTime = req.closeTime,
                breakStart = req.breakStart,
                breakEnd = req.breakEnd,
            )
        }

        SalonSettingsValidator.validateOpeningHours(hours).getOrThrow()
        BusinessHours(hours).validate().getOrThrow()

        openingHoursRepository.deleteAllBySalonId(salonId)
        val saved = openingHoursRepository.saveAll(hours)

        cacheService.evict(salonId)
        eventPublisher.publishEvent(SalonOpeningHoursUpdatedEvent(salonId))

        return saved.map { it.toDto() }
    }

    @Transactional
    fun updateInvoicingSettings(salonId: UUID, request: UpdateInvoicingSettingsRequest): SalonSettingsDto {
        val salon = findSalonOrThrow(salonId)

        request.taxId?.let {
            SalonSettingsValidator.validateTaxId(it, salon.country).getOrThrow()
        }

        request.invoicingName?.let { salon.invoicingName = it }
        request.taxId?.let { salon.taxId = it }
        request.invoicingStreet?.let { salon.invoicingStreet = it }
        request.invoicingCity?.let { salon.invoicingCity = it }
        request.invoicingPostalCode?.let { salon.invoicingPostalCode = it }
        request.bankAccountNumber?.let { salon.bankAccountNumber = it }
        request.invoiceFooterNotes?.let { salon.invoiceFooterNotes = it }

        salonRepository.save(salon)
        cacheService.evict(salonId)

        return getSalonSettings(salonId)
    }

    @Transactional
    fun updateLoyaltySettings(salonId: UUID, request: UpdateLoyaltySettingsRequest): SalonSettingsDto {
        val salon = findSalonOrThrow(salonId)

        val config = LoyaltyConfig(
            enabled = request.enabled,
            pointsPerVisit = request.pointsPerVisit,
            pointsPerCurrencyUnit = request.pointsPerCurrencyUnit,
            redemptionRate = request.redemptionRate,
            expireDays = request.expireDays,
        )
        config.validate().getOrThrow()

        val wasDisabled = !salon.loyaltyEnabled

        salon.loyaltyEnabled = request.enabled
        salon.pointsPerVisit = request.pointsPerVisit
        salon.pointsPerCurrencyUnit = request.pointsPerCurrencyUnit
        salon.pointsRedemptionRate = request.redemptionRate
        salon.loyaltyPointsExpireDays = request.expireDays

        salonRepository.save(salon)
        cacheService.evict(salonId)

        if (wasDisabled && request.enabled) {
            eventPublisher.publishEvent(LoyaltyProgramEnabledEvent(salonId))
        }

        return getSalonSettings(salonId)
    }

    @Transactional
    fun updateNotificationSettings(salonId: UUID, request: UpdateNotificationSettingsRequest): SalonNotificationSettingsDto {
        findSalonOrThrow(salonId)

        val existing = notificationSettingsRepository.findBySalonId(salonId)
        val settings = existing ?: SalonNotificationSettings(salonId = salonId)

        request.reminderEnabled?.let { settings.appointmentReminderEnabled = it }
        request.reminderHoursBefore?.let { settings.appointmentReminderHoursBefore = it }
        request.confirmationEnabled?.let { settings.appointmentConfirmationEnabled = it }
        request.cancellationNotificationEnabled?.let { settings.cancellationNotificationEnabled = it }
        request.smsEnabled?.let { settings.smsNotificationsEnabled = it }
        request.emailEnabled?.let { settings.emailNotificationsEnabled = it }
        request.notificationEmail?.let { settings.notificationEmail = it }
        request.notificationPhone?.let { settings.notificationPhone = it }

        val saved = notificationSettingsRepository.save(settings)
        cacheService.evict(salonId)

        return saved.toDto()
    }

    @Transactional
    fun updateIntegrationSettings(salonId: UUID, request: UpdateIntegrationSettingsRequest): SalonIntegrationSettingsDto {
        findSalonOrThrow(salonId)

        val existing = integrationSettingsRepository.findBySalonId(salonId)
        val settings = existing ?: SalonIntegrationSettings(salonId = salonId)

        request.facebookPixelId?.let { settings.facebookPixelId = it }
        request.googleAnalyticsId?.let { settings.googleAnalyticsId = it }
        request.webhookUrl?.let { settings.webhookUrl = it }
        request.webhookSecret?.let { settings.webhookSecret = encryptionService.encrypt(it) }
        request.marketplaceEnabled?.let { settings.marketplaceEnabled = it }
        request.marketplaceProfileVisible?.let { settings.marketplaceProfileVisible = it }

        val saved = integrationSettingsRepository.save(settings)
        cacheService.evict(salonId)

        return saved.toDto()
    }

    @Transactional(readOnly = true)
    fun getSalonPublicProfile(slug: String): SalonPublicProfileDto {
        val salon = salonRepository.findBySlug(slug)
            ?: throw SalonNotFoundException(UUID(0, 0))
        val salonId = salon.id!!
        val openingHours = openingHoursRepository.findAllBySalonIdOrderByDayOfWeek(salonId)

        return SalonPublicProfileDto(
            id = salonId,
            name = salon.name,
            slug = salon.slug,
            logoUrl = salon.logoUrl,
            coverImageUrl = salon.coverImageUrl,
            description = salon.description,
            phone = salon.phone,
            email = salon.email,
            address = salon.toAddressDto(),
            openingHours = openingHours.map { it.toDto() },
            timezone = salon.timezone,
        )
    }

    @Transactional
    fun uploadLogo(salonId: UUID, file: MultipartFile): String {
        val salon = findSalonOrThrow(salonId)

        val contentType = file.contentType
            ?: throw IllegalArgumentException("Content type is required")

        if (contentType !in allowedImageTypes) {
            throw IllegalArgumentException("Invalid file type: $contentType. Allowed: $allowedImageTypes")
        }

        if (file.size > maxLogoSize) {
            throw IllegalArgumentException("File too large: ${file.size} bytes. Maximum: $maxLogoSize bytes (2MB)")
        }

        val extension = when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "bin"
        }

        val filename = "$salonId/${UUID.randomUUID()}.$extension"
        val url = storageService.store(file.inputStream, filename, contentType)

        salon.logoUrl = url
        salonRepository.save(salon)
        cacheService.evict(salonId)

        return url
    }

    fun getLoyaltyConfigForAppointment(salonId: UUID): LoyaltyConfig? {
        val salon = findSalonOrThrow(salonId)
        if (!salon.loyaltyEnabled) return null

        return LoyaltyConfig(
            enabled = true,
            pointsPerVisit = salon.pointsPerVisit,
            pointsPerCurrencyUnit = salon.pointsPerCurrencyUnit,
            redemptionRate = salon.pointsRedemptionRate,
            expireDays = salon.loyaltyPointsExpireDays,
        )
    }

    private fun findSalonOrThrow(salonId: UUID): Salon =
        salonRepository.findById(salonId).orElseThrow { SalonNotFoundException(salonId) }

    private fun mapToSettingsDto(
        salon: Salon,
        openingHours: List<SalonOpeningHours>,
        notifications: SalonNotificationSettings?,
        integrations: SalonIntegrationSettings?,
    ): SalonSettingsDto = SalonSettingsDto(
        id = salon.id!!,
        name = salon.name,
        slug = salon.slug,
        status = salon.status.name,
        logoUrl = salon.logoUrl,
        coverImageUrl = salon.coverImageUrl,
        description = salon.description,
        websiteUrl = salon.websiteUrl,
        phone = salon.phone,
        email = salon.email,
        address = salon.toAddressDto(),
        timezone = salon.timezone,
        currency = salon.currency,
        cancellationWindowHours = salon.cancellationWindowHours,
        bookingWindowDays = salon.bookingWindowDays,
        slotIntervalMinutes = salon.slotIntervalMinutes,
        defaultAppointmentBufferMinutes = salon.defaultAppointmentBufferMinutes,
        invoicing = InvoicingDto(
            invoicingName = salon.invoicingName,
            taxId = salon.taxId,
            street = salon.invoicingStreet,
            city = salon.invoicingCity,
            postalCode = salon.invoicingPostalCode,
            bankAccountNumber = salon.bankAccountNumber,
            footerNotes = salon.invoiceFooterNotes,
        ),
        loyalty = LoyaltySettingsDto(
            enabled = salon.loyaltyEnabled,
            pointsPerVisit = salon.pointsPerVisit,
            pointsPerCurrencyUnit = salon.pointsPerCurrencyUnit,
            redemptionRate = salon.pointsRedemptionRate,
            expireDays = salon.loyaltyPointsExpireDays,
        ),
        booking = BookingPolicyDto(
            requireClientPhone = salon.requireClientPhone,
            allowOnlineBooking = salon.allowOnlineBooking,
            autoConfirmAppointments = salon.autoConfirmAppointments,
            onlineBookingMessage = salon.onlineBookingMessage,
            maxAdvanceBookingDays = salon.maxAdvanceBookingDays,
        ),
        openingHours = openingHours.map { it.toDto() },
        notifications = notifications?.toDto(),
        integrations = integrations?.toDto(),
        createdAt = salon.createdAt,
        updatedAt = salon.updatedAt,
    )

    private fun Salon.toAddressDto(): AddressDto {
        val formatted = if (street != null && postalCode != null && city != null) {
            "$street, $postalCode $city"
        } else null
        return AddressDto(
            street = street,
            city = city,
            postalCode = postalCode,
            country = country,
            latitude = latitude,
            longitude = longitude,
            formatted = formatted,
        )
    }

    private fun SalonOpeningHours.toDto() = SalonOpeningHoursDto(
        dayOfWeek = dayOfWeek,
        isOpen = isOpen,
        openTime = openTime,
        closeTime = closeTime,
        breakStart = breakStart,
        breakEnd = breakEnd,
    )

    private fun SalonNotificationSettings.toDto() = SalonNotificationSettingsDto(
        reminderEnabled = appointmentReminderEnabled,
        reminderHoursBefore = appointmentReminderHoursBefore,
        confirmationEnabled = appointmentConfirmationEnabled,
        cancellationNotificationEnabled = cancellationNotificationEnabled,
        smsEnabled = smsNotificationsEnabled,
        emailEnabled = emailNotificationsEnabled,
        notificationEmail = notificationEmail,
        notificationPhone = notificationPhone,
    )

    private fun SalonIntegrationSettings.toDto() = SalonIntegrationSettingsDto(
        googleCalendarEnabled = googleCalendarEnabled,
        facebookPixelId = facebookPixelId,
        googleAnalyticsId = googleAnalyticsId,
        webhookUrl = webhookUrl,
        webhookConfigured = !webhookSecret.isNullOrBlank(),
        marketplaceEnabled = marketplaceEnabled,
        marketplaceProfileVisible = marketplaceProfileVisible,
    )
}

// Request DTOs used by the service layer
data class UpdateGeneralSettingsRequest(
    val name: String? = null,
    val slug: String? = null,
    val timezone: String? = null,
    val currency: String? = null,
    val cancellationWindowHours: Int? = null,
    val bookingWindowDays: Int? = null,
    val slotIntervalMinutes: Int? = null,
    val defaultAppointmentBufferMinutes: Int? = null,
    val phone: String? = null,
    val email: String? = null,
    val description: String? = null,
    val websiteUrl: String? = null,
    val street: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
)

data class UpdateOpeningHoursRequest(
    val dayOfWeek: java.time.DayOfWeek,
    val isOpen: Boolean,
    val openTime: java.time.LocalTime? = null,
    val closeTime: java.time.LocalTime? = null,
    val breakStart: java.time.LocalTime? = null,
    val breakEnd: java.time.LocalTime? = null,
)

data class UpdateInvoicingSettingsRequest(
    val invoicingName: String? = null,
    val taxId: String? = null,
    val invoicingStreet: String? = null,
    val invoicingCity: String? = null,
    val invoicingPostalCode: String? = null,
    val bankAccountNumber: String? = null,
    val invoiceFooterNotes: String? = null,
)

data class UpdateLoyaltySettingsRequest(
    val enabled: Boolean,
    val pointsPerVisit: Int? = null,
    val pointsPerCurrencyUnit: Int? = null,
    val redemptionRate: java.math.BigDecimal? = null,
    val expireDays: Int? = null,
)

data class UpdateNotificationSettingsRequest(
    val reminderEnabled: Boolean? = null,
    val reminderHoursBefore: Int? = null,
    val confirmationEnabled: Boolean? = null,
    val cancellationNotificationEnabled: Boolean? = null,
    val smsEnabled: Boolean? = null,
    val emailEnabled: Boolean? = null,
    val notificationEmail: String? = null,
    val notificationPhone: String? = null,
)

data class UpdateIntegrationSettingsRequest(
    val facebookPixelId: String? = null,
    val googleAnalyticsId: String? = null,
    val webhookUrl: String? = null,
    val webhookSecret: String? = null,
    val marketplaceEnabled: Boolean? = null,
    val marketplaceProfileVisible: Boolean? = null,
)
