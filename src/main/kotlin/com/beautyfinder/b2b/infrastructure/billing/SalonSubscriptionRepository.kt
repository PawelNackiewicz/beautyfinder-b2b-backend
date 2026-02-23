package com.beautyfinder.b2b.infrastructure.billing

import com.beautyfinder.b2b.domain.billing.SalonSubscription
import com.beautyfinder.b2b.domain.billing.SubscriptionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface SalonSubscriptionRepository : JpaRepository<SalonSubscription, UUID> {

    fun findBySalonIdAndStatus(salonId: UUID, status: SubscriptionStatus): SalonSubscription?

    @Query(
        """
        SELECT s FROM SalonSubscription s
        WHERE s.salonId = :salonId
        AND s.status = 'ACTIVE'
        AND s.validFrom <= :date
        AND (s.validTo IS NULL OR s.validTo >= :date)
        """
    )
    fun findActiveSubscription(
        @Param("salonId") salonId: UUID,
        @Param("date") date: LocalDate,
    ): SalonSubscription?
}
