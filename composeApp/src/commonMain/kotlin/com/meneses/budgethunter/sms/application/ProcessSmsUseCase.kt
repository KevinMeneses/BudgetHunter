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
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.commons.resources.StringResourceProvider
import com.meneses.budgethunter.commons.util.toCurrency
import com.meneses.budgethunter.sms.domain.BankEmail
import com.meneses.budgethunter.sms.domain.BankNotification
import com.meneses.budgethunter.sms.domain.BankNotificationService
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import com.meneses.budgethunter.sms.domain.SmsParseResult
import com.meneses.budgethunter.sms.domain.SmsService
import kotlinx.coroutines.CancellationException

class ProcessSmsUseCase(
    private val registerSmsEntry: RegisterSmsEntryUseCase,
    private val notificationManager: NotificationManager,
    private val stringResourceProvider: StringResourceProvider
) : SmsService, BankNotificationService {
    override suspend fun processSms(messageBody: String, bankConfigs: Set<BankSmsConfig>) {
        try {
            when (val result = registerSmsEntry(messageBody, bankConfigs)) {
                is SmsParseResult.Success -> notifyEntryAdded(result.entry)
                else -> notifyFailure(result, messageBody)
            }
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

    override suspend fun processBankNotification(notification: BankNotification, bankConfig: BankSmsConfig) {
        reportFailures {
            when (val result = registerSmsEntry.registerNotification(notification, bankConfig)) {
                is SmsParseResult.Success -> notifyEntryAdded(result.entry)
                // Bank apps also notify codes, news and reminders. Unlike an SMS from a bank, a
                // notification nobody can read is far more likely one of those than a movement,
                // so warning about each would only be noise.
                SmsParseResult.Unrecognized -> Unit
                else -> notifyFailure(result, notification.message)
            }
        }
    }

    override suspend fun processBankEmail(email: BankEmail, bankConfig: BankSmsConfig) {
        reportFailures {
            when (val result = registerSmsEntry.registerNotification(email.toNotification(), bankConfig)) {
                is SmsParseResult.Success -> notifyEntryAdded(result.entry)
                // The movement was read but had nowhere to go: the user can fix that
                SmsParseResult.NoDefaultBudget -> notifyFailure(result, email.body)
                // Banks email statements, offers and news far more than movements, and the
                // notification shows only the start of the body, which may stop before the amount:
                // anything else is dropped without a word
                else -> Unit
            }
        }
    }

    private suspend fun reportFailures(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
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

    private suspend fun notifyFailure(result: SmsParseResult, messageBody: String) {
        when (result) {
            SmsParseResult.NoDefaultBudget -> notificationManager.showOpenSettingsNotification(
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

        notificationManager.showEntryAddedNotification(
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
