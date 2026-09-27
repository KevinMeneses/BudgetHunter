package com.meneses.budgethunter.commons.util

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for the [today] and [String.toCalendarDate] helpers used across
 * budget/entry sync to fill in the user-facing calendar date.
 */
class DatesTest {

    // ========== today() Tests ==========

    @Test
    fun `today returns the current date in yyyy-MM-dd form`() {
        // Given - The date computed the same way production code does
        val expected = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()

        // When
        val result = today()

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun `today returns a 10 character string`() {
        // When
        val result = today()

        // Then
        assertEquals(10, result.length)
    }

    // ========== String.toCalendarDate() Tests ==========

    @Test
    fun `toCalendarDate extracts the calendar date from a backend timestamp`() {
        // Given
        val timestamp = "2026-09-10T21:48:21.123456"

        // When
        val result = timestamp.toCalendarDate()

        // Then
        assertEquals("2026-09-10", result)
    }

    @Test
    fun `toCalendarDate extracts the calendar date from a timestamp with no fractional seconds`() {
        // Given
        val timestamp = "2024-01-01T00:00:00"

        // When
        val result = timestamp.toCalendarDate()

        // Then
        assertEquals("2024-01-01", result)
    }

    @Test
    fun `toCalendarDate returns the same string when it is already a calendar date`() {
        // Given - A value with no 'T' separator, already 10 characters long
        val plainDate = "2026-03-10"

        // When
        val result = plainDate.toCalendarDate()

        // Then
        assertEquals("2026-03-10", result)
    }

    @Test
    fun `toCalendarDate returns null for malformed input instead of a garbage date`() {
        // Given - A string with no 'T' separator that is not a valid calendar date
        val malformed = "not-a-valid-timestamp"

        // When
        val result = malformed.toCalendarDate()

        // Then
        assertNull(result)
    }

    @Test
    fun `toCalendarDate returns null for an empty string`() {
        // When
        val result = "".toCalendarDate()

        // Then
        assertNull(result)
    }

    @Test
    fun `toCalendarDate returns null when the portion before T is too short`() {
        // Given - Truncated/garbled timestamp
        val malformed = "2026-9-1T10:00:00"

        // When
        val result = malformed.toCalendarDate()

        // Then
        assertNull(result)
    }
}
