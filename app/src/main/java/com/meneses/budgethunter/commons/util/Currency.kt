package com.meneses.budgethunter.commons.util

import com.meneses.budgethunter.commons.EMPTY
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * Formats a Double as currency with proper decimal handling
 */
fun Double.toCurrency(): String {
    return BigDecimal(this.toString())
        .setScale(2, RoundingMode.HALF_UP)
        .toCurrency()
}

/**
 * Formats a String as currency with validation and proper decimal handling
 */
fun String.toCurrency(): String {
    if (this.isBlank()) return EMPTY

    val cleanAmount = this.replace(",", "").replace("$", "")
    val amount = cleanAmount.toBigDecimalOrNull() ?: return EMPTY

    return amount.setScale(2, RoundingMode.HALF_UP).toCurrency()
}

/**
 * Formats a BigDecimal as currency
 */
private fun BigDecimal.toCurrency(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US) as DecimalFormat

    // If the amount is a whole number, don't show decimals
    if (this.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
        formatter.maximumFractionDigits = 0
        formatter.minimumFractionDigits = 0
    } else {
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 2
    }

    return formatter.format(this)
}
