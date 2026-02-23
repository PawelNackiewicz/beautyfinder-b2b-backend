package com.beautyfinder.b2b.infrastructure.salon

import com.beautyfinder.b2b.domain.salon.SalonNotificationSettings
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SalonNotificationSettingsRepository : JpaRepository<SalonNotificationSettings, UUID> {
    fun findBySalonId(salonId: UUID): SalonNotificationSettings?
}
