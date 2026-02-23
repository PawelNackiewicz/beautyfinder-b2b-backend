package com.beautyfinder.b2b.domain.service

import java.math.BigDecimal

data class PriceRange(val min: BigDecimal, val max: BigDecimal?) {
    init {
        require(min >= BigDecimal.ZERO) { "Price must be non-negative" }
        require(max == null || max >= min) { "Max price must be >= min price" }
    }

    fun formatted(currency: String = "PLN"): String =
        if (hasRange) "${min.stripTrailingZeros().toPlainString()}-${max!!.stripTrailingZeros().toPlainString()} $currency"
        else "${min.stripTrailingZeros().toPlainString()} $currency"

    val hasRange: Boolean get() = max != null && max > min
}
