package com.meneses.budgethunter.commons.util

import com.meneses.budgethunter.commons.EMPTY

/**
 * Formats a Double as currency with proper decimal handling
 * Avoids scientific notation for large numbers
 */
fun Double.toCurrency(): String {
    return this.formatAsCurrency()
}

/**
 * Formats a String as currency with validation and proper decimal handling
 */
fun String.toCurrency(): String {
    if (this.isBlank()) return EMPTY

    val cleanAmount = this.replace(",", "").replace("$", "").replace("€", "").replace("£", "")
    val amount = cleanAmount.toDoubleOrNull() ?: return EMPTY

    return amount.formatAsCurrency()
}

/**
 * Formats a Double as currency using proper formatting with thousand separators
 * Handles large numbers without scientific notation
 */
private fun Double.formatAsCurrency(): String {
    // Handle negative numbers
    val isNegative = this < 0
    val absValue = if (isNegative) -this else this

    // Round to 2 decimal places more carefully
    val roundedCents = kotlin.math.round(absValue * 100.0).toLong()
    val integerPart = roundedCents / 100
    val decimalPart = roundedCents % 100

    // Format integer part with thousand separators
    val formattedInteger = integerPart.formatWithThousandSeparators()

    // Format the result
    val formatted = if (decimalPart == 0L) {
        // Whole number, don't show decimals
        "$$formattedInteger"
    } else {
        // Show 2 decimal places
        "$$formattedInteger.${decimalPart.toString().padStart(2, '0')}"
    }

    return if (isNegative) "-$formatted" else formatted
}

/**
 * Formats a long integer with thousand separators (commas)
 * Supports large numbers without overflow
 */
private fun Long.formatWithThousandSeparators(): String {
    if (this < 1000) return this.toString()

    val str = this.toString()
    val result = StringBuilder()
    var count = 0

    // Process digits from right to left
    for (i in str.length - 1 downTo 0) {
        if (count > 0 && count % 3 == 0) {
            result.insert(0, ',')
        }
        result.insert(0, str[i])
        count++
    }

    return result.toString()
}

/**
 * Formats a Double as a plain number string without scientific notation
 * Used for form fields and data conversion where currency symbols are not needed
 */
fun Double.toPlainString(): String {
    val roundedCents = kotlin.math.round(this * 100.0).toLong()
    val integerPart = roundedCents / 100
    val decimalPart = roundedCents % 100

    return if (decimalPart == 0L) {
        integerPart.toString()
    } else {
        "$integerPart.${decimalPart.toString().padStart(2, '0')}"
    }
}
