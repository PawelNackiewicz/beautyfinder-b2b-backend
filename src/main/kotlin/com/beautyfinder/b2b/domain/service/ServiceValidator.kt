package com.beautyfinder.b2b.domain.service

import java.math.BigDecimal

object ServiceValidator {

    private val ALLOWED_NAME_PATTERN = Regex("^[\\p{L}\\p{N}\\s()\\-&]+$")

    fun validateServiceName(name: String): Result<Unit> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Service name must not be blank"))
        if (name.length > 100) return Result.failure(IllegalArgumentException("Service name must not exceed 100 characters"))
        if (!ALLOWED_NAME_PATTERN.matches(name)) return Result.failure(IllegalArgumentException("Service name contains invalid characters"))
        return Result.success(Unit)
    }

    fun validateVariantDuration(minutes: Int): Result<Unit> {
        if (minutes < 5 || minutes > 480) return Result.failure(InvalidDurationException(minutes))
        if (minutes % 5 != 0) return Result.failure(InvalidDurationException(minutes))
        return Result.success(Unit)
    }

    fun validatePrice(price: BigDecimal, priceMax: BigDecimal?): Result<Unit> {
        if (price < BigDecimal.ZERO) return Result.failure(InvalidPriceException("Price must be non-negative"))
        if (priceMax != null && priceMax < price) return Result.failure(InvalidPriceException("Max price must be >= min price"))
        return Result.success(Unit)
    }

    fun validateDisplayOrder(order: Int): Result<Unit> {
        if (order < 0) return Result.failure(IllegalArgumentException("Display order must be >= 0"))
        return Result.success(Unit)
    }
}
