package com.beautyfinder.b2b.api.salon

import com.beautyfinder.b2b.application.salon.AddressDto
import com.beautyfinder.b2b.application.salon.BookingPolicyDto
import com.beautyfinder.b2b.application.salon.InvoicingDto
import com.beautyfinder.b2b.application.salon.LoyaltySettingsDto
import com.beautyfinder.b2b.application.salon.SalonIntegrationSettingsDto
import com.beautyfinder.b2b.application.salon.SalonNotificationSettingsDto
import com.beautyfinder.b2b.application.salon.SalonOpeningHoursDto
import com.beautyfinder.b2b.application.salon.SalonPublicProfileDto
import com.beautyfinder.b2b.application.salon.SalonSettingsDto
import com.beautyfinder.b2b.application.salon.UpdateGeneralSettingsRequest
import com.beautyfinder.b2b.application.salon.UpdateIntegrationSettingsRequest
import com.beautyfinder.b2b.application.salon.UpdateInvoicingSettingsRequest
import com.beautyfinder.b2b.application.salon.UpdateLoyaltySettingsRequest
import com.beautyfinder.b2b.application.salon.UpdateNotificationSettingsRequest
import com.beautyfinder.b2b.application.salon.UpdateOpeningHoursRequest
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

// ==================== Request DTOs ====================

data class UpdateGeneralSettingsApiRequest(
    @field:Size(min = 2, max = 100) val name: String? = null,
    @field:Pattern(regexp = "^[a-z0-9-]{2,60}$") val slug: String? = null,
    @field:Size(max = 50) val timezone: String? = null,
    @field:Size(min = 3, max = 3) val currency: String? = null,
    @field:Min(0) @field:Max(168) val cancellationWindowHours: Int? = null,
    @field:Min(1) @field:Max(365) val bookingWindowDays: Int? = null,
    val slotIntervalMinutes: Int? = null,
    @field:Min(0) @field:Max(120) val defaultAppointmentBufferMinutes: Int? = null,
    @field:Pattern(regexp = "\\+?[0-9]{9,15}") val phone: String? = null,
    @field:Email val email: String? = null,
    @field:Size(max = 2000) val description: String? = null,
    @field:Size(max = 500) val websiteUrl: String? = null,
    @field:Size(max = 200) val street: String? = null,
    @field:Size(max = 100) val city: String? = null,
    @field:Size(max = 10) val postalCode: String? = null,
) {
    fun toServiceRequest() = UpdateGeneralSettingsRequest(
        name = name, slug = slug, timezone = timezone, currency = currency,
        cancellationWindowHours = cancellationWindowHours, bookingWindowDays = bookingWindowDays,
        slotIntervalMinutes = slotIntervalMinutes, defaultAppointmentBufferMinutes = defaultAppointmentBufferMinutes,
        phone = phone, email = email, description = description, websiteUrl = websiteUrl,
        street = street, city = city, postalCode = postalCode,
    )
}

data class UpdateOpeningHoursApiRequest(
    @field:NotNull val dayOfWeek: DayOfWeek,
    @field:NotNull val isOpen: Boolean,
    val openTime: LocalTime? = null,
    val closeTime: LocalTime? = null,
    val breakStart: LocalTime? = null,
    val breakEnd: LocalTime? = null,
) {
    fun toServiceRequest() = UpdateOpeningHoursRequest(
        dayOfWeek = dayOfWeek, isOpen = isOpen,
        openTime = openTime, closeTime = closeTime,
        breakStart = breakStart, breakEnd = breakEnd,
    )
}

