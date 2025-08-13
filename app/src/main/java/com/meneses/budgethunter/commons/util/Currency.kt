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

/**
 * Converts currency string back to a clean numeric string for editing
 */
fun String.fromCurrency(): String {
    if (this.isBlank()) return EMPTY
    
    return this.replace("$", "")
        .replace(",", "")
        .let { cleanAmount ->
            cleanAmount.toBigDecimalOrNull()?.let { amount ->
                // Return as string without unnecessary decimals
                if (amount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
                    amount.setScale(0, RoundingMode.HALF_UP).toString()
                } else {
                    amount.setScale(2, RoundingMode.HALF_UP).toString()
                }
            } ?: EMPTY
        }
}

/**
 * Validates if a string can be converted to a valid currency amount
 */
fun String.isValidCurrencyAmount(): Boolean {
    if (this.isBlank()) return false
    
    val cleanAmount = this.replace(",", "").replace("$", "")
    val amount = cleanAmount.toBigDecimalOrNull() ?: return false
    
    // Check if it's positive and has at most 2 decimal places
    return amount > BigDecimal.ZERO && 
           (cleanAmount.split(".").getOrNull(1)?.length ?: 0) <= 2
}

/**
 * Formats input text for real-time currency editing
 * This function maintains cursor position and allows natural editing
 */
fun String.formatCurrencyInput(): String {
    if (this.isBlank()) return EMPTY
    
    // Remove all non-numeric characters except decimal point
    val cleanInput = this.filter { it.isDigit() || it == '.' }
    
    // Handle multiple decimal points - keep only the first one
    val parts = cleanInput.split(".")
    val cleanAmount = if (parts.size > 2) {
        parts[0] + "." + parts.drop(1).joinToString("")
    } else {
        cleanInput
    }
    
    // Limit decimal places to 2
    val finalAmount = if (cleanAmount.contains(".")) {
        val decimalParts = cleanAmount.split(".")
        val integerPart = decimalParts[0]
        val decimalPart = decimalParts[1].take(2)
        if (decimalPart.isEmpty()) integerPart else "$integerPart.$decimalPart"
    } else {
        cleanAmount
    }
    
    return finalAmount
}

