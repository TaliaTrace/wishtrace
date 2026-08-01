package com.wishtrace.app.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {
    @Test
    fun formatsMinorUnitsWithoutBinaryFloatingPoint() {
        val money = Money(minorUnits = 6_000, currencyCode = "USD")

        assertEquals("$60.00", money.formatted(Locale.US))
    }

    @Test
    fun rejectsInvalidCurrencyCode() {
        assertThrows(IllegalArgumentException::class.java) {
            Money(minorUnits = 6_000, currencyCode = "usd")
        }
    }
}
