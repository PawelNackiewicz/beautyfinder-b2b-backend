package com.beautyfinder.b2b.infrastructure.salon

import com.beautyfinder.b2b.domain.salon.SalonOpeningHours
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.DayOfWeek
import java.util.UUID

@Repository
interface SalonOpeningHoursRepository : JpaRepository<SalonOpeningHours, UUID> {
    fun findAllBySalonIdOrderByDayOfWeek(salonId: UUID): List<SalonOpeningHours>
    fun findBySalonIdAndDayOfWeek(salonId: UUID, dayOfWeek: DayOfWeek): SalonOpeningHours?
    fun deleteAllBySalonId(salonId: UUID)
}
