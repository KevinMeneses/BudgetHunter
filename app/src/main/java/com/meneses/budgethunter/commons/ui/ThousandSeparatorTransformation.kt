package com.meneses.budgethunter.commons.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Visual transformation that adds thousand separators (commas) to numeric input
 */
class ThousandSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isBlank()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        // Split by decimal point if present
        val parts = originalText.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) parts[1] else ""

        // Add commas to integer part
        val formattedInteger = integerPart.reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()

        // Combine with decimal part
        val formattedText = if (decimalPart.isNotEmpty()) {
            "$formattedInteger.$decimalPart"
        } else if (originalText.endsWith(".")) {
            "$formattedInteger."
        } else {
            formattedInteger
        }

        // Create offset mapping to handle cursor position
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset == 0) return 0
                if (offset > originalText.length) return formattedText.length

                // For positions after decimal point, handle separately
                if (originalText.contains(".")) {
                    val decimalIndex = originalText.indexOf(".")
                    if (offset > decimalIndex) {
                        // Position is after decimal point
                        val integerCommas = maxOf(0, (decimalIndex - 1) / 3)
                        return offset + integerCommas
                    }
                }

                // Count commas that would be inserted before this position in integer part
                val commasAdded = if (offset <= 3) 0 else (offset - 1) / 3
                return minOf(offset + commasAdded, formattedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset == 0) return 0
                if (offset > formattedText.length) return originalText.length

                // Count commas before this position in formatted text
                val commasBefore = formattedText.take(offset).count { it == ',' }
                val originalOffset = offset - commasBefore
                return minOf(originalOffset, originalText.length)
            }
        }

        return TransformedText(
            AnnotatedString(formattedText),
            offsetMapping
        )
    }
}
