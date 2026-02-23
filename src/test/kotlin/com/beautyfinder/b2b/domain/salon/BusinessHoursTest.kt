package com.beautyfinder.b2b.domain.salon

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

class BusinessHoursTest {

    private val timezone = ZoneId.of("Europe/Warsaw")
    private val salonId = UUID.randomUUID()

    // 2024-06-05 is a Wednesday
    private val wednesday = LocalDate.of(2024, 6, 5)
    private val sunday = LocalDate.of(2024, 6, 9)

    private fun createOpeningHours(
        dayOfWeek: DayOfWeek,
        isOpen: Boolean,
        openTime: LocalTime? = null,
        closeTime: LocalTime? = null,
        breakStart: LocalTime? = null,
        breakEnd: LocalTime? = null,
    ) = SalonOpeningHours(
        salonId = salonId,
        dayOfWeek = dayOfWeek,
        isOpen = isOpen,
        openTime = openTime,
        closeTime = closeTime,
        breakStart = breakStart,
        breakEnd = breakEnd,
    )

    private fun createWeekdayHours() = listOf(
        createOpeningHours(DayOfWeek.MONDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0)),
        createOpeningHours(DayOfWeek.TUESDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0)),
        createOpeningHours(
            DayOfWeek.WEDNESDAY, true,
            LocalTime.of(9, 0), LocalTime.of(18, 0),
            LocalTime.of(13, 0), LocalTime.of(14, 0),
        ),
        createOpeningHours(DayOfWeek.THURSDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0)),
        createOpeningHours(DayOfWeek.FRIDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0)),
        createOpeningHours(DayOfWeek.SATURDAY, true, LocalTime.of(10, 0), LocalTime.of(14, 0)),
        createOpeningHours(DayOfWeek.SUNDAY, false),
    )

    private fun dateTimeAt(date: LocalDate, hour: Int, minute: Int): OffsetDateTime =
        date.atTime(LocalTime.of(hour, minute))
            .atZone(timezone)
            .toOffsetDateTime()

    @Test
    fun `isOpenAt_withinHours_returnsTrue`() {
        val businessHours = BusinessHours(createWeekdayHours())

        val result = businessHours.isOpenAt(dateTimeAt(wednesday, 14, 0), timezone)

        assertTrue(result)
    }

    @Test
    fun `isOpenAt_beforeOpening_returnsFalse`() {
        val businessHours = BusinessHours(createWeekdayHours())

        val result = businessHours.isOpenAt(dateTimeAt(wednesday, 8, 0), timezone)

        assertFalse(result)
    }

    @Test
    fun `isOpenAt_afterClosing_returnsFalse`() {
        val businessHours = BusinessHours(createWeekdayHours())

        val result = businessHours.isOpenAt(dateTimeAt(wednesday, 18, 1), timezone)

        assertFalse(result)
    }

    @Test
    fun `isOpenAt_duringBreak_returnsFalse`() {
        val businessHours = BusinessHours(createWeekdayHours())

        val result = businessHours.isOpenAt(dateTimeAt(wednesday, 13, 15), timezone)

        assertFalse(result)
    }

    @Test
    fun `isOpenAt_closedDay_returnsFalse`() {
        val businessHours = BusinessHours(createWeekdayHours())

        val result = businessHours.isOpenAt(dateTimeAt(sunday, 12, 0), timezone)

        assertFalse(result)
    }

    @Test
    fun `nextOpeningTime_fromClosedDay_returnsMonday`() {
        val businessHours = BusinessHours(createWeekdayHours())
        val sundayEvening = dateTimeAt(sunday, 20, 0)

        val result = businessHours.nextOpeningTime(sundayEvening, timezone)

        val expected = LocalDate.of(2024, 6, 10) // Monday
            .atTime(LocalTime.of(9, 0))
            .atZone(timezone)
            .toOffsetDateTime()
        assertEquals(expected, result)
    }

    @Test
    fun `nextOpeningTime_duringBreak_returnsBreakEnd`() {
        val businessHours = BusinessHours(createWeekdayHours())
        val duringBreak = dateTimeAt(wednesday, 13, 15)

        val result = businessHours.nextOpeningTime(duringBreak, timezone)

        val expected = wednesday.atTime(LocalTime.of(14, 0))
            .atZone(timezone)
            .toOffsetDateTime()
        assertEquals(expected, result)
    }

    @Test
    fun `validate_closeBeforeOpen_returnsFailure`() {
        val hours = listOf(
            createOpeningHours(
                DayOfWeek.MONDAY, true,
                LocalTime.of(18, 0), LocalTime.of(9, 0),
            ),
        )
        val businessHours = BusinessHours(hours)

        val result = businessHours.validate()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidOpeningHoursException)
    }

    @Test
    fun `validate_breakOutsideOpenHours_returnsFailure`() {
        val hours = listOf(
            createOpeningHours(
                DayOfWeek.MONDAY, true,
                LocalTime.of(9, 0), LocalTime.of(17, 0),
                LocalTime.of(8, 0), LocalTime.of(10, 0),
            ),
        )
        val businessHours = BusinessHours(hours)

        val result = businessHours.validate()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidOpeningHoursException)
    }

    @Test
    fun `validate_duplicateDayOfWeek_returnsFailure`() {
        val hours = listOf(
            createOpeningHours(DayOfWeek.MONDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0)),
            createOpeningHours(DayOfWeek.MONDAY, true, LocalTime.of(10, 0), LocalTime.of(17, 0)),
        )
        val businessHours = BusinessHours(hours)

        val result = businessHours.validate()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidOpeningHoursException)
    }
}
