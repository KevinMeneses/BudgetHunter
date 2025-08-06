package com.meneses.budgethunter.sms

import com.meneses.budgethunter.MyApplication
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.bank.BankSmsConfig
import com.meneses.budgethunter.commons.data.PreferencesManager

class SmsMapper(
    private val preferencesManager: PreferencesManager = MyApplication.preferencesManager
) {
    fun smsToBudgetEntry(messageBody: String, bankConfig: BankSmsConfig): BudgetEntry? {
        val amount = bankConfig.transactionAmountRegex
            ?.find(messageBody)
            ?.groupValues?.get(2)
            ?: return null

        val normalizedAmount = when {
            // Formato tipo colombiano: punto miles, coma decimal → "$560.575,67"
            amount.contains(".") && amount.contains(",") && amount.indexOf(",") > amount.indexOf(
                "."
            ) -> {
                amount.replace(".", "").replace(",", ".")
            }

            // Formato tipo americano: coma miles, punto decimal → "$125,678.00"
            amount.contains(",") && amount.contains(".") && amount.indexOf(".") > amount.indexOf(
                ","
            ) -> {
                amount.replace(",", "")
            }

            // Solo coma (asumimos coma miles) → "$125,678"
            amount.contains(",") && !amount.contains(".") -> {
                amount.replace(",", "")
            }

            // Solo punto (asumimos decimal) → "$125.50"
            amount.contains(".") && !amount.contains(",") -> {
                amount
            }

            else -> amount
        }

        val description = bankConfig.transactionDescriptionRegex
            ?.find(messageBody)
            ?.groups?.first()
            ?.value?.trim()

        return BudgetEntry(
            amount = normalizedAmount,
            description = description ?: "Transacción de ${bankConfig.displayName}",
            type = BudgetEntry.Type.OUTCOME,
            budgetId = preferencesManager.defaultBudgetId
        )
    }
}
