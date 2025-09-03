package com.meneses.budgethunter.commons.util

import com.meneses.budgethunter.commons.EMPTY
import kotlin.math.roundToInt

/**
 * Formats a Double as currency with proper decimal handling
 */
fun Double.toCurrency(): String {
    return this.toString().toCurrency()
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
 * Formats a Double as currency using basic formatting without platform-specific locale
 */
private fun Double.formatAsCurrency(): String {
    // Round to 2 decimal places
    val roundedAmount = (this * 100.0).roundToInt() / 100.0
    
    // Format with proper decimal places
    val formatted = if (roundedAmount == roundedAmount.toInt().toDouble()) {
        // Whole number, don't show decimals
        "$${roundedAmount.toInt()}"
    } else {
        // Show 2 decimal places
        val integerPart = roundedAmount.toInt()
        val decimalPart = ((roundedAmount - integerPart) * 100).roundToInt()
        "$${integerPart}.${decimalPart.toString().padStart(2, '0')}"
    }
    
    return formatted
}

/**
 * Converts currency string to double value
 */
fun String.currencyToDouble(): Double {
    val cleanAmount = this.replace("$", "").replace(",", "").replace("€", "").replace("£", "")
    return cleanAmount.toDoubleOrNull() ?: 0.0
}
