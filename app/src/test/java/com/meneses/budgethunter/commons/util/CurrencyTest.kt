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
}
