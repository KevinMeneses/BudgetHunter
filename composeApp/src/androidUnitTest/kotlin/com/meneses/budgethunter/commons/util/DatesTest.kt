package com.meneses.budgethunter.commons.util

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
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

    // ========== toLocalDateOrNull / relativeTo Tests ==========

    @Test
    fun `toLocalDateOrNull parses a stored date`() {
        assertEquals(LocalDate(2026, 10, 3), "2026-10-03".toLocalDateOrNull())
    }

    @Test
    fun `toLocalDateOrNull gives null for an empty string`() {
        assertNull("".toLocalDateOrNull())
    }

    @Test
    fun `toLocalDateOrNull gives null for an impossible date`() {
        assertNull("2026-13-40".toLocalDateOrNull())
    }

    @Test
    fun `toLocalDateOrNull gives null for another format`() {
        assertNull("03/10/2026".toLocalDateOrNull())
    }

    @Test
    fun `relativeTo is TODAY for the same day`() {
        assertEquals(RelativeDay.TODAY, LocalDate(2026, 10, 3).relativeTo(LocalDate(2026, 10, 3)))
    }

    @Test
    fun `relativeTo is YESTERDAY for the day before`() {
        assertEquals(RelativeDay.YESTERDAY, LocalDate(2026, 10, 2).relativeTo(LocalDate(2026, 10, 3)))
    }

    @Test
    fun `relativeTo is OTHER for two days before`() {
        assertEquals(RelativeDay.OTHER, LocalDate(2026, 10, 1).relativeTo(LocalDate(2026, 10, 3)))
    }

    @Test
    fun `relativeTo is OTHER for tomorrow`() {
        assertEquals(RelativeDay.OTHER, LocalDate(2026, 10, 4).relativeTo(LocalDate(2026, 10, 3)))
    }

    @Test
    fun `relativeTo is YESTERDAY across a month boundary`() {
        assertEquals(RelativeDay.YESTERDAY, LocalDate(2026, 9, 30).relativeTo(LocalDate(2026, 10, 1)))
    }

    @Test
    fun `relativeTo is YESTERDAY across a year boundary`() {
        assertEquals(RelativeDay.YESTERDAY, LocalDate(2025, 12, 31).relativeTo(LocalDate(2026, 1, 1)))
    }
}
