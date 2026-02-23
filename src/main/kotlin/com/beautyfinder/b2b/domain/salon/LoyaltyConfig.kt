package com.beautyfinder.b2b.domain.salon

import java.math.BigDecimal
import java.math.RoundingMode

data class LoyaltyConfig(
    val enabled: Boolean,
    val pointsPerVisit: Int?,
    val pointsPerCurrencyUnit: Int?,
    val redemptionRate: BigDecimal?,
    val expireDays: Int?,
) {
    fun calculatePointsForVisit(finalPrice: BigDecimal): Int {
        if (!enabled) return 0
        if (pointsPerVisit != null) return pointsPerVisit
        if (pointsPerCurrencyUnit != null && pointsPerCurrencyUnit > 0) {
            return finalPrice.divide(BigDecimal(pointsPerCurrencyUnit), 0, RoundingMode.FLOOR).toInt()
        }
        return 0
    }

    fun calculateRedemptionValue(points: Int): BigDecimal {
        if (!enabled || redemptionRate == null) return BigDecimal.ZERO
        return redemptionRate.multiply(BigDecimal(points)).setScale(2, RoundingMode.HALF_UP)
    }

    fun validate(): Result<Unit> {
        if (!enabled) return Result.success(Unit)

        val violations = mutableListOf<String>()

        if (pointsPerVisit == null && pointsPerCurrencyUnit == null) {
            violations.add("Either pointsPerVisit or pointsPerCurrencyUnit must be set when loyalty is enabled")
        }
        if (redemptionRate == null) {
            violations.add("Redemption rate is required when loyalty is enabled")
        } else if (redemptionRate <= BigDecimal.ZERO) {
            violations.add("Redemption rate must be greater than 0")
        }

        return if (violations.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(InvalidLoyaltyConfigException(violations))
        }
    }
}
