package com.beautyfinder.b2b.domain.schedule

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

enum class ScheduleExceptionType {
    VACATION, SICK_LEAVE, BLOCKED, PERSONAL
}

@Entity
@Table(name = "schedule_exceptions")
class ScheduleException(
    @Column(name = "employee_id", nullable = false)
    var employeeId: UUID,

    @Column(name = "salon_id", nullable = false)
    var salonId: UUID,

    @Column(name = "start_at", nullable = false)
    var startAt: OffsetDateTime,

    @Column(name = "end_at", nullable = false)
    var endAt: OffsetDateTime,

    var reason: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: ScheduleExceptionType,
) : BaseEntity()
