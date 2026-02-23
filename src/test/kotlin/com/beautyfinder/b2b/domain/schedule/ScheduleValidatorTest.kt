package com.beautyfinder.b2b.domain.schedule

import com.beautyfinder.b2b.domain.employee.InvalidScheduleException
import com.beautyfinder.b2b.domain.employee.ScheduleExceptionOverlapException
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ScheduleValidatorTest {

    private val salonId = UUID.randomUUID()
    private val employeeId = UUID.randomUUID()

    private fun buildWeeklySchedule(
        day: DayOfWeek = DayOfWeek.MONDAY,
        startTime: LocalTime = LocalTime.of(9, 0),
        endTime: LocalTime = LocalTime.of(17, 0),
    ) = WeeklySchedule(
        employeeId = employeeId,
        salonId = salonId,
        dayOfWeek = day,
        startTime = startTime,
        endTime = endTime,
        isWorkingDay = true,
    )

    private fun buildScheduleException(
        startAt: OffsetDateTime = OffsetDateTime.now().plusDays(5),
        endAt: OffsetDateTime = OffsetDateTime.now().plusDays(6),
        type: ScheduleExceptionType = ScheduleExceptionType.VACATION,
    ) = ScheduleException(
        employeeId = employeeId,
        salonId = salonId,
        startAt = startAt,
        endAt = endAt,
        reason = "Test",
        type = type,
    )

    // --- WeeklySchedule validation ---

    @Test
    fun `validateWeeklySchedule - valid schedule returns success`() {
        val schedule = buildWeeklySchedule()
        val result = ScheduleValidator.validateWeeklySchedule(schedule)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateWeeklySchedule - end before start returns failure`() {
        val schedule = buildWeeklySchedule(startTime = LocalTime.of(17, 0), endTime = LocalTime.of(9, 0))
        val result = ScheduleValidator.validateWeeklySchedule(schedule)
        assertTrue(result.isFailure)
        assertThrows<InvalidScheduleException> { result.getOrThrow() }
    }

    @Test
    fun `validateWeeklySchedule - too short (15 min) returns failure`() {
        val schedule = buildWeeklySchedule(startTime = LocalTime.of(9, 0), endTime = LocalTime.of(9, 15))
        val result = ScheduleValidator.validateWeeklySchedule(schedule)
        assertTrue(result.isFailure)
        assertThrows<InvalidScheduleException> { result.getOrThrow() }
    }

    @Test
    fun `validateWeeklySchedule - too long (13h) returns failure`() {
        val schedule = buildWeeklySchedule(startTime = LocalTime.of(5, 0), endTime = LocalTime.of(18, 1))
        val result = ScheduleValidator.validateWeeklySchedule(schedule)
        assertTrue(result.isFailure)
        assertThrows<InvalidScheduleException> { result.getOrThrow() }
    }

    // --- ScheduleException validation ---

    @Test
    fun `validateScheduleException - valid exception returns success`() {
        val exception = buildScheduleException()
        val result = ScheduleValidator.validateScheduleException(exception, emptyList())
        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateScheduleException - overlaps existing returns failure`() {
        val existing = buildScheduleException(
            startAt = OffsetDateTime.now().plusDays(4),
            endAt = OffsetDateTime.now().plusDays(7),
        )
        val newException = buildScheduleException(
            startAt = OffsetDateTime.now().plusDays(5),
            endAt = OffsetDateTime.now().plusDays(6),
        )
        val result = ScheduleValidator.validateScheduleException(newException, listOf(existing))
        assertTrue(result.isFailure)
        assertThrows<ScheduleExceptionOverlapException> { result.getOrThrow() }
    }

    @Test
    fun `validateScheduleException - in past returns failure`() {
        val exception = buildScheduleException(
            startAt = OffsetDateTime.now().minusDays(2),
            endAt = OffsetDateTime.now().minusDays(1),
        )
        val result = ScheduleValidator.validateScheduleException(exception, emptyList())
        assertTrue(result.isFailure)
        assertThrows<InvalidScheduleException> { result.getOrThrow() }
    }

    @Test
    fun `validateScheduleException - end before start returns failure`() {
        val exception = buildScheduleException(
            startAt = OffsetDateTime.now().plusDays(6),
            endAt = OffsetDateTime.now().plusDays(5),
        )
        val result = ScheduleValidator.validateScheduleException(exception, emptyList())
        assertTrue(result.isFailure)
        assertThrows<InvalidScheduleException> { result.getOrThrow() }
    }
}
