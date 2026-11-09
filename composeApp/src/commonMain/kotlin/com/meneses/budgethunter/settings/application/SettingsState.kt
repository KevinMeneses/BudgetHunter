package com.meneses.budgethunter.settings.application

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.sms.domain.BankChannels
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import com.meneses.budgethunter.sms.domain.ReadingChannel
import org.jetbrains.compose.resources.StringResource

data class SettingsState(
    val isSmsReadingEnabled: Boolean = false,
    val defaultBudget: Budget? = null,
    val hasSmsPermission: Boolean = false,
    /** Reading the notifications of bank apps that send no SMS (Nequi, Nu). Android only. */
    val isBankNotificationReadingEnabled: Boolean = false,
    /** Reading the notifications of email apps for the emails of the selected banks. Android only. */
    val isBankEmailReadingEnabled: Boolean = false,
    /** Whether the system lets the app read notifications; granted in the system settings. */
    val hasNotificationListenerAccess: Boolean = false,
    /** Android hands no notification to the listener while the app's own ones are switched off. */
    val areAppNotificationsEnabled: Boolean = true,
    /** Explains why the app's own notifications must be on before sending the user to switch them. */
    val isAppNotificationsDialogVisible: Boolean = false,
    /** Everything is granted but the system left the listener unbound: only a force stop brings it back. */
    val isNotificationReaderStopped: Boolean = false,
    val isNotificationReaderStoppedDialogVisible: Boolean = false,
    val notificationBanks: List<BankSmsConfig> = emptyList(),
    /** Explains what is read before sending the user to the system's notification access screen. */
    val isNotificationAccessDialogVisible: Boolean = false,
    val isDefaultBudgetSelectorVisible: Boolean = false,
    val allBudgets: List<Budget> = emptyList(),
    // True from the first frame: the device's values are read right after creation, and without
    // this the screen would flash its empty defaults before that starts.
    val isLoading: Boolean = true,
    val availableBanks: List<BankSmsConfig> = emptyList(),
    val selectedBanks: Set<BankSmsConfig> = emptySet(),
    /** The channels picked per bank id; a bank missing here is read by its defaults ([BankChannels]). */
    val bankChannels: Map<String, Set<ReadingChannel>> = emptyMap(),
    val isBankSelectorVisible: Boolean = false,
    val isAiProcessingEnabled: Boolean = true,
    val isManualPermissionDialogVisible: Boolean = false,
    /** The account section only makes sense for a signed-in user; offline mode has no account. */
    val isSignedIn: Boolean = false,
    /** False for an account created through Google, which is offered a first password instead. */
    val hasPassword: Boolean = false,
    val isPasswordDialogVisible: Boolean = false,
    val isSavingPassword: Boolean = false,
    val passwordError: StringResource? = null,
    /** The signed-in account, shown at the top of the screen; null in offline mode. */
    val email: String? = null,
    val isSignOutConfirmationVisible: Boolean = false,
    /** Some budgets or entries never reached the server, so the confirmation warns they go. */
    val signOutLosesUnsyncedChanges: Boolean = false,
    val isSigningOut: Boolean = false,
    /** Why the last sign out failed; the user stays signed in and can try again. */
    val signOutError: StringResource? = null
) {
    /** The bank apps and the bank emails are both read by the notification listener. */
    val isNotificationReaderNeeded: Boolean
        get() = isBankNotificationReadingEnabled || isBankEmailReadingEnabled

    val isAnyReadingEnabled: Boolean
        get() = isSmsReadingEnabled || isNotificationReaderNeeded

    fun channelsOf(bank: BankSmsConfig): Set<ReadingChannel> = BankChannels.of(bank, bankChannels)

    fun isReading(channel: ReadingChannel): Boolean = when (channel) {
        ReadingChannel.SMS -> isSmsReadingEnabled
        ReadingChannel.APP -> isBankNotificationReadingEnabled
        ReadingChannel.EMAIL -> isBankEmailReadingEnabled
    }

    /** Some selected bank is read from [channel]. */
    fun hasBankReadBy(channel: ReadingChannel): Boolean = selectedBanks.any { channel in channelsOf(it) }

    /** Some selected bank is read from [channel] while its switch is off, so those alerts are missed. */
    fun isMissing(channel: ReadingChannel): Boolean = !isReading(channel) && hasBankReadBy(channel)

    /**
     * Some selected bank is read from more than one channel that is on: a movement it reports
     * by both is registered once per channel.
     */
    val mayRepeatMovements: Boolean
        get() = selectedBanks.any { bank -> channelsOf(bank).count { isReading(it) } > 1 }
}
