package com.meneses.budgethunter.commons.util

import com.meneses.budgethunter.commons.EMPTY
import java.text.NumberFormat
import java.util.Locale

fun Double.toCurrency(): String {
    var amountFormatted = NumberFormat
        .getCurrencyInstance(Locale.US)
        .format(this)

    if ((this - toInt()) == 0.0) {
        amountFormatted = amountFormatted.dropLast(3)
    }

    return amountFormatted
}

fun String.toCurrency(): String {
    val amount = toDoubleOrNull() ?: return EMPTY
    var amountFormatted = NumberFormat
        .getCurrencyInstance(Locale.US)
        .format(amount)

    if (amountFormatted.substringAfterLast(".") == "00") {
        amountFormatted = amountFormatted.dropLast(3)
    }

    return amountFormatted
}

fun String.fromCurrency() =
    replace("$", "")
        .replace(",", "")
        .toDoubleOrNull()
        ?.toString()
        ?.let {
            if (it.substringAfterLast(".") == "00") {
                it.dropLast(3)
            } else {
                it
            }
        } ?: EMPTY

