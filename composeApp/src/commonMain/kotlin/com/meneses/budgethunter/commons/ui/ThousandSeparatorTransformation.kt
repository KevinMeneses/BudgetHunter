package com.meneses.budgethunter.commons.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class ThousandSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isBlank()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val parts = originalText.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) parts[1] else ""

        val formattedInteger = integerPart.reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()

        val formattedText = if (decimalPart.isNotEmpty()) {
            "$formattedInteger.$decimalPart"
        } else if (originalText.endsWith(".")) {
            "$formattedInteger."
        } else {
            formattedInteger
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset == 0) return 0
                if (offset > originalText.length) return formattedText.length

                if (originalText.contains(".")) {
                    val decimalIndex = originalText.indexOf(".")
                    if (offset > decimalIndex) {
                        val integerCommas = maxOf(0, (decimalIndex - 1) / 3)
                        return offset + integerCommas
                    }
                }

                val commasBeforeOffset = maxOf(0, (offset - 1) / 3)
                return offset + commasBeforeOffset
            }

            override fun transformedToOriginal(offset: Int): Int {
                var originalOffset = offset
                var commaCount = 0

                for (i in 0 until minOf(offset, formattedText.length)) {
                    if (formattedText[i] == ',') {
                        commaCount++
                    }
                }

                originalOffset -= commaCount
                return maxOf(0, minOf(originalOffset, originalText.length))
            }
        }

        return TransformedText(
            AnnotatedString(formattedText),
            offsetMapping
        )
    }
}
