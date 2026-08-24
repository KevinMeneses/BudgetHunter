package com.meneses.budgethunter.sms.domain

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

/**
 * Outcome of reading the text of a bank SMS.
 */
sealed interface SmsAnalysis {
    /** The message reports a movement that could be fully read. */
    data class Transaction(
        val amount: String,
        val description: String?,
        val type: BudgetEntry.Type
    ) : SmsAnalysis

    /**
     * The message is known not to be a movement: promotions, pre approved credits
     * or payment reminders. It is discarded without telling the user.
     */
    data object Ignored : SmsAnalysis

    /**
     * The message comes from the bank but its wording is not covered yet,
     * so it cannot be classified. The user is told about it.
     */
    data object Unrecognized : SmsAnalysis

    /** The message reports a movement but no usable amount could be read from it. */
    data object AmountNotFound : SmsAnalysis
}
