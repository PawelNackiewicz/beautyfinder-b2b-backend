package com.beautyfinder.b2b.domain.client

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

enum class ConsentType {
    MARKETING_EMAIL, MARKETING_SMS, DATA_PROCESSING, PROFILING, THIRD_PARTY_SHARING
}

@Entity
@Table(name = "gdpr_consents")
class GdprConsent(
    @Column(name = "client_id", nullable = false)
    val clientId: UUID,

    @Column(name = "salon_id", nullable = false)
    val salonId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false)
    val consentType: ConsentType,

    @Column(nullable = false)
    val granted: Boolean,

    @Column(name = "granted_at", nullable = false)
    val grantedAt: OffsetDateTime,

    @Column(name = "ip_address")
    val ipAddress: String? = null,

    @Column(name = "consent_version", nullable = false)
    val consentVersion: String,

    @Column(name = "expires_at")
    val expiresAt: OffsetDateTime? = null,
) : BaseEntity()
