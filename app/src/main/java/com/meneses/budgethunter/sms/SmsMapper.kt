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
            Log.d("SmsMapper", "No bank keyword found in message. Keywords tried: ${bankConfig.senderKeywords}")
            return null
        }

        val amount = extractAmount(messageBody, bankConfig) ?: return null
        val description = extractDescription(messageBody, bankConfig)

        Log.d("SmsMapper", "Extracted amount: $amount, description: $description")

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
                Log.d("SmsMapper", "Bank-specific regex match found: ${amountMatch.value}")
                Log.d("SmsMapper", "Group values: ${amountMatch.groupValues}")
                
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
                    else -> {
                        Log.w("SmsMapper", "No valid amount found in regex groups")
                        null
                    }
                }
                
                if (amount != null) {
                    Log.d("SmsMapper", "Raw amount extracted: $amount")
                    val normalizedAmount = normalizeAmount(amount)
                    Log.d("SmsMapper", "Normalized amount: $normalizedAmount")
                    
                    // Validate that the amount is not zero or empty
                    if (normalizedAmount.isNotEmpty() && normalizedAmount != "0" && normalizedAmount != "0.0") {
                        return normalizedAmount
                    }
                }
            } else {
                Log.d("SmsMapper", "Bank-specific regex failed to match: ${regex.pattern}")
            }
        }
        
        // Fallback: try to find any amount pattern in the message
        Log.d("SmsMapper", "Bank-specific regex failed, trying fallback patterns")
        return extractAmountFallback(messageBody)
    }

    private fun extractAmountFallback(messageBody: String): String? {
        // Common amount patterns that might appear in SMS messages
        val fallbackPatterns = listOf(
            Regex("""\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE), // Basic amount with optional $
            Regex("""(?:valor|monto|por|de)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE), // Amount with keywords
            Regex("""(?:compra|pago|transaccion)\s*(?:por|de)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE), // Transaction amount
            Regex("""\$?\s*([\d.,]+)\s*(?:pesos|cop)""", RegexOption.IGNORE_CASE), // Amount with currency
            Regex("""(?:por|de)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE), // Simple "por/de" amount
            Regex("""([\d.,]+)\s*\$""", RegexOption.IGNORE_CASE) // Amount before $ symbol
        )
        
        for ((index, pattern) in fallbackPatterns.withIndex()) {
            val match = pattern.find(messageBody)
            if (match != null) {
                Log.d("SmsMapper", "Fallback pattern $index match: ${match.value}")
                val amount = match.groupValues.getOrNull(1)
                if (amount != null && amount.isNotEmpty()) {
                    val normalizedAmount = normalizeAmount(amount)
                    if (normalizedAmount.isNotEmpty() && normalizedAmount != "0" && normalizedAmount != "0.0") {
                        Log.d("SmsMapper", "Fallback amount found: $normalizedAmount")
                        return normalizedAmount
                    }
                }
            }
        }
        
        Log.w("SmsMapper", "No amount found with any pattern")
        return null
    }

    private fun extractDescription(messageBody: String, bankConfig: BankSmsConfig): String? {
        val regex = bankConfig.transactionDescriptionRegex ?: return null
        val descriptionMatch = regex.find(messageBody)
        
        if (descriptionMatch != null) {
            Log.d("SmsMapper", "Description match found: ${descriptionMatch.value}")
            Log.d("SmsMapper", "Description groups: ${descriptionMatch.groupValues}")
        } else {
            Log.d("SmsMapper", "Description regex failed to match: ${regex.pattern}")
        }
        
        return descriptionMatch?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun normalizeAmount(amount: String): String {
        // Remove any currency symbols and extra spaces
        val cleanAmount = amount.replace(Regex("""[^\d.,]"""), "").trim()
        
        Log.d("SmsMapper", "Cleaned amount: $cleanAmount")
        
        return when {
            // Formato tipo colombiano: punto miles, coma decimal → "560.575,67"
            cleanAmount.contains(".") && cleanAmount.contains(",") && cleanAmount.indexOf(",") > cleanAmount.indexOf(".") -> {
                val normalized = cleanAmount.replace(".", "").replace(",", ".")
                Log.d("SmsMapper", "Colombian format detected: $cleanAmount -> $normalized")
                normalized
            }

            // Formato tipo americano: coma miles, punto decimal → "125,678.00"
            cleanAmount.contains(",") && cleanAmount.contains(".") && cleanAmount.indexOf(".") > cleanAmount.indexOf(",") -> {
                val normalized = cleanAmount.replace(",", "")
                Log.d("SmsMapper", "American format detected: $cleanAmount -> $normalized")
                normalized
            }

            // Solo coma (asumimos coma miles) → "125,678"
            cleanAmount.contains(",") && !cleanAmount.contains(".") -> {
                val normalized = cleanAmount.replace(",", "")
                Log.d("SmsMapper", "Comma-only format detected: $cleanAmount -> $normalized")
                normalized
            }

            // Solo punto (asumimos decimal) → "125.50"
            cleanAmount.contains(".") && !cleanAmount.contains(",") -> {
                Log.d("SmsMapper", "Dot-only format detected: $cleanAmount")
                cleanAmount
            }

            else -> {
                Log.d("SmsMapper", "No special formatting detected: $cleanAmount")
                cleanAmount
            }
        }
    }
}
