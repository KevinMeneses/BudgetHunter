package com.meneses.budgethunter.settings.application

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.bank.BankSmsConfig

sealed interface SettingsEvent {
    data class ToggleSmsReading(val enabled: Boolean) : SettingsEvent
    data class SetDefaultBudget(val budget: Budget) : SettingsEvent
    data class HandleSMSPermissionResult(val granted: Boolean) : SettingsEvent
    object ShowDefaultBudgetSelector : SettingsEvent
    object HideDefaultBudgetSelector : SettingsEvent
    object ShowBankSelector : SettingsEvent
    object HideBankSelector : SettingsEvent
    data class SetSelectedBanks(val bankConfigs: Set<BankSmsConfig>) : SettingsEvent
} 
