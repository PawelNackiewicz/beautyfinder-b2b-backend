package com.beautyfinder.b2b.domain.billing

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

enum class SubscriptionPlan {
    BASIC, STANDARD, PREMIUM
}

enum class SubscriptionStatus {
    ACTIVE, SUSPENDED, CANCELLED
}

@Entity
@Table(name = "salon_subscriptions")
class SalonSubscription(
    @Column(name = "salon_id", nullable = false)
    val salonId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val plan: SubscriptionPlan,

    @Column(name = "monthly_fee", nullable = false, precision = 10, scale = 2)
    val monthlyFee: BigDecimal,

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    val commissionRate: BigDecimal,

    @Column(name = "valid_from", nullable = false)
    val validFrom: LocalDate,

    @Column(name = "valid_to")
    val validTo: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
) : BaseEntity()