data class UpdateInvoicingSettingsApiRequest(
    @field:Size(max = 200) val invoicingName: String? = null,
    @field:Pattern(regexp = "[0-9]{10}") val taxId: String? = null,
    @field:Size(max = 200) val invoicingStreet: String? = null,
    @field:Size(max = 100) val invoicingCity: String? = null,
    @field:Pattern(regexp = "[0-9]{2}-[0-9]{3}") val invoicingPostalCode: String? = null,
    @field:Size(max = 34) val bankAccountNumber: String? = null,
    @field:Size(max = 500) val invoiceFooterNotes: String? = null,
) {
    fun toServiceRequest() = UpdateInvoicingSettingsRequest(
        invoicingName = invoicingName, taxId = taxId,
        invoicingStreet = invoicingStreet, invoicingCity = invoicingCity,
        invoicingPostalCode = invoicingPostalCode, bankAccountNumber = bankAccountNumber,
        invoiceFooterNotes = invoiceFooterNotes,
    )
}

data class UpdateLoyaltySettingsApiRequest(
    @field:NotNull val enabled: Boolean,
    @field:Min(1) @field:Max(1000) val pointsPerVisit: Int? = null,
    @field:Min(1) @field:Max(100) val pointsPerCurrencyUnit: Int? = null,
    @field:DecimalMin("0.01") @field:DecimalMax("10.00") val redemptionRate: BigDecimal? = null,
    @field:Min(30) @field:Max(3650) val expireDays: Int? = null,
) {
    fun toServiceRequest() = UpdateLoyaltySettingsRequest(
        enabled = enabled, pointsPerVisit = pointsPerVisit,
        pointsPerCurrencyUnit = pointsPerCurrencyUnit,
        redemptionRate = redemptionRate, expireDays = expireDays,
    )
}

data class UpdateNotificationSettingsApiRequest(
    val reminderEnabled: Boolean? = null,
    @field:Min(1) @field:Max(72) val reminderHoursBefore: Int? = null,
    val confirmationEnabled: Boolean? = null,
    val cancellationNotificationEnabled: Boolean? = null,
    val smsEnabled: Boolean? = null,
    val emailEnabled: Boolean? = null,
    @field:Email val notificationEmail: String? = null,
    @field:Pattern(regexp = "\\+?[0-9]{9,15}") val notificationPhone: String? = null,
) {
    fun toServiceRequest() = UpdateNotificationSettingsRequest(
        reminderEnabled = reminderEnabled, reminderHoursBefore = reminderHoursBefore,
        confirmationEnabled = confirmationEnabled,
        cancellationNotificationEnabled = cancellationNotificationEnabled,
        smsEnabled = smsEnabled, emailEnabled = emailEnabled,
        notificationEmail = notificationEmail, notificationPhone = notificationPhone,
    )
}

data class UpdateIntegrationSettingsApiRequest(
    @field:Size(max = 50) val facebookPixelId: String? = null,
    @field:Pattern(regexp = "G-[A-Z0-9]{10}|UA-[0-9]+-[0-9]+") val googleAnalyticsId: String? = null,
    @field:Size(max = 500) val webhookUrl: String? = null,
    @field:Size(min = 16, max = 256) val webhookSecret: String? = null,
    val marketplaceEnabled: Boolean? = null,
    val marketplaceProfileVisible: Boolean? = null,
) {
    fun toServiceRequest() = UpdateIntegrationSettingsRequest(
        facebookPixelId = facebookPixelId, googleAnalyticsId = googleAnalyticsId,
        webhookUrl = webhookUrl, webhookSecret = webhookSecret,
        marketplaceEnabled = marketplaceEnabled, marketplaceProfileVisible = marketplaceProfileVisible,
    )
}

// ==================== Response DTOs ====================

data class SalonSettingsResponse(
    val id: UUID,
    val name: String,
    val slug: String,
    val status: String,
    val logoUrl: String?,
    val coverImageUrl: String?,
    val description: String?,
    val websiteUrl: String?,
    val phone: String?,
    val email: String?,
    val address: AddressDto,
    val timezone: String,
    val currency: String,
    val cancellationWindowHours: Int,
    val bookingWindowDays: Int,
    val slotIntervalMinutes: Int,
    val defaultAppointmentBufferMinutes: Int,
    val invoicing: InvoicingDto,
    val loyalty: LoyaltySettingsDto,
    val booking: BookingPolicyDto,
    val openingHours: List<SalonOpeningHoursDto>,
    val notifications: SalonNotificationSettingsDto?,
    val integrations: SalonIntegrationSettingsDto?,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
)

