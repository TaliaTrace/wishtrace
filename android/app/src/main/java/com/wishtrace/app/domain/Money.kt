package com.wishtrace.app.domain

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class Money(
    val minorUnits: Long,
    val currencyCode: String,
) {
    init {
        require(currencyCode.matches(Regex("[A-Z]{3}"))) {
            "currencyCode must be a three-letter ISO 4217 code"
        }
    }

    fun formatted(locale: Locale = Locale.getDefault()): String {
        val currency = Currency.getInstance(currencyCode)
        val amount = BigDecimal.valueOf(minorUnits, currency.defaultFractionDigits)
        return NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
        }.format(amount)
    }
}
