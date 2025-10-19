package com.meneses.budgethunter.commons.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyKtTest {

    @Test
    fun `double toCurrency formats positive values with thousand separators`() {
        val formatted = 1234567.89.toCurrency()

        assertEquals("$1,234,567.89", formatted)
    }

    @Test
    fun `double toCurrency keeps negative sign`() {
        val formatted = (-45.0).toCurrency()

        assertEquals("-$45", formatted)
    }

    @Test
    fun `string toCurrency cleans currency symbols and formats with decimals`() {
        val formatted = "$1,200.5".toCurrency()

        assertEquals("$1,200.50", formatted)
    }

    @Test
    fun `string toCurrency returns empty for invalid input`() {
        val formatted = "invalid".toCurrency()

        assertEquals("", formatted)
    }

    @Test
    fun `toPlainString removes trailing decimals when not needed`() {
        val formatted = 1000.0.toPlainString()

        assertEquals("1000", formatted)
    }

    @Test
    fun `toPlainString keeps two decimal digits when required`() {
        val formatted = 1234.567.toPlainString()

        assertEquals("1234.57", formatted)
    }
}
