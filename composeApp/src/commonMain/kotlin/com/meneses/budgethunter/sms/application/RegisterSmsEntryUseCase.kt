package com.meneses.budgethunter.sms.application

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.sms.data.RegisteredMovements
import com.meneses.budgethunter.sms.data.SmsMapper
import com.meneses.budgethunter.sms.domain.BankEmail
import com.meneses.budgethunter.sms.domain.BankNotification
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import com.meneses.budgethunter.sms.domain.MovementFingerprints
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
    private val budgetEntryRepository: BudgetEntryRepository,
    private val registeredMovements: RegisteredMovements
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
     * Registers a movement read from a bank app notification. The app that posted it already
     * says which bank it is, so only that bank is tried, and the entry is described with the
     * notification's text rather than with its title.
     *
     * With its [identity] (on Android the notification's key and time), it is registered once:
     * apps post a notification again, also after the process that read it died. Only a movement
     * that was read and stored is remembered, so a code, an ad or a missing default budget does
     * not keep a copy from being read later.
     */
    suspend fun registerNotification(
        notification: BankNotification,
        bankConfig: BankSmsConfig,
        identity: String? = null
    ): SmsParseResult = register(
        notification,
        bankConfig,
        fingerprintOf = identity?.let { { _: BudgetEntry -> MovementFingerprints.ofAppNotification(it) } }
    )

    /**
     * Registers a movement read from an email of [bankConfig], read like a bank app notification
     * ([BankEmail.toNotification]), once: email apps post an email again while it is in the shade,
     * also after the process that read it died, or show it twice when two apps hold the same
     * mailbox (caught only when both show the same text). A movement already registered is [SmsParseResult.Ignored], even if the user deleted
     * its entry since.
     */
    suspend fun registerEmail(email: BankEmail, bankConfig: BankSmsConfig): SmsParseResult =
        register(
            email.toNotification(bankConfig),
            bankConfig,
            // The whole text shown, not just the part the movement was read from: a bank may write
            // the time after the movement's sentence, and it is what tells two equal purchases apart
            fingerprintOf = { entry ->
                MovementFingerprints.ofEmailMovement(bankConfig, "${email.subject}\n${email.body}", entry, email.receivedAt)
            }
        )

    private suspend fun register(
        notification: BankNotification,
        bankConfig: BankSmsConfig,
        fingerprintOf: ((BudgetEntry) -> String)?
    ): SmsParseResult {
        val result = smsMapper.smsToBudgetEntry(notification.message, bankConfig)
        if (result !is SmsParseResult.Success) return result

        // The text says who the money came from or went to; the app never names itself in it
        val entry = notification.description
            ?.let { result.entry.copy(description = bankConfig.describe(it)) }
            ?: result.entry
        if (fingerprintOf == null) {
            budgetEntryRepository.create(entry)
            return SmsParseResult.Success(entry)
        }
        // Remembered as soon as the row is stored, before the push, which may wait on the network
        // while the process can be killed: a failure to store lets the next copy try again
        val registered = registeredMovements.registerOnce(fingerprintOf(entry)) {
            budgetEntryRepository.createLocally(entry)
        }
        if (!registered) return SmsParseResult.Ignored
        budgetEntryRepository.pushPending(entry.budgetId)
        return SmsParseResult.Success(entry)
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
