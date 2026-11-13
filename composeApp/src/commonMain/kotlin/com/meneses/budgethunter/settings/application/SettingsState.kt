package com.meneses.budgethunter.settings.application

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.sms.domain.BankChannels
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import com.meneses.budgethunter.sms.domain.ReadingChannel
import org.jetbrains.compose.resources.StringResource

data class SettingsState(
    val defaultBudget: Budget? = null,
    val hasSmsPermission: Boolean = false,
    /** Whether the system lets the app read notifications; granted in the system settings. */
    val hasNotificationListenerAccess: Boolean = false,
    /** Android hands no notification to the listener while the app's own ones are switched off. */
    val areAppNotificationsEnabled: Boolean = true,
    /** Explains why the app's own notifications must be on before sending the user to switch them. */
    val isAppNotificationsDialogVisible: Boolean = false,
    /** Everything is granted but the system left the listener unbound: only a force stop brings it back. */
    val isNotificationReaderStopped: Boolean = false,
    val isNotificationReaderStoppedDialogVisible: Boolean = false,
    /** Explains what is read before sending the user to the system's notification access screen. */
    val isNotificationAccessDialogVisible: Boolean = false,
    val isDefaultBudgetSelectorVisible: Boolean = false,
    val allBudgets: List<Budget> = emptyList(),
    // True from the first frame: the device's values are read right after creation, and without
    // this the screen would flash its empty defaults before that starts.
    val isLoading: Boolean = true,
    val availableBanks: List<BankSmsConfig> = emptyList(),
    /** The banks read automatically (Android), each from the channels in [bankChannels]. */
    val selectedBanks: Set<BankSmsConfig> = emptySet(),
    /** The channels picked per bank id; a bank missing here is read by its defaults ([BankChannels]). */
    val bankChannels: Map<String, Set<ReadingChannel>> = emptyMap(),
    /** The list of banks that can still be added. */
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
    val signOutError: StringResource? = null,
    /** True while the plan is asked for, before the delete confirmation shows. */
    val isCheckingPlanForDeletion: Boolean = false,
    val isDeleteAccountConfirmationVisible: Boolean = false,
    /** Whether the confirmation warns that the store subscription is not cancelled with the account. */
    val deleteAccountSubscriptionNotice: SubscriptionNotice = SubscriptionNotice.NONE,
    val isDeletingAccount: Boolean = false,
    /** Why the last deletion failed; the user stays signed in and can try again. */
    val deleteAccountError: StringResource? = null
) {
    enum class SubscriptionNotice {
        NONE,

        /** The account is premium: its subscription keeps being charged until cancelled in the store. */
        PREMIUM,

        /** The plan could not be read: the warning is shown as a possibility rather than skipped. */
        UNKNOWN
    }

    fun channelsOf(bank: BankSmsConfig): Set<ReadingChannel> = BankChannels.of(bank, bankChannels)

    /** Every channel some selected bank is read from: each needs its permission. */
    val channelsInUse: Set<ReadingChannel>
        get() = BankChannels.inUse(selectedBanks, bankChannels)

    val isAnyReadingEnabled: Boolean
        get() = channelsInUse.isNotEmpty()

    val isSmsPermissionMissing: Boolean
        get() = ReadingChannel.SMS in channelsInUse && !hasSmsPermission

    /** The bank apps and the bank emails are both read by the notification listener. */
    val isNotificationReaderNeeded: Boolean
        get() = ReadingChannel.APP in channelsInUse || ReadingChannel.EMAIL in channelsInUse

    /** The selected banks in the order they are listed, by name. */
    val sortedSelectedBanks: List<BankSmsConfig>
        get() = selectedBanks.sortedBy { it.displayName }

    /** The banks that can still be added. */
    val unselectedBanks: List<BankSmsConfig>
        get() = availableBanks.filter { it !in selectedBanks }
}
