package com.meneses.budgethunter.sms.application

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.sms_shortcut_error
import budgethunter.composeapp.generated.resources.sms_shortcut_expense_registered
import budgethunter.composeapp.generated.resources.sms_shortcut_expense_registered_at
import budgethunter.composeapp.generated.resources.sms_shortcut_ignored
import budgethunter.composeapp.generated.resources.sms_shortcut_income_registered
import budgethunter.composeapp.generated.resources.sms_shortcut_income_registered_from
import budgethunter.composeapp.generated.resources.sms_shortcut_no_budget
import budgethunter.composeapp.generated.resources.sms_shortcut_unrecognized
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.resources.StringResourceProvider
import com.meneses.budgethunter.commons.util.toCurrency
import com.meneses.budgethunter.sms.domain.SmsCounterpartExtractor
import com.meneses.budgethunter.sms.domain.SmsParseResult
import com.meneses.budgethunter.sms.domain.SupportedBanks
import kotlin.coroutines.cancellation.CancellationException

/** What the shortcut answers once the message has been handled. */
data class SmsShortcutReply(
    val isRegistered: Boolean,
    val message: String
)

/**
 * Registers the bank SMS that an iOS Shortcuts automation hands to the app.
 *
 * iOS does not let apps read messages, so the automation itself decides which messages come in:
 * the user filters them by sender or text when setting it up. Every supported bank is therefore
 * tried, and the bank that names itself in the message wins, as on Android.
 */
class RegisterSmsFromShortcutUseCase(
    private val registerSmsEntry: RegisterSmsEntryUseCase,
    private val stringResourceProvider: StringResourceProvider
) {
    suspend operator fun invoke(messageBody: String): SmsShortcutReply = try {
        when (val result = registerSmsEntry(messageBody, SupportedBanks.ALL_BANKS)) {
            is SmsParseResult.Success -> registered(result.entry, messageBody)
            SmsParseResult.NoDefaultBudget -> notRegistered(stringResourceProvider.getString(Res.string.sms_shortcut_no_budget))
            SmsParseResult.Ignored -> notRegistered(stringResourceProvider.getString(Res.string.sms_shortcut_ignored))
            SmsParseResult.AmountNotFound,
            SmsParseResult.Unrecognized -> notRegistered(stringResourceProvider.getString(Res.string.sms_shortcut_unrecognized))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        notRegistered(stringResourceProvider.getString(Res.string.sms_shortcut_error))
    }

    private suspend fun registered(entry: BudgetEntry, messageBody: String): SmsShortcutReply {
        val amount = entry.amount.toCurrency().ifEmpty { entry.amount }
        val counterpart = SmsCounterpartExtractor.extract(messageBody)

        val message = when (entry.type) {
            BudgetEntry.Type.OUTCOME -> if (counterpart != null) {
                stringResourceProvider.getString(Res.string.sms_shortcut_expense_registered_at, amount, counterpart)
            } else {
                stringResourceProvider.getString(Res.string.sms_shortcut_expense_registered, amount)
            }

            BudgetEntry.Type.INCOME -> if (counterpart != null) {
                stringResourceProvider.getString(Res.string.sms_shortcut_income_registered_from, amount, counterpart)
            } else {
                stringResourceProvider.getString(Res.string.sms_shortcut_income_registered, amount)
            }
        }

        return SmsShortcutReply(isRegistered = true, message = message)
    }

    private fun notRegistered(message: String) = SmsShortcutReply(isRegistered = false, message = message)
}
