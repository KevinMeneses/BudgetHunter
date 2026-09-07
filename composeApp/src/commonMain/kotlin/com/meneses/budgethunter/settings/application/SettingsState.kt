package com.meneses.budgethunter.settings.application

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import org.jetbrains.compose.resources.StringResource

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
    val isManualPermissionDialogVisible: Boolean = false,
    /** The account section only makes sense for a signed-in user; offline mode has no account. */
    val isSignedIn: Boolean = false,
    /** False for an account created through Google, which is offered a first password instead. */
    val hasPassword: Boolean = false,
    val isPasswordDialogVisible: Boolean = false,
    val isSavingPassword: Boolean = false,
    val passwordError: StringResource? = null
)
