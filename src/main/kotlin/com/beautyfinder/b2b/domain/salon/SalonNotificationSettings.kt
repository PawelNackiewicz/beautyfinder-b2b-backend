package com.beautyfinder.b2b.domain.salon

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "salon_notification_settings",
    uniqueConstraints = [UniqueConstraint(columnNames = ["salon_id"])],
)
class SalonNotificationSettings(
    @Column(name = "salon_id", nullable = false)
    val salonId: UUID,

    @Column(name = "appointment_reminder_enabled", nullable = false)
    var appointmentReminderEnabled: Boolean = true,

    @Column(name = "appointment_reminder_hours_before", nullable = false)
    var appointmentReminderHoursBefore: Int = 24,

    @Column(name = "appointment_confirmation_enabled", nullable = false)
    var appointmentConfirmationEnabled: Boolean = true,

    @Column(name = "cancellation_notification_enabled", nullable = false)
    var cancellationNotificationEnabled: Boolean = true,

    @Column(name = "marketing_emails_enabled", nullable = false)
    var marketingEmailsEnabled: Boolean = false,

    @Column(name = "sms_notifications_enabled", nullable = false)
    var smsNotificationsEnabled: Boolean = false,

    @Column(name = "email_notifications_enabled", nullable = false)
    var emailNotificationsEnabled: Boolean = true,

    @Column(name = "notification_email")
    var notificationEmail: String? = null,

    @Column(name = "notification_phone")
    var notificationPhone: String? = null,
) : BaseEntity()
