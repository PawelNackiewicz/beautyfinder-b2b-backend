package com.beautyfinder.b2b.domain.billing

import java.math.BigDecimal
import java.math.RoundingMode

data class Money(val amount: BigDecimal, val currency: String = "PLN") {

    init {
        require(currency.isNotBlank()) { "Currency must not be blank" }
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add different currencies: $currency and ${other.currency}" }
        return Money(amount.add(other.amount).setScale(2, RoundingMode.HALF_UP), currency)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Cannot subtract different currencies: $currency and ${other.currency}" }
        return Money(amount.subtract(other.amount).setScale(2, RoundingMode.HALF_UP), currency)
    }

    operator fun times(multiplier: BigDecimal): Money =
        Money(amount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP), currency)

    fun withVat(vatRate: BigDecimal): Money =
        Money(amount.multiply(BigDecimal.ONE.add(vatRate)).setScale(2, RoundingMode.HALF_UP), currency)

    companion object {
        val ZERO = Money(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))

        fun of(amount: Double): Money =
            Money(BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP))

        fun of(amount: String): Money =
            Money(BigDecimal(amount).setScale(2, RoundingMode.HALF_UP))
    }
}
