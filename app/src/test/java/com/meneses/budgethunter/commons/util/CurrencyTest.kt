package com.meneses.budgethunter.commons.util

import org.junit.Test
import org.junit.Assert.assertEquals

class CurrencyTest {

    @Test
    fun `toCurrency formats whole numbers without decimals`() {
        assertEquals("$100", "100".toCurrency())
        assertEquals("$1,000", "1000".toCurrency())
        assertEquals("$50", 50.0.toCurrency())
    }

    @Test
    fun `toCurrency formats decimal numbers with proper decimals`() {
        assertEquals("$100.50", "100.5".toCurrency())
        assertEquals("$1,234.56", "1234.56".toCurrency())
        assertEquals("$0.99", 0.99.toCurrency())
    }

    @Test
    fun `fromCurrency converts back to clean numeric string`() {
        assertEquals("100", "$100".fromCurrency())
        assertEquals("100.50", "$100.50".fromCurrency())
        assertEquals("1234.56", "$1,234.56".fromCurrency())
    }

    @Test
    fun `formatCurrencyInput handles user input correctly`() {
        assertEquals("123", "123".formatCurrencyInput())
        assertEquals("123.45", "123.45".formatCurrencyInput())
        assertEquals("123.45", "123.456".formatCurrencyInput()) // Limits to 2 decimals
        assertEquals("123", "123.".formatCurrencyInput())
        assertEquals("123.4", "123.4".formatCurrencyInput())
    }

    @Test
    fun `formatCurrencyInput removes invalid characters`() {
        assertEquals("123.45", "abc123.45def".formatCurrencyInput())
        assertEquals("123", "$123".formatCurrencyInput())
        assertEquals("123.45", "1,2,3.4,5".formatCurrencyInput())
    }

    @Test
    fun `isValidCurrencyAmount validates correctly`() {
        assertTrue("100".isValidCurrencyAmount())
        assertTrue("100.50".isValidCurrencyAmount())
        assertTrue("0.01".isValidCurrencyAmount())

        assertFalse("".isValidCurrencyAmount())
        assertFalse("0".isValidCurrencyAmount())
        assertFalse("-100".isValidCurrencyAmount())
        assertFalse("100.123".isValidCurrencyAmount()) // More than 2 decimals
        assertFalse("abc".isValidCurrencyAmount())
    }

    @Test
    fun `currency functions handle edge cases`() {
        assertEquals("", "".toCurrency())
        assertEquals("", "".fromCurrency())
        assertEquals("", "abc".toCurrency())
        assertEquals("", "$".fromCurrency())
        assertEquals("", "".formatCurrencyInput())
    }
}
