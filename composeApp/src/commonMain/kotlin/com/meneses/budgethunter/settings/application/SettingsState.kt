package com.meneses.budgethunter.settings.application

import com.meneses.budgethunter.budgetList.domain.Budget

data class SettingsState(
    val defaultBudget: Budget? = null,
    val isDefaultBudgetSelectorVisible: Boolean = false,
    val allBudgets: List<Budget> = emptyList(),
    val isLoading: Boolean = false,
    val isAiProcessingEnabled: Boolean = true
)
