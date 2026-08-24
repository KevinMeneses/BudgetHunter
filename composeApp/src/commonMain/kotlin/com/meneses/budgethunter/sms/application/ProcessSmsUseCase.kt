package com.meneses.budgethunter.sms.application

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.transaction_added
import budgethunter.composeapp.generated.resources.transaction_added_message
import budgethunter.composeapp.generated.resources.transaction_error
import budgethunter.composeapp.generated.resources.transaction_error_message
import budgethunter.composeapp.generated.resources.transaction_failed
import budgethunter.composeapp.generated.resources.transaction_failed_message
import budgethunter.composeapp.generated.resources.transaction_income_added_message
import budgethunter.composeapp.generated.resources.transaction_no_budget
import budgethunter.composeapp.generated.resources.transaction_no_budget_message
import budgethunter.composeapp.generated.resources.transaction_unrecognized
import budgethunter.composeapp.generated.resources.transaction_unrecognized_message
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.commons.resources.StringResourceProvider
import com.meneses.budgethunter.commons.util.toCurrency
import com.meneses.budgethunter.sms.data.SmsMapper
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import com.meneses.budgethunter.sms.domain.SmsParseResult
import com.meneses.budgethunter.sms.domain.SmsService

class ProcessSmsUseCase(
    private val smsMapper: SmsMapper,
    private val budgetEntryRepository: BudgetEntryRepository,
    private val notificationManager: NotificationManager,
    private val stringResourceProvider: StringResourceProvider
) : SmsService {
    override suspend fun processSms(messageBody: String, bankConfigs: Set<BankSmsConfig>) {
        try {
            val candidates = candidateConfigs(messageBody, bankConfigs)
            if (candidates.isEmpty()) return

            var pendingResult: SmsParseResult = SmsParseResult.Ignored

            for (bankConfig in candidates) {
                val result = smsMapper.smsToBudgetEntry(messageBody, bankConfig)
                if (result is SmsParseResult.Success) {
                    budgetEntryRepository.create(result.entry)
                    notifyEntryAdded(result.entry)
                    return
                }
                pendingResult = mostRelevant(pendingResult, result)
            }

            notifyFailure(pendingResult, messageBody)
        } catch (e: Exception) {
            notificationManager.showNotification(
                title = stringResourceProvider.getString(Res.string.transaction_error),
                message = stringResourceProvider.getString(
                    Res.string.transaction_error_message,
                    e.message.orEmpty()
                )
            )
        }
    }

    /**
     * The banks that name themselves in the message are tried first. When none does, the
     * message still comes from a selected bank (the receiver matched the sender), so it is
     * read with the first configuration instead of being dropped.
     */
    private fun candidateConfigs(messageBody: String, bankConfigs: Set<BankSmsConfig>): List<BankSmsConfig> {
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

    private suspend fun notifyFailure(result: SmsParseResult, messageBody: String) {
        when (result) {
            SmsParseResult.NoDefaultBudget -> notificationManager.showNotification(
                title = stringResourceProvider.getString(Res.string.transaction_no_budget),
                message = stringResourceProvider.getString(Res.string.transaction_no_budget_message)
            )

            SmsParseResult.AmountNotFound -> notificationManager.showNotification(
                title = stringResourceProvider.getString(Res.string.transaction_failed),
                message = stringResourceProvider.getString(Res.string.transaction_failed_message)
            )

            SmsParseResult.Unrecognized -> notificationManager.showNotification(
                title = stringResourceProvider.getString(Res.string.transaction_unrecognized),
                message = stringResourceProvider.getString(
                    Res.string.transaction_unrecognized_message,
                    messageBody.preview()
                )
            )

            // Advertising and reminders are discarded on purpose
            else -> Unit
        }
    }

    private suspend fun notifyEntryAdded(entry: BudgetEntry) {
        val messageResource = when (entry.type) {
            BudgetEntry.Type.INCOME -> Res.string.transaction_income_added_message
            BudgetEntry.Type.OUTCOME -> Res.string.transaction_added_message
        }

        notificationManager.showNotification(
            title = stringResourceProvider.getString(Res.string.transaction_added),
            message = stringResourceProvider.getString(
                messageResource,
                entry.amount.toCurrency().ifEmpty { entry.amount }
            )
        )
    }

    /** Enough of the message for the user to recognize which one could not be read. */
    private fun String.preview(): String {
        val singleLine = trim().replace(Regex("""\s+"""), " ")
        return if (singleLine.length <= PREVIEW_LENGTH) {
            singleLine
        } else {
            singleLine.take(PREVIEW_LENGTH).trimEnd() + "..."
        }
    }

    private companion object {
        const val PREVIEW_LENGTH = 120
    }
}
