package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.schedule.ScheduleException
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime
import java.util.UUID

interface ScheduleExceptionRepository : JpaRepository<ScheduleException, UUID> {
    fun findByEmployeeIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
        employeeId: UUID,
        endAt: OffsetDateTime,
        startAt: OffsetDateTime,
    ): List<ScheduleException>

    fun findByEmployeeIdAndStartAtLessThanAndEndAtGreaterThan(
        employeeId: UUID,
        endAt: OffsetDateTime,
        startAt: OffsetDateTime,
    ): List<ScheduleException>

    fun findAllByEmployeeIdAndSalonIdAndStartAtGreaterThanEqualAndEndAtLessThanEqual(
        employeeId: UUID,
        salonId: UUID,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<ScheduleException>

    fun findByIdAndEmployeeIdAndSalonId(id: UUID, employeeId: UUID, salonId: UUID): ScheduleException?
}
