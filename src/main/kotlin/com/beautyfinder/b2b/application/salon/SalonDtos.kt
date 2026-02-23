package com.beautyfinder.b2b.application.salon

import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

data class SalonSettingsDto(
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

data class AddressDto(
    val street: String?,
    val city: String?,
    val postalCode: String?,
    val country: String,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val formatted: String?,
)

data class InvoicingDto(
    val invoicingName: String?,
    val taxId: String?,
    val street: String?,
    val city: String?,
    val postalCode: String?,
    val bankAccountNumber: String?,
    val footerNotes: String?,
)

data class LoyaltySettingsDto(
    val enabled: Boolean,
    val pointsPerVisit: Int?,
    val pointsPerCurrencyUnit: Int?,
    val redemptionRate: BigDecimal?,
    val expireDays: Int?,
)

data class BookingPolicyDto(
    val requireClientPhone: Boolean,
    val allowOnlineBooking: Boolean,
    val autoConfirmAppointments: Boolean,
    val onlineBookingMessage: String?,
    val maxAdvanceBookingDays: Int,
)

data class SalonOpeningHoursDto(
    val dayOfWeek: DayOfWeek,
    val isOpen: Boolean,
    val openTime: LocalTime?,
    val closeTime: LocalTime?,
    val breakStart: LocalTime?,
    val breakEnd: LocalTime?,
)

data class SalonNotificationSettingsDto(
    val reminderEnabled: Boolean,
    val reminderHoursBefore: Int,
    val confirmationEnabled: Boolean,
    val cancellationNotificationEnabled: Boolean,
    val smsEnabled: Boolean,
    val emailEnabled: Boolean,
    val notificationEmail: String?,
    val notificationPhone: String?,
)

data class SalonIntegrationSettingsDto(
    val googleCalendarEnabled: Boolean,
    val facebookPixelId: String?,
    val googleAnalyticsId: String?,
    val webhookUrl: String?,
    val webhookConfigured: Boolean,
    val marketplaceEnabled: Boolean,
    val marketplaceProfileVisible: Boolean,
)

data class SalonPublicProfileDto(
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
