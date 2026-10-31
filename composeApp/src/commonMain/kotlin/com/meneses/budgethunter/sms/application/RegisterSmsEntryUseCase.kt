package com.meneses.budgethunter.sms.application

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.sms.data.SmsMapper
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import com.meneses.budgethunter.sms.domain.SmsParseResult

/**
 * Turns the text of a bank SMS into a budget entry and stores it, without telling the user.
 *
 * Shared by the Android receiver, which notifies about the outcome, and the iOS shortcut, which
 * answers with a dialog instead. The entry is saved locally first and pushed right after when
 * there is a session, like any other entry.
 */
class RegisterSmsEntryUseCase(
    private val smsMapper: SmsMapper,
    private val budgetEntryRepository: BudgetEntryRepository
) {
    /** Returns [SmsParseResult.Success] once stored, otherwise the failure that says the most. */
    suspend operator fun invoke(messageBody: String, bankConfigs: Collection<BankSmsConfig>): SmsParseResult {
        var pendingResult: SmsParseResult = SmsParseResult.Ignored

        for (bankConfig in candidateConfigs(messageBody, bankConfigs)) {
            val result = smsMapper.smsToBudgetEntry(messageBody, bankConfig)
            if (result is SmsParseResult.Success) {
                budgetEntryRepository.create(result.entry)
                return result
            }
            pendingResult = mostRelevant(pendingResult, result)
        }

        return pendingResult
    }

    /**
     * The banks that name themselves in the message are tried first. When none does, the
     * message still comes from a selected bank (the caller matched the sender), so it is
     * read with the first configuration instead of being dropped.
     */
    private fun candidateConfigs(messageBody: String, bankConfigs: Collection<BankSmsConfig>): List<BankSmsConfig> {
        val named = bankConfigs.filter { bankConfig ->
            bankConfig.senderKeywords.any { keyword ->
                messageBody.contains(keyword, ignoreCase = true)
            }
        }

        return named.ifEmpty { listOfNotNull(bankConfigs.firstOrNull()) }
    }

    /** Keeps the outcome that says the most to the user when several configurations fail. */
    private fun mostRelevant(current: SmsParseResult, candidate: SmsParseResult): SmsParseResult =
        if (relevance(candidate) > relevance(current)) candidate else current

    private fun relevance(result: SmsParseResult): Int = when (result) {
        is SmsParseResult.Success -> 4
        SmsParseResult.NoDefaultBudget -> 3
        SmsParseResult.AmountNotFound -> 2
        SmsParseResult.Unrecognized -> 1
        SmsParseResult.Ignored -> 0
    }
}
