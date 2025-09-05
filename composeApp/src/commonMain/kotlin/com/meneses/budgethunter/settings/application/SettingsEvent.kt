package com.meneses.budgethunter.settings.application

import com.meneses.budgethunter.budgetList.domain.Budget

sealed class SettingsEvent {
    data object ShowDefaultBudgetSelector : SettingsEvent()
    data object HideDefaultBudgetSelector : SettingsEvent()
    data class SetDefaultBudget(val budget: Budget) : SettingsEvent()
    data class ToggleAiProcessing(val enabled: Boolean) : SettingsEvent()
}
