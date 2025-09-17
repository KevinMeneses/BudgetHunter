package com.meneses.budgethunter.sms.data

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import com.meneses.budgethunter.commons.data.PreferencesManager

class SmsMapper(
    private val preferencesManager: PreferencesManager
) {
    suspend fun smsToBudgetEntry(messageBody: String, bankConfig: BankSmsConfig): BudgetEntry? {
        return try {
            if (messageBody.isBlank()) {
                return null
            }

            val containsBankKeyword = bankConfig.senderKeywords.any { keyword ->
                messageBody.contains(keyword, ignoreCase = true)
            }

            if (!containsBankKeyword) {
                // Log could be added here for debugging
                return null
            }

            val amount = extractAmount(messageBody, bankConfig) ?: return null
            val description = extractDescription(messageBody, bankConfig)
            val defaultBudgetId = preferencesManager.getDefaultBudgetId()

            if (defaultBudgetId <= 0) {
                // Invalid default budget ID
                return null
            }

            BudgetEntry(
                amount = amount,
                description = description ?: "Transacción de ${bankConfig.displayName}",
                type = BudgetEntry.Type.OUTCOME,
                budgetId = defaultBudgetId
            )
        } catch (_: Exception) {
            // Return null for any unexpected errors during SMS processing
            // In a production app, this could be logged for debugging
            null
        }
    }

    private fun extractAmount(messageBody: String, bankConfig: BankSmsConfig): String? {
        return try {
            if (messageBody.isBlank()) return null

            // Try bank-specific regex first
            val regex = bankConfig.transactionAmountRegex
            if (regex != null) {
                val amountMatch = regex.find(messageBody)
                if (amountMatch != null) {
                    // Try to find the amount in the captured groups
                    val amount = when {
                        amountMatch.groupValues.size > 2 && amountMatch.groupValues[2].isNotEmpty() -> {
                            // If group 2 exists and is not empty, use it (for patterns like Bancolombia)
                            amountMatch.groupValues[2]
                        }

                        amountMatch.groupValues.size > 1 && amountMatch.groupValues[1].isNotEmpty() -> {
                            // Otherwise use group 1
                            amountMatch.groupValues[1]
                        }

                        else -> null
                    }

                    if (amount != null) {
                        val normalizedAmount = normalizeAmount(amount)

                        // Validate that the amount is not zero or empty
                        if (normalizedAmount.isNotEmpty() && normalizedAmount != "0" && normalizedAmount != "0.0") {
                            return normalizedAmount
                        }
                    }
                }
            }

            extractAmountFallback(messageBody)
        } catch (_: Exception) {
            // Fallback to null if regex processing fails
            null
        }
    }

    private fun extractAmountFallback(messageBody: String): String? {
        return try {
            if (messageBody.isBlank()) return null

            // Common amount patterns that might appear in SMS messages
            val fallbackPatterns = listOf(
                Regex("""\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE), // Basic amount with optional $
                Regex(
                    """(?:valor|monto|por|de)\s*\$?\s*([\d.,]+)""",
                    RegexOption.IGNORE_CASE
                ), // Amount with keywords
                Regex(
                    """(?:compra|pago|transaccion)\s*(?:por|de)\s*\$?\s*([\d.,]+)""",
                    RegexOption.IGNORE_CASE
                ), // Transaction amount
                Regex(
                    """\$?\s*([\d.,]+)\s*(?:pesos|cop)""",
                    RegexOption.IGNORE_CASE
                ), // Amount with currency
                Regex(
                    """(?:por|de)\s*\$?\s*([\d.,]+)""",
                    RegexOption.IGNORE_CASE
                ), // Simple "por/de" amount
                Regex("""([\d.,]+)\s*\$""", RegexOption.IGNORE_CASE) // Amount before $ symbol
            )

            for (pattern in fallbackPatterns) {
                try {
                    val match = pattern.find(messageBody)
                    if (match != null) {
                        val amount = match.groupValues.getOrNull(1)
                        if (!amount.isNullOrEmpty()) {
                            val normalizedAmount = normalizeAmount(amount)
                            if (normalizedAmount.isNotEmpty() && normalizedAmount != "0" && normalizedAmount != "0.0") {
                                return normalizedAmount
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Continue to next pattern if this one fails
                    continue
                }
            }

            null
        } catch (_: Exception) {
            null
        }
    }

    private fun extractDescription(messageBody: String, bankConfig: BankSmsConfig): String? {
        return try {
            if (messageBody.isBlank()) return null
            val regex = bankConfig.transactionDescriptionRegex ?: return null
            val descriptionMatch = regex.find(messageBody)
            descriptionMatch?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizeAmount(amount: String): String {
        return try {
            if (amount.isBlank()) return ""

            // Remove any currency symbols and extra spaces
            val cleanAmount = amount.replace(Regex("""[^\d.,]"""), "").trim()

            if (cleanAmount.isEmpty()) return ""

            when {
                // Formato tipo colombiano: punto miles, coma decimal → "560.575,67"
                cleanAmount.contains(".") && cleanAmount.contains(",") && cleanAmount.indexOf(",") > cleanAmount.indexOf(
                    "."
                ) -> {
                    cleanAmount.replace(".", "").replace(",", ".")
                }

                // Formato tipo americano: coma miles, punto decimal → "125,678.00"
                cleanAmount.contains(",") && cleanAmount.contains(".") && cleanAmount.indexOf(".") > cleanAmount.indexOf(
                    ","
                ) -> {
                    cleanAmount.replace(",", "")
                }

                // Solo coma (asumimos coma miles) → "125,678"
                cleanAmount.contains(",") && !cleanAmount.contains(".") -> {
                    cleanAmount.replace(",", "")
                }

                // Solo punto (asumimos decimal) → "125.50"
                else -> cleanAmount
            }
        } catch (_: Exception) {
            ""
        }
    }
}
