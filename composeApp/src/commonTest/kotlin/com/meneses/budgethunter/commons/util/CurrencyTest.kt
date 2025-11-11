package com.meneses.budgethunter.commons.util

import com.meneses.budgethunter.commons.EMPTY
import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyTest {

    // Double.toCurrency() tests
    @Test
    fun `toCurrency formats zero correctly`() {
        assertEquals("$0", 0.0.toCurrency())
    }

    @Test
    fun `toCurrency formats small amounts with decimals`() {
        assertEquals("$1.50", 1.5.toCurrency())
        assertEquals("$0.99", 0.99.toCurrency())
        assertEquals("$0.01", 0.01.toCurrency())
    }

    @Test
    fun `toCurrency formats whole numbers without decimals`() {
        assertEquals("$100", 100.0.toCurrency())
        assertEquals("$500", 500.0.toCurrency())
    }

    @Test
    fun `toCurrency formats with thousand separators`() {
        assertEquals("$1,000", 1000.0.toCurrency())
        assertEquals("$1,234.56", 1234.56.toCurrency())
        assertEquals("$10,000", 10000.0.toCurrency())
        assertEquals("$100,000", 100000.0.toCurrency())
        assertEquals("$1,000,000", 1000000.0.toCurrency())
    }

    @Test
    fun `toCurrency formats large numbers correctly`() {
        assertEquals("$1,234,567.89", 1234567.89.toCurrency())
        assertEquals("$999,999,999.99", 999999999.99.toCurrency())
    }

    @Test
    fun `toCurrency handles negative numbers`() {
        assertEquals("-$1.50", (-1.5).toCurrency())
        assertEquals("-$100", (-100.0).toCurrency())
        assertEquals("-$1,234.56", (-1234.56).toCurrency())
        assertEquals("-$1,000,000", (-1000000.0).toCurrency())
    }

    @Test
    fun `toCurrency rounds to 2 decimal places`() {
        assertEquals("$1.23", 1.234.toCurrency())
        assertEquals("$1.24", 1.235.toCurrency())
        assertEquals("$1.99", 1.994.toCurrency())
        assertEquals("$2", 1.999.toCurrency())
    }

    @Test
    fun `toCurrency handles very small amounts`() {
        assertEquals("$0.01", 0.005.toCurrency())
        assertEquals("$0", 0.004.toCurrency())
    }

    // String.toCurrency() tests
    @Test
    fun `String toCurrency returns empty for blank string`() {
        assertEquals(EMPTY, "".toCurrency())
        assertEquals(EMPTY, "   ".toCurrency())
    }

    @Test
    fun `String toCurrency removes currency symbols`() {
        assertEquals("$100", "$100".toCurrency())
        assertEquals("$100", "€100".toCurrency())
        assertEquals("$100", "£100".toCurrency())
    }

    @Test
    fun `String toCurrency removes commas from input`() {
        assertEquals("$1,234.56", "1,234.56".toCurrency())
        assertEquals("$1,000", "1,000".toCurrency())
    }

    @Test
    fun `String toCurrency handles valid numeric strings`() {
        assertEquals("$100", "100".toCurrency())
        assertEquals("$1.50", "1.5".toCurrency())
        assertEquals("$1,234.56", "1234.56".toCurrency())
    }

    @Test
    fun `String toCurrency returns empty for invalid strings`() {
        assertEquals(EMPTY, "abc".toCurrency())
        assertEquals(EMPTY, "not a number".toCurrency())
        assertEquals(EMPTY, "12.34.56".toCurrency())
    }

    @Test
    fun `String toCurrency handles negative numbers`() {
        assertEquals("-$100", "-100".toCurrency())
        assertEquals("-$1,234.56", "-1234.56".toCurrency())
    }

    @Test
    fun `String toCurrency handles combined currency symbols and commas`() {
        assertEquals("$1,234.56", "$1,234.56".toCurrency())
        assertEquals("$1,000", "€1,000".toCurrency())
    }

    // Double.toPlainString() tests
    @Test
    fun `toPlainString formats zero correctly`() {
        assertEquals("0", 0.0.toPlainString())
    }

    @Test
    fun `toPlainString formats whole numbers without decimals`() {
        assertEquals("100", 100.0.toPlainString())
        assertEquals("1000", 1000.0.toPlainString())
    }

    @Test
    fun `toPlainString formats decimals correctly`() {
        assertEquals("1.50", 1.5.toPlainString())
        assertEquals("1.23", 1.23.toPlainString())
        assertEquals("0.99", 0.99.toPlainString())
    }

    @Test
    fun `toPlainString does not include thousand separators`() {
        assertEquals("1000", 1000.0.toPlainString())
        assertEquals("10000", 10000.0.toPlainString())
        assertEquals("1234567.89", 1234567.89.toPlainString())
    }

    @Test
    fun `toPlainString does not include currency symbol`() {
        assertEquals("100", 100.0.toPlainString())
        assertEquals("1.50", 1.5.toPlainString())
    }

    @Test
    fun `toPlainString rounds to 2 decimal places`() {
        assertEquals("1.23", 1.234.toPlainString())
        assertEquals("1.24", 1.235.toPlainString())
        assertEquals("2", 1.999.toPlainString())
    }

    @Test
    fun `toPlainString handles negative numbers`() {
        // Note: Based on the implementation, negative numbers may not be handled
        // This test documents the actual behavior
        val result = (-100.0).toPlainString()
        // The implementation uses roundedCents / 100, which should preserve sign
        assertEquals("-100", result)
    }

    @Test
    fun `toPlainString handles large numbers`() {
        assertEquals("1000000", 1000000.0.toPlainString())
        assertEquals("999999999.99", 999999999.99.toPlainString())
    }

    // Edge cases
    @Test
    fun `toCurrency handles decimal values close to rounding boundary`() {
        assertEquals("$1.50", 1.495.toCurrency())
        assertEquals("$1.49", 1.494.toCurrency())
    }

    @Test
    fun `String toCurrency handles leading and trailing spaces`() {
        assertEquals("$100", "  100  ".toCurrency())
        assertEquals("$1.50", "  1.5  ".toCurrency())
    }

    @Test
    fun `toPlainString formats small decimals correctly`() {
        assertEquals("0.01", 0.01.toPlainString())
        assertEquals("0.99", 0.99.toPlainString())
    }
}
