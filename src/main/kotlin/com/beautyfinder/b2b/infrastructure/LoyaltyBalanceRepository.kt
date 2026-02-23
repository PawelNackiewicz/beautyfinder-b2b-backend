package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.client.LoyaltyBalance
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LoyaltyBalanceRepository : JpaRepository<LoyaltyBalance, UUID> {

    fun findByClientIdAndSalonId(clientId: UUID, salonId: UUID): LoyaltyBalance?
}
