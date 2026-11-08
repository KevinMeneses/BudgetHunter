package com.meneses.budgethunter.settings.application

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.sms.domain.BankSmsConfig
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
}
