package com.beautyfinder.b2b.domain.appointment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class TimeSlotTest {

    private val base = OffsetDateTime.of(2025, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC)

    @Test
    fun `no overlap - slot before`() {
        // given
        val slot1 = TimeSlot(base, base.plusHours(1))
        val slot2 = TimeSlot(base.plusHours(2), base.plusHours(3))

        // then
        assertFalse(slot1.overlaps(slot2))
    }

    @Test
    fun `no overlap - slot after`() {
        // given
        val slot1 = TimeSlot(base.plusHours(2), base.plusHours(3))
        val slot2 = TimeSlot(base, base.plusHours(1))

        // then
        assertFalse(slot1.overlaps(slot2))
    }

    @Test
    fun `partial overlap from left`() {
        // given
        val slot1 = TimeSlot(base, base.plusHours(2))
        val slot2 = TimeSlot(base.plusHours(1), base.plusHours(3))

        // then
        assertTrue(slot1.overlaps(slot2))
    }

    @Test
    fun `partial overlap from right`() {
        // given
        val slot1 = TimeSlot(base.plusHours(1), base.plusHours(3))
        val slot2 = TimeSlot(base, base.plusHours(2))

        // then
        assertTrue(slot1.overlaps(slot2))
    }

    @Test
    fun `full containment`() {
        // given
        val outer = TimeSlot(base, base.plusHours(4))
        val inner = TimeSlot(base.plusHours(1), base.plusHours(2))

        // then
        assertTrue(outer.overlaps(inner))
        assertTrue(inner.overlaps(outer))
    }

    @Test
    fun `identical slots overlap`() {
        // given
        val slot1 = TimeSlot(base, base.plusHours(1))
        val slot2 = TimeSlot(base, base.plusHours(1))

        // then
        assertTrue(slot1.overlaps(slot2))
    }

    @Test
    fun `durationMinutes returns correct duration`() {
        // given
        val slot = TimeSlot(base, base.plusMinutes(90))

        // then
        assertEquals(90L, slot.durationMinutes())
    }
}
