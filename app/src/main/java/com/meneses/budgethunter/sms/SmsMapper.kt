package com.meneses.budgethunter.sms

import android.util.Log
import com.meneses.budgethunter.MyApplication
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.bank.BankSmsConfig
import com.meneses.budgethunter.commons.data.PreferencesManager

class SmsMapper(
    private val preferencesManager: PreferencesManager = MyApplication.preferencesManager
) {
    fun smsToBudgetEntry(messageBody: String, bankConfig: BankSmsConfig): BudgetEntry? {
        val containsBankKeyword = bankConfig.senderKeywords.any { keyword ->
            messageBody.contains(keyword, ignoreCase = true)
        }

        if (!containsBankKeyword) {
            Log.d(
                /* tag = */ "SmsMapper",
                /* msg = */
                "No bank keyword found in message. Keywords tried: ${bankConfig.senderKeywords}"
            )
            return null
        }

        val amount = extractAmount(messageBody, bankConfig) ?: return null
        val description = extractDescription(messageBody, bankConfig)


        return BudgetEntry(
            amount = amount,
            description = description ?: "Transacción de ${bankConfig.displayName}",
            type = BudgetEntry.Type.OUTCOME,
            budgetId = preferencesManager.defaultBudgetId
        )
    }

    private fun extractAmount(messageBody: String, bankConfig: BankSmsConfig): String? {
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

        return extractAmountFallback(messageBody)
    }

    private fun extractAmountFallback(messageBody: String): String? {
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

        for ((index, pattern) in fallbackPatterns.withIndex()) {
            val match = pattern.find(messageBody)
            if (match != null) {
                Log.d("SmsMapper", "Fallback pattern $index match: ${match.value}")
                val amount = match.groupValues.getOrNull(1)
                if (!amount.isNullOrEmpty()) {
                    val normalizedAmount = normalizeAmount(amount)
                    if (normalizedAmount.isNotEmpty() && normalizedAmount != "0" && normalizedAmount != "0.0") {
                        return normalizedAmount
                    }
                }
            }
        }

        return null
    }

    private fun extractDescription(messageBody: String, bankConfig: BankSmsConfig): String? {
        val regex = bankConfig.transactionDescriptionRegex ?: return null
        val descriptionMatch = regex.find(messageBody)
        return descriptionMatch?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun normalizeAmount(amount: String): String {
        // Remove any currency symbols and extra spaces
        val cleanAmount = amount.replace(Regex("""[^\d.,]"""), "").trim()
        return when {
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
    }
}
