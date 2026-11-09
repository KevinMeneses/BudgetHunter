package com.meneses.budgethunter.settings.application

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import com.meneses.budgethunter.sms.domain.ReadingChannel

sealed interface SettingsIntent {
    data class ToggleSmsReading(val enabled: Boolean) : SettingsIntent
    data class ToggleBankNotificationReading(val enabled: Boolean) : SettingsIntent
    data class ToggleBankEmailReading(val enabled: Boolean) : SettingsIntent
    data object OpenNotificationAccessSettings : SettingsIntent
    data object HideNotificationAccessDialog : SettingsIntent
    data object OpenAppNotificationSettings : SettingsIntent
    data object HideAppNotificationsDialog : SettingsIntent

    /** Opens the app's system screen, where the user force stops it to get the reader bound again. */
    data object OpenAppInfoToRestartReader : SettingsIntent
    data object HideNotificationReaderStoppedDialog : SettingsIntent

    /** The screen came back to the front, maybe from the system settings: access may have changed. */
    data object RefreshPermissions : SettingsIntent

    data class SetDefaultBudget(val budget: Budget) : SettingsIntent
    data object ShowDefaultBudgetSelector : SettingsIntent
    data object HideDefaultBudgetSelector : SettingsIntent
    data object ShowBankSelector : SettingsIntent
    data object HideBankSelector : SettingsIntent

    /** The banks picked in the selector and, for each, the channels it is read from. */
    data class SetSelectedBanks(
        val bankConfigs: Set<BankSmsConfig>,
        val channels: Map<String, Set<ReadingChannel>> = emptyMap()
    ) : SettingsIntent

    data class ToggleAiProcessing(val enabled: Boolean) : SettingsIntent
    data object ShowManualPermissionDialog : SettingsIntent
    data object HideManualPermissionDialog : SettingsIntent
    data object OpenAppSettings : SettingsIntent
    data object ShowPasswordDialog : SettingsIntent
    data object HidePasswordDialog : SettingsIntent
    data class SavePassword(val currentPassword: String, val newPassword: String) : SettingsIntent
    data class ToggleSignOutConfirmation(val show: Boolean) : SettingsIntent
    data object SignOut : SettingsIntent
    data object DismissSignOutError : SettingsIntent
}
