package com.meneses.budgethunter.sms.data

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.sms_transaction_from_bank
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.resources.StringResourceProvider
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import com.meneses.budgethunter.sms.domain.SmsAnalysis
import com.meneses.budgethunter.sms.domain.SmsParseResult
import com.meneses.budgethunter.sms.domain.SmsTransactionParser

class SmsMapper(
    private val preferencesManager: PreferencesManager,
    private val stringResourceProvider: StringResourceProvider
) {
    suspend fun smsToBudgetEntry(messageBody: String, bankConfig: BankSmsConfig): SmsParseResult {
        return try {
            when (val analysis = SmsTransactionParser.analyze(messageBody, bankConfig)) {
                SmsAnalysis.Ignored -> SmsParseResult.Ignored
                SmsAnalysis.Unrecognized -> SmsParseResult.Unrecognized
                SmsAnalysis.AmountNotFound -> SmsParseResult.AmountNotFound
                is SmsAnalysis.Transaction -> toBudgetEntry(analysis, bankConfig)
            }
        } catch (_: Exception) {
            // The message could not be read, better to warn than to lose a movement
            SmsParseResult.Unrecognized
        }
    }

    private suspend fun toBudgetEntry(
        analysis: SmsAnalysis.Transaction,
        bankConfig: BankSmsConfig
    ): SmsParseResult {
        val defaultBudgetId = preferencesManager.getDefaultBudgetId()
        if (defaultBudgetId <= 0) return SmsParseResult.NoDefaultBudget

        val entry = BudgetEntry(
            amount = analysis.amount,
            description = analysis.description ?: stringResourceProvider.getString(
                Res.string.sms_transaction_from_bank,
                bankConfig.displayName
            ),
            type = analysis.type,
            budgetId = defaultBudgetId
        )

        return SmsParseResult.Success(entry)
    }
}
