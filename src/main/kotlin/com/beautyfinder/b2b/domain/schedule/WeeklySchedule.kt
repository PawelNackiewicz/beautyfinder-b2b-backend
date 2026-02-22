package com.beautyfinder.b2b.domain.schedule

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(
    name = "weekly_schedules",
    uniqueConstraints = [UniqueConstraint(columnNames = ["employee_id", "day_of_week"])],
)
class WeeklySchedule(
    @Column(name = "employee_id", nullable = false)
    var employeeId: UUID,

    @Column(name = "salon_id", nullable = false)
    var salonId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    var dayOfWeek: DayOfWeek,

    @Column(name = "start_time", nullable = false)
    var startTime: LocalTime,

    @Column(name = "end_time", nullable = false)
    var endTime: LocalTime,

    @Column(name = "is_working_day", nullable = false)
    var isWorkingDay: Boolean = true,
) : BaseEntity()
