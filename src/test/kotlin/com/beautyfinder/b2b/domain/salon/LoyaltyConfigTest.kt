package com.beautyfinder.b2b.domain.salon

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class LoyaltyConfigTest {

    @Test
    fun `calculatePointsForVisit_pointsPerVisit_returnsFixed`() {
        val config = LoyaltyConfig(
            enabled = true,
            pointsPerVisit = 10,
            pointsPerCurrencyUnit = null,
            redemptionRate = BigDecimal("0.05"),
            expireDays = 365,
        )

        val result = config.calculatePointsForVisit(BigDecimal("150"))

        assertEquals(10, result)
    }

    @Test
    fun `calculatePointsForVisit_pointsPerCurrencyUnit_calculatesFromPrice`() {
        val config = LoyaltyConfig(
            enabled = true,
            pointsPerVisit = null,
            pointsPerCurrencyUnit = 10,
            redemptionRate = BigDecimal("0.05"),
            expireDays = 365,
        )

        val result = config.calculatePointsForVisit(BigDecimal("150"))

        assertEquals(15, result)
    }

    @Test
    fun `calculateRedemptionValue_correctCalculation`() {
        val config = LoyaltyConfig(
            enabled = true,
            pointsPerVisit = 10,
            pointsPerCurrencyUnit = null,
            redemptionRate = BigDecimal("0.05"),
            expireDays = 365,
        )

        val result = config.calculateRedemptionValue(100)

        assertEquals(BigDecimal("5.00"), result)
    }

    @Test
    fun `validate_enabled_noRedemptionRate_returnsFailure`() {
        val config = LoyaltyConfig(
            enabled = true,
            pointsPerVisit = 10,
            pointsPerCurrencyUnit = null,
            redemptionRate = null,
            expireDays = 365,
        )

        val result = config.validate()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidLoyaltyConfigException)
    }

    @Test
    fun `validate_enabled_noPointsSource_returnsFailure`() {
        val config = LoyaltyConfig(
            enabled = true,
            pointsPerVisit = null,
            pointsPerCurrencyUnit = null,
            redemptionRate = BigDecimal("0.05"),
            expireDays = 365,
        )

        val result = config.validate()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidLoyaltyConfigException)
    }

    @Test
    fun `validate_disabled_noValidation_returnsSuccess`() {
        val config = LoyaltyConfig(
            enabled = false,
            pointsPerVisit = null,
            pointsPerCurrencyUnit = null,
            redemptionRate = null,
            expireDays = null,
        )

        val result = config.validate()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validate_negativeRedemptionRate_returnsFailure`() {
        val config = LoyaltyConfig(
            enabled = true,
            pointsPerVisit = 10,
            pointsPerCurrencyUnit = null,
            redemptionRate = BigDecimal("-1"),
            expireDays = 365,
        )

        val result = config.validate()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidLoyaltyConfigException)
    }

    @Test
    fun `calculatePoints_roundingDown`() {
        val config = LoyaltyConfig(
            enabled = true,
            pointsPerVisit = null,
            pointsPerCurrencyUnit = 10,
            redemptionRate = BigDecimal("0.05"),
            expireDays = 365,
        )

        val result = config.calculatePointsForVisit(BigDecimal("155"))

        assertEquals(15, result)
    }
}
