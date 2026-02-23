package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.client.LoyaltyTransaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LoyaltyTransactionRepository : JpaRepository<LoyaltyTransaction, UUID> {

    fun findAllByClientIdAndSalonIdOrderByCreatedAtDesc(
        clientId: UUID,
        salonId: UUID,
        pageable: Pageable,
    ): Page<LoyaltyTransaction>
}
