package com.meneses.budgethunter.sms.domain

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

/**
 * Outcome of turning a bank SMS into a budget entry.
 */
sealed interface SmsParseResult {
    data class Success(val entry: BudgetEntry) : SmsParseResult

    /** The SMS is known not to report a movement, there is nothing to notify about. */
    data object Ignored : SmsParseResult

    /** The SMS could not be interpreted, the user has to be told to avoid losing a movement. */
    data object Unrecognized : SmsParseResult

    /** The SMS reports a movement but the amount could not be read. */
    data object AmountNotFound : SmsParseResult

    /** The SMS reports a movement but there is no budget configured to store it in. */
    data object NoDefaultBudget : SmsParseResult
}
