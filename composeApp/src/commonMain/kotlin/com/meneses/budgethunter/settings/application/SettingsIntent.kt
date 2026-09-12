package com.meneses.budgethunter.settings.application

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.sms.domain.BankSmsConfig

sealed interface SettingsIntent {
    data class ToggleSmsReading(val enabled: Boolean) : SettingsIntent
    data class SetDefaultBudget(val budget: Budget) : SettingsIntent
    data object ShowDefaultBudgetSelector : SettingsIntent
    data object HideDefaultBudgetSelector : SettingsIntent
    data object ShowBankSelector : SettingsIntent
    data object HideBankSelector : SettingsIntent
    data class SetSelectedBanks(val bankConfigs: Set<BankSmsConfig>) : SettingsIntent
    data class ToggleAiProcessing(val enabled: Boolean) : SettingsIntent
    data object ShowManualPermissionDialog : SettingsIntent
    data object HideManualPermissionDialog : SettingsIntent
    data object OpenAppSettings : SettingsIntent
    data object ShowPasswordDialog : SettingsIntent
    data object HidePasswordDialog : SettingsIntent
    data class SavePassword(val currentPassword: String, val newPassword: String) : SettingsIntent
}
