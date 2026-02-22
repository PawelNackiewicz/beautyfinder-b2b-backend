package com.beautyfinder.b2b.domain.schedule

import com.beautyfinder.b2b.domain.employee.InvalidScheduleException
import com.beautyfinder.b2b.domain.employee.ScheduleExceptionOverlapException
import com.beautyfinder.b2b.domain.employee.ScheduleOverlapException
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

object ScheduleValidator {

    fun validateWeeklySchedule(schedule: WeeklySchedule): Result<Unit> {
        if (!schedule.endTime.isAfter(schedule.startTime)) {
            return Result.failure(InvalidScheduleException("End time must be after start time"))
        }

        val durationMinutes = ChronoUnit.MINUTES.between(schedule.startTime, schedule.endTime)

        if (durationMinutes < 30) {
            return Result.failure(InvalidScheduleException("Minimum shift duration is 30 minutes"))
        }

        if (durationMinutes > 720) {
            return Result.failure(InvalidScheduleException("Maximum shift duration is 12 hours (720 minutes)"))
        }

        return Result.success(Unit)
    }

    fun validateScheduleException(
        exception: ScheduleException,
        existing: List<ScheduleException>,
    ): Result<Unit> {
        if (!exception.endAt.isAfter(exception.startAt)) {
            return Result.failure(InvalidScheduleException("End time must be after start time"))
        }

        val durationMinutes = ChronoUnit.MINUTES.between(exception.startAt, exception.endAt)
        if (durationMinutes < 60) {
            return Result.failure(InvalidScheduleException("Minimum exception duration is 1 hour"))
        }

        if (exception.startAt.isBefore(OffsetDateTime.now())) {
            return Result.failure(InvalidScheduleException("Schedule exception cannot start in the past"))
        }

        val overlapping = existing.any { ex ->
            exception.startAt.isBefore(ex.endAt) && exception.endAt.isAfter(ex.startAt)
        }
        if (overlapping) {
            return Result.failure(
                ScheduleExceptionOverlapException(exception.employeeId, exception.startAt, exception.endAt)
            )
        }

        return Result.success(Unit)
    }

    fun validateNoOverlap(newSchedule: WeeklySchedule, existing: List<WeeklySchedule>): Result<Unit> {
        val overlap = existing.find { it.dayOfWeek == newSchedule.dayOfWeek && it.id != newSchedule.id }
        if (overlap != null) {
            return Result.failure(
                ScheduleOverlapException(newSchedule.employeeId, newSchedule.dayOfWeek, overlap.startTime, overlap.endTime)
            )
        }
        return Result.success(Unit)
    }
}
