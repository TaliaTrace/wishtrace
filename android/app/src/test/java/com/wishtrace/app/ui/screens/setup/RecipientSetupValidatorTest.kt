package com.wishtrace.app.ui.screens.setup

import java.time.LocalDate
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipientSetupValidatorTest {
    private val today = LocalDate.of(2026, 7, 30)

    @Test
    fun blankIdentityShowsInlineErrors() {
        val result = RecipientSetupValidator.validatePerson(
            name = " ",
            relationship = "",
        )

        assertFalse(result.isValid)
        assertEquals("Enter their name", result.nameError)
        assertEquals("Choose or enter a relationship", result.relationshipError)
    }

    @Test
    fun pastDateAndNonPositiveBudgetAreRejected() {
        val result = RecipientSetupValidator.validateOccasion(
            date = today.minusDays(1),
            interests = setOf("Books"),
            budgetText = "0",
            today = today,
        )

        assertFalse(result.isValid)
        assertEquals("Choose today or a future date", result.dateError)
        assertEquals("Budget must be more than zero", result.budgetError)
    }

    @Test
    fun decimalBudgetUsesMinorUnitsWithoutBinaryFloatingPoint() {
        val result = RecipientSetupValidator.validateOccasion(
            date = today.plusDays(12),
            interests = setOf("Books"),
            budgetText = "49.95",
            today = today,
        )

        assertTrue(result.isValid)
        assertEquals(4_995L, result.budgetMinorUnits)
        assertNull(result.dateError)
    }

    @Test
    fun datePickerConversionDoesNotShiftInNegativeTimeZone() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            val utcMillis = LocalDate.of(2026, 8, 11)
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()

            assertEquals(
                LocalDate.of(2026, 8, 11),
                RecipientSetupValidator.datePickerUtcMillisToLocalDate(utcMillis),
            )
        } finally {
            TimeZone.setDefault(previous)
        }
    }
}