data class SalonOpeningHoursResponse(
    val dayOfWeek: DayOfWeek,
    val isOpen: Boolean,
    val openTime: LocalTime?,
    val closeTime: LocalTime?,
    val breakStart: LocalTime?,
    val breakEnd: LocalTime?,
)

data class SalonNotificationSettingsResponse(
    val reminderEnabled: Boolean,
    val reminderHoursBefore: Int,
    val confirmationEnabled: Boolean,
    val cancellationNotificationEnabled: Boolean,
    val smsEnabled: Boolean,
    val emailEnabled: Boolean,
    val notificationEmail: String?,
    val notificationPhone: String?,
)

data class SalonIntegrationSettingsResponse(
    val googleCalendarEnabled: Boolean,
    val facebookPixelId: String?,
    val googleAnalyticsId: String?,
    val webhookUrl: String?,
    val webhookConfigured: Boolean,
    val marketplaceEnabled: Boolean,
    val marketplaceProfileVisible: Boolean,
)

data class SalonPublicProfileResponse(
    val id: UUID,
    val name: String,
    val slug: String,
    val logoUrl: String?,
    val coverImageUrl: String?,
    val description: String?,
    val phone: String?,
    val email: String?,
    val address: AddressDto,
    val openingHours: List<SalonOpeningHoursDto>,
    val timezone: String,
)

data class LogoUploadResponse(val logoUrl: String)

// ==================== Mapper Extensions ====================

fun SalonSettingsDto.toResponse() = SalonSettingsResponse(
    id = id, name = name, slug = slug, status = status,
    logoUrl = logoUrl, coverImageUrl = coverImageUrl,
    description = description, websiteUrl = websiteUrl,
    phone = phone, email = email, address = address,
    timezone = timezone, currency = currency,
    cancellationWindowHours = cancellationWindowHours,
    bookingWindowDays = bookingWindowDays,
    slotIntervalMinutes = slotIntervalMinutes,
    defaultAppointmentBufferMinutes = defaultAppointmentBufferMinutes,
    invoicing = invoicing, loyalty = loyalty, booking = booking,
    openingHours = openingHours, notifications = notifications,
    integrations = integrations,
    createdAt = createdAt, updatedAt = updatedAt,
)

fun SalonOpeningHoursDto.toResponse() = SalonOpeningHoursResponse(
    dayOfWeek = dayOfWeek, isOpen = isOpen,
    openTime = openTime, closeTime = closeTime,
    breakStart = breakStart, breakEnd = breakEnd,
)

fun SalonNotificationSettingsDto.toResponse() = SalonNotificationSettingsResponse(
    reminderEnabled = reminderEnabled, reminderHoursBefore = reminderHoursBefore,
    confirmationEnabled = confirmationEnabled,
    cancellationNotificationEnabled = cancellationNotificationEnabled,
    smsEnabled = smsEnabled, emailEnabled = emailEnabled,
    notificationEmail = notificationEmail, notificationPhone = notificationPhone,
)

fun SalonIntegrationSettingsDto.toResponse() = SalonIntegrationSettingsResponse(
    googleCalendarEnabled = googleCalendarEnabled,
    facebookPixelId = facebookPixelId,
    googleAnalyticsId = googleAnalyticsId,
    webhookUrl = webhookUrl,
    webhookConfigured = webhookConfigured,
    marketplaceEnabled = marketplaceEnabled,
    marketplaceProfileVisible = marketplaceProfileVisible,
)

fun SalonPublicProfileDto.toResponse() = SalonPublicProfileResponse(
    id = id, name = name, slug = slug,
    logoUrl = logoUrl, coverImageUrl = coverImageUrl,
    description = description, phone = phone, email = email,
    address = address, openingHours = openingHours, timezone = timezone,
)
