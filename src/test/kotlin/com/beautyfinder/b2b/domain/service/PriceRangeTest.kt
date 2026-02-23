package com.beautyfinder.b2b.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class PriceRangeTest {

    @Test
    fun `formatted single price no currency`() {
        assertEquals("50 PLN", PriceRange(BigDecimal("50"), null).formatted())
    }

    @Test
    fun `formatted range shows both prices`() {
        assertEquals("50-80 PLN", PriceRange(BigDecimal("50"), BigDecimal("80")).formatted())
    }

    @Test
    fun `hasRange same min max returns false`() {
        assertFalse(PriceRange(BigDecimal("50"), BigDecimal("50")).hasRange)
    }

    @Test
    fun `hasRange different min max returns true`() {
        assertTrue(PriceRange(BigDecimal("50"), BigDecimal("80")).hasRange)
    }

    @Test
    fun `constructor negative min throws exception`() {
        assertThrows<IllegalArgumentException> {
            PriceRange(BigDecimal("-1"), null)
        }
    }

    @Test
    fun `constructor max less than min throws exception`() {
        assertThrows<IllegalArgumentException> {
            PriceRange(BigDecimal("80"), BigDecimal("50"))
        }
    }

    @Test
    fun `constructor null max success`() {
        val range = PriceRange(BigDecimal("50"), null)
        assertEquals(BigDecimal("50"), range.min)
        assertEquals(null, range.max)
    }
}
