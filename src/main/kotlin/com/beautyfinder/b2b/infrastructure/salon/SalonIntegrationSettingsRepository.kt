package com.beautyfinder.b2b.infrastructure.salon

import com.beautyfinder.b2b.domain.salon.SalonIntegrationSettings
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SalonIntegrationSettingsRepository : JpaRepository<SalonIntegrationSettings, UUID> {
    fun findBySalonId(salonId: UUID): SalonIntegrationSettings?
}
