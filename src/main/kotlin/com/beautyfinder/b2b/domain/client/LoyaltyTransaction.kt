package com.beautyfinder.b2b.domain.client

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.util.UUID

enum class LoyaltyTransactionType {
    VISIT_REWARD, MANUAL_ADD, MANUAL_DEDUCT, REDEMPTION, EXPIRY
}

@Entity
@Table(name = "loyalty_transactions")
class LoyaltyTransaction(
    @Column(name = "client_id", nullable = false)
    val clientId: UUID,

    @Column(name = "salon_id", nullable = false)
    val salonId: UUID,

    @Column(nullable = false)
    val points: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: LoyaltyTransactionType,

    @Column(name = "appointment_id")
    val appointmentId: UUID? = null,

    val note: String? = null,

    @Column(name = "balance_after", nullable = false)
    val balanceAfter: Int,
) : BaseEntity()
