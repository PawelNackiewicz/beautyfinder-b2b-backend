package com.beautyfinder.b2b.domain

import com.beautyfinder.b2b.domain.salon.SalonStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "salons")
class Salon(
    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, unique = true)
    var slug: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SalonStatus = SalonStatus.ACTIVE,

    @Column(name = "logo_url")
    var logoUrl: String? = null,

    @Column(name = "cover_image_url")
    var coverImageUrl: String? = null,

    @Column(length = 2000)
    var description: String? = null,

    @Column(name = "website_url")
    var websiteUrl: String? = null,

    var phone: String? = null,

    var email: String? = null,

    // Address
    var street: String? = null,

    var city: String? = null,

    @Column(name = "postal_code")
    var postalCode: String? = null,

    @Column(nullable = false)
    var country: String = "PL",

    var latitude: BigDecimal? = null,

    var longitude: BigDecimal? = null,

    // Operational settings
    @Column(nullable = false)
    var timezone: String = "Europe/Warsaw",

    @Column(nullable = false)
    var currency: String = "PLN",

    @Column(name = "cancellation_window_hours", nullable = false)
    var cancellationWindowHours: Int = 24,

    @Column(name = "booking_window_days", nullable = false)
    var bookingWindowDays: Int = 60,

    @Column(name = "slot_interval_minutes", nullable = false)
    var slotIntervalMinutes: Int = 15,

    @Column(name = "default_appointment_buffer_minutes", nullable = false)
    var defaultAppointmentBufferMinutes: Int = 0,

    @Column(name = "max_advance_booking_days", nullable = false)
    var maxAdvanceBookingDays: Int = 90,

    // Invoicing
    @Column(name = "invoicing_name")
    var invoicingName: String? = null,

    @Column(name = "tax_id")
    var taxId: String? = null,

    @Column(name = "invoicing_street")
    var invoicingStreet: String? = null,

    @Column(name = "invoicing_city")
    var invoicingCity: String? = null,

    @Column(name = "invoicing_postal_code")
    var invoicingPostalCode: String? = null,

    @Column(name = "bank_account_number")
    var bankAccountNumber: String? = null,

    @Column(name = "invoice_footer_notes", length = 500)
    var invoiceFooterNotes: String? = null,

    // Loyalty program
    @Column(name = "loyalty_enabled", nullable = false)
    var loyaltyEnabled: Boolean = false,

    @Column(name = "points_per_visit")
    var pointsPerVisit: Int? = null,

    @Column(name = "points_per_currency_unit")
    var pointsPerCurrencyUnit: Int? = null,

    @Column(name = "points_redemption_rate")
    var pointsRedemptionRate: BigDecimal? = null,

    @Column(name = "loyalty_points_expire_days")
    var loyaltyPointsExpireDays: Int? = null,

    // Booking policy
    @Column(name = "require_client_phone", nullable = false)
    var requireClientPhone: Boolean = true,

    @Column(name = "allow_online_booking", nullable = false)
    var allowOnlineBooking: Boolean = true,

    @Column(name = "auto_confirm_appointments", nullable = false)
    var autoConfirmAppointments: Boolean = false,

    @Column(name = "online_booking_message", length = 500)
    var onlineBookingMessage: String? = null,
) : BaseEntity()
