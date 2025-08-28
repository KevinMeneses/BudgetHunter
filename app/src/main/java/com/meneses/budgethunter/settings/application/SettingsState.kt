package com.meneses.budgethunter.settings.application

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.bank.BankSmsConfig

data class SettingsState(
    val isSmsReadingEnabled: Boolean = false,
    val defaultBudget: Budget? = null,
    val hasSmsPermission: Boolean = false,
    val isDefaultBudgetSelectorVisible: Boolean = false,
    val allBudgets: List<Budget> = emptyList(),
    val isLoading: Boolean = false,
    val availableBanks: List<BankSmsConfig> = emptyList(),
    val selectedBanks: Set<BankSmsConfig> = emptySet(),
    val isBankSelectorVisible: Boolean = false,
    val isAiProcessingEnabled: Boolean = true,
    val isManualPermissionDialogVisible: Boolean = false
)
