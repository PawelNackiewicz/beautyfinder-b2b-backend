package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.schedule.WeeklySchedule
import org.springframework.data.jpa.repository.JpaRepository
import java.time.DayOfWeek
import java.util.UUID

interface WeeklyScheduleRepository : JpaRepository<WeeklySchedule, UUID> {
    fun findAllByEmployeeId(employeeId: UUID): List<WeeklySchedule>
    fun findByEmployeeIdAndDayOfWeek(employeeId: UUID, dayOfWeek: DayOfWeek): WeeklySchedule?
    fun deleteAllByEmployeeId(employeeId: UUID)
    fun findAllByEmployeeIdAndSalonId(employeeId: UUID, salonId: UUID): List<WeeklySchedule>
}
