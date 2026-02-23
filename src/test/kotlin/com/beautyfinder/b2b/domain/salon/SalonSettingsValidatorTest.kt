package com.beautyfinder.b2b.domain.salon

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class SalonSettingsValidatorTest {

    private val salonId = UUID.randomUUID()

    private fun createOpeningHours(
        dayOfWeek: DayOfWeek,
        isOpen: Boolean,
        openTime: LocalTime? = null,
        closeTime: LocalTime? = null,
    ) = SalonOpeningHours(
        salonId = salonId,
        dayOfWeek = dayOfWeek,
        isOpen = isOpen,
        openTime = openTime,
        closeTime = closeTime,
    )

    @Test
    fun `validateTimezone_valid_returnsSuccess`() {
        val result = SalonSettingsValidator.validateTimezone("Europe/Warsaw")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateTimezone_invalid_returnsFailure`() {
        val result = SalonSettingsValidator.validateTimezone("Mars/Olympus")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidTimezoneException)
    }

    @Test
    fun `validateCancellationWindow_zeroHours_returnsSuccess`() {
        val result = SalonSettingsValidator.validateCancellationWindow(0)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateCancellationWindow_168hours_returnsSuccess`() {
        val result = SalonSettingsValidator.validateCancellationWindow(168)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateCancellationWindow_negative_returnsFailure`() {
        val result = SalonSettingsValidator.validateCancellationWindow(-1)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidCancellationWindowException)
    }

    @Test
    fun `validateSlotInterval_15min_returnsSuccess`() {
        val result = SalonSettingsValidator.validateSlotInterval(15)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateSlotInterval_17min_returnsFailure`() {
        val result = SalonSettingsValidator.validateSlotInterval(17)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidSlotIntervalException)
    }

    @Test
    fun `validateTaxId_validNip_returnsSuccess`() {
        val result = SalonSettingsValidator.validateTaxId("5252344078")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateTaxId_invalidChecksum_returnsFailure`() {
        val result = SalonSettingsValidator.validateTaxId("5252344079")

        assertTrue(result.isFailure)
    }

    @Test
    fun `validateTaxId_wrongLength_returnsFailure`() {
        val result = SalonSettingsValidator.validateTaxId("123")

        assertTrue(result.isFailure)
    }

    @Test
    fun `validateOpeningHours_validWeek_returnsSuccess`() {
        val hours = listOf(
            createOpeningHours(DayOfWeek.MONDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0)),
            createOpeningHours(DayOfWeek.TUESDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0)),
            createOpeningHours(DayOfWeek.WEDNESDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0)),
            createOpeningHours(DayOfWeek.THURSDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0)),
            createOpeningHours(DayOfWeek.FRIDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0)),
            createOpeningHours(DayOfWeek.SATURDAY, true, LocalTime.of(10, 0), LocalTime.of(14, 0)),
            createOpeningHours(DayOfWeek.SUNDAY, false),
        )

        val result = SalonSettingsValidator.validateOpeningHours(hours)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateOpeningHours_openTimeAfterCloseTime_returnsFailure`() {
        val hours = listOf(
            createOpeningHours(DayOfWeek.MONDAY, true, LocalTime.of(18, 0), LocalTime.of(9, 0)),
        )

        val result = SalonSettingsValidator.validateOpeningHours(hours)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidOpeningHoursException)
    }
}
