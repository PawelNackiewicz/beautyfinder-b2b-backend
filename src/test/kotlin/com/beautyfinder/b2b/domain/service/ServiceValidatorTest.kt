package com.beautyfinder.b2b.domain.service

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

class ServiceValidatorTest {

    @ParameterizedTest
    @ValueSource(ints = [15, 30, 60, 90])
    fun `validateDuration multiple of 5 returns success`(minutes: Int) {
        assertTrue(ServiceValidator.validateVariantDuration(minutes).isSuccess)
    }

    @ParameterizedTest
    @ValueSource(ints = [17, 23])
    fun `validateDuration not multiple of 5 returns failure`(minutes: Int) {
        assertTrue(ServiceValidator.validateVariantDuration(minutes).isFailure)
    }

    @Test
    fun `validateDuration zero returns failure`() {
        assertTrue(ServiceValidator.validateVariantDuration(0).isFailure)
    }

    @Test
    fun `validateDuration 480 returns success`() {
        assertTrue(ServiceValidator.validateVariantDuration(480).isSuccess)
    }

    @Test
    fun `validatePrice zero returns success`() {
        assertTrue(ServiceValidator.validatePrice(BigDecimal.ZERO, null).isSuccess)
    }

    @Test
    fun `validatePrice negative returns failure`() {
        assertTrue(ServiceValidator.validatePrice(BigDecimal("-1"), null).isFailure)
    }

    @Test
    fun `validatePrice max less than min returns failure`() {
        assertTrue(ServiceValidator.validatePrice(BigDecimal("100"), BigDecimal("50")).isFailure)
    }

    @Test
    fun `validateServiceName valid returns success`() {
        assertTrue(ServiceValidator.validateServiceName("Strzyżenie damskie (premium)").isSuccess)
    }

    @Test
    fun `validateServiceName too long returns failure`() {
        val longName = "A".repeat(101)
        assertTrue(ServiceValidator.validateServiceName(longName).isFailure)
    }

    @Test
    fun `validateServiceName blank returns failure`() {
        assertTrue(ServiceValidator.validateServiceName("   ").isFailure)
    }
}
