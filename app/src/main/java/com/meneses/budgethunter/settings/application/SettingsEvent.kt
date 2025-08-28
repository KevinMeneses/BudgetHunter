package com.meneses.budgethunter.settings.application

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.bank.BankSmsConfig

sealed interface SettingsEvent {
    data object LoadSettings : SettingsEvent
    data class ToggleSmsReading(val enabled: Boolean) : SettingsEvent
    data class SetDefaultBudget(val budget: Budget) : SettingsEvent
    data object ShowDefaultBudgetSelector : SettingsEvent
    data object HideDefaultBudgetSelector : SettingsEvent
    data object ShowBankSelector : SettingsEvent
    data object HideBankSelector : SettingsEvent
    data class SetSelectedBanks(val bankConfigs: Set<BankSmsConfig>) : SettingsEvent
    data class ToggleAiProcessing(val enabled: Boolean) : SettingsEvent
    data object ShowManualPermissionDialog : SettingsEvent
    data object HideManualPermissionDialog : SettingsEvent
    data object OpenAppSettings : SettingsEvent
}
