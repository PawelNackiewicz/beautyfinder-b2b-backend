package com.beautyfinder.b2b.application.employee

import com.beautyfinder.b2b.domain.employee.EmployeeStatus
import com.beautyfinder.b2b.domain.schedule.ScheduleExceptionType
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

data class EmployeeDto(
    val id: UUID,
    val salonId: UUID,
    val userId: UUID,
    val displayName: String,
    val phone: String?,
    val avatarUrl: String?,
    val color: String?,
    val status: EmployeeStatus,
    val serviceIds: Set<UUID>,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
)

data class WeeklyScheduleDto(
    val id: UUID?,
    val employeeId: UUID,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isWorkingDay: Boolean,
)

data class ScheduleExceptionDto(
    val id: UUID,
    val employeeId: UUID,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
    val reason: String?,
    val type: ScheduleExceptionType,
    val createdAt: OffsetDateTime?,
)

data class AvailableSlotDto(
    val start: OffsetDateTime,
    val end: OffsetDateTime,
    val durationMinutes: Int,
)
