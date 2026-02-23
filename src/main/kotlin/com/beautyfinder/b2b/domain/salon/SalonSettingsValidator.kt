package com.beautyfinder.b2b.domain.salon

import java.time.ZoneId

object SalonSettingsValidator {

    private val ALLOWED_SLOT_INTERVALS = setOf(5, 10, 15, 20, 30, 60)
    private val NIP_WEIGHTS = intArrayOf(6, 5, 7, 2, 3, 4, 5, 6, 7)

    fun validateTimezone(timezone: String): Result<Unit> =
        try {
            ZoneId.of(timezone)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(InvalidTimezoneException(timezone))
        }

    fun validateCancellationWindow(hours: Int): Result<Unit> =
        if (hours in 0..168) {
            Result.success(Unit)
        } else {
            Result.failure(InvalidCancellationWindowException(hours))
        }

    fun validateSlotInterval(minutes: Int): Result<Unit> =
        if (minutes in ALLOWED_SLOT_INTERVALS) {
            Result.success(Unit)
        } else {
            Result.failure(InvalidSlotIntervalException(minutes))
        }

    fun validateBookingWindow(days: Int): Result<Unit> =
        if (days in 1..365) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Booking window must be between 1 and 365 days"))
        }

    fun validateOpeningHours(hours: List<SalonOpeningHours>): Result<Unit> =
        BusinessHours(hours).validate()

    fun validateLoyaltyConfig(config: LoyaltyConfig): Result<Unit> =
        config.validate()

    fun validateTaxId(taxId: String, country: String = "PL"): Result<Unit> {
        if (country != "PL") return Result.success(Unit)

        val digits = taxId.replace("-", "")
        if (digits.length != 10 || !digits.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("NIP must be exactly 10 digits"))
        }

        val checksum = digits.take(9)
            .mapIndexed { index, c -> c.digitToInt() * NIP_WEIGHTS[index] }
            .sum() % 11

        return if (checksum == digits[9].digitToInt()) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Invalid NIP checksum"))
        }
    }
}
