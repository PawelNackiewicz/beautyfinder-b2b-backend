package com.beautyfinder.b2b.domain.salon

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "salon_integration_settings",
    uniqueConstraints = [UniqueConstraint(columnNames = ["salon_id"])],
)
class SalonIntegrationSettings(
    @Column(name = "salon_id", nullable = false)
    val salonId: UUID,

    @Column(name = "google_calendar_enabled", nullable = false)
    var googleCalendarEnabled: Boolean = false,

    @Column(name = "google_calendar_token")
    var googleCalendarToken: String? = null,

    @Column(name = "facebook_pixel_id")
    var facebookPixelId: String? = null,

    @Column(name = "google_analytics_id")
    var googleAnalyticsId: String? = null,

    @Column(name = "webhook_url")
    var webhookUrl: String? = null,

    @Column(name = "webhook_secret")
    var webhookSecret: String? = null,

    @Column(name = "marketplace_enabled", nullable = false)
    var marketplaceEnabled: Boolean = true,

    @Column(name = "marketplace_profile_visible", nullable = false)
    var marketplaceProfileVisible: Boolean = true,
) : BaseEntity()
