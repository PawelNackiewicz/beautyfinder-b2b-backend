package com.beautyfinder.b2b.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime
import java.time.ZoneOffset

class DurationTest {

    @Test
    fun `formatted 30min returns 30 min`() {
        assertEquals("30 min", Duration(30).formatted())
    }

    @Test
    fun `formatted 90min returns 1h 30 min`() {
        assertEquals("1h 30 min", Duration(90).formatted())
    }

    @Test
    fun `formatted 120min returns 2h`() {
        assertEquals("2h", Duration(120).formatted())
    }

    @Test
    fun `formatted 5min returns 5 min`() {
        assertEquals("5 min", Duration(5).formatted())
    }

    @Test
    fun `toEndTime adds minutes correctly`() {
        val start = OffsetDateTime.of(2025, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)
        val end = Duration(45).toEndTime(start)
        assertEquals(start.plusMinutes(45), end)
    }

    @Test
    fun `constructor below min throws exception`() {
        assertThrows<IllegalArgumentException> { Duration(4) }
    }

    @Test
    fun `constructor above max throws exception`() {
        assertThrows<IllegalArgumentException> { Duration(481) }
    }

    @Test
    fun `constructor exactly 5min success`() {
        assertEquals(5, Duration(5).minutes)
    }

    @Test
    fun `constructor exactly 480min success`() {
        assertEquals(480, Duration(480).minutes)
    }
}
