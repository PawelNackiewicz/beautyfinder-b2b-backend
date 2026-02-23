package com.beautyfinder.b2b.domain.client

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "loyalty_balances",
    uniqueConstraints = [UniqueConstraint(columnNames = ["client_id", "salon_id"])],
)
class LoyaltyBalance(
    @Column(name = "client_id", nullable = false)
    val clientId: UUID,

    @Column(name = "salon_id", nullable = false)
    val salonId: UUID,

    @Column(nullable = false)
    var points: Int = 0,

    @Column(name = "total_earned", nullable = false)
    var totalEarned: Int = 0,

    @Column(name = "total_redeemed", nullable = false)
    var totalRedeemed: Int = 0,
) : BaseEntity()
