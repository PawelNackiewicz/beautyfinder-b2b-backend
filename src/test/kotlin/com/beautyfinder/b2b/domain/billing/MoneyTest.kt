package com.beautyfinder.b2b.domain.billing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.RoundingMode

class MoneyTest {

    @Test
    fun `plus sameCurrency addsCorrectly`() {
        val a = Money.of("100.00")
        val b = Money.of("50.50")
        val result = a + b
        assertEquals(BigDecimal("150.50"), result.amount)
        assertEquals("PLN", result.currency)
    }

    @Test
    fun `minus sameCurrency subtractsCorrectly`() {
        val a = Money.of("100.00")
        val b = Money.of("30.25")
        val result = a - b
        assertEquals(BigDecimal("69.75"), result.amount)
        assertEquals("PLN", result.currency)
    }

    @Test
    fun `times multiplier calculatesCorrectly`() {
        val money = Money.of("50.00")
        val result = money * BigDecimal("3")
        assertEquals(BigDecimal("150.00"), result.amount)
    }

    @Test
    fun `withVat 23percent calculatesCorrectly`() {
        val money = Money.of("100.00")
        val result = money.withVat(BigDecimal("0.23"))
        assertEquals(BigDecimal("123.00"), result.amount)
    }

    @Test
    fun `withVat precisionTest roundsCorrectly`() {
        val money = Money.of("33.33")
        val result = money.withVat(BigDecimal("0.23"))
        // 33.33 * 1.23 = 40.9959 → rounds to 41.00 with HALF_UP
        assertEquals(BigDecimal("41.00").setScale(2, RoundingMode.HALF_UP), result.amount)
    }

    @Test
    fun `plus differentCurrency throwsException`() {
        val pln = Money(BigDecimal("100.00"), "PLN")
        val eur = Money(BigDecimal("50.00"), "EUR")
        assertThrows<IllegalArgumentException> { pln + eur }
    }

    @Test
    fun `of double createsCorrectMoney`() {
        val money = Money.of(99.99)
        assertEquals(BigDecimal("99.99"), money.amount)
        assertEquals("PLN", money.currency)
    }

    @Test
    fun `ZERO hasCorrectValue`() {
        assertEquals(BigDecimal("0.00"), Money.ZERO.amount)
        assertEquals("PLN", Money.ZERO.currency)
    }
}
