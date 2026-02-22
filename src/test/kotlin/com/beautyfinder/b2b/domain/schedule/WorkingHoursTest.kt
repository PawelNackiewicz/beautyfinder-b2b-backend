package com.beautyfinder.b2b.domain.schedule

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalTime

class WorkingHoursTest {

    @Test
    fun `constructor - invalid range (end before start) throws exception`() {
        assertThrows<IllegalArgumentException> {
            WorkingHours(LocalTime.of(18, 0), LocalTime.of(9, 0))
        }
    }

    @Test
    fun `constructor - same start and end throws exception`() {
        assertThrows<IllegalArgumentException> {
            WorkingHours(LocalTime.of(9, 0), LocalTime.of(9, 0))
        }
    }

    @Test
    fun `containsSlot - slot within hours returns true`() {
        val hours = WorkingHours(LocalTime.of(9, 0), LocalTime.of(17, 0))
        assertTrue(hours.containsSlot(LocalTime.of(10, 0), LocalTime.of(11, 0)))
    }

    @Test
    fun `containsSlot - slot starting before work returns false`() {
        val hours = WorkingHours(LocalTime.of(9, 0), LocalTime.of(17, 0))
        assertFalse(hours.containsSlot(LocalTime.of(8, 0), LocalTime.of(10, 0)))
    }

    @Test
    fun `containsSlot - slot ending after work returns false`() {
        val hours = WorkingHours(LocalTime.of(9, 0), LocalTime.of(17, 0))
        assertFalse(hours.containsSlot(LocalTime.of(16, 0), LocalTime.of(18, 0)))
    }

    @Test
    fun `containsSlot - exact boundary match returns true`() {
        val hours = WorkingHours(LocalTime.of(9, 0), LocalTime.of(17, 0))
        assertTrue(hours.containsSlot(LocalTime.of(9, 0), LocalTime.of(17, 0)))
    }

    @Test
    fun `durationMinutes - correct calculation`() {
        val hours = WorkingHours(LocalTime.of(9, 0), LocalTime.of(17, 0))
        assertEquals(480L, hours.durationMinutes())
    }

    @Test
    fun `toMinuteRanges - returns correct range`() {
        val hours = WorkingHours(LocalTime.of(9, 0), LocalTime.of(17, 0))
        val range = hours.toMinuteRanges()
        assertEquals(540, range.first) // 9*60
        assertEquals(1020, range.last) // 17*60
    }
}
