package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetList.domain.Budget

sealed interface BudgetListIntent {
    data class CreateBudget(val budget: Budget) : BudgetListIntent
    data class UpdateBudget(val budget: Budget) : BudgetListIntent
    data class DuplicateBudget(val budget: Budget) : BudgetListIntent
    data class DeleteBudget(val budgetId: Long) : BudgetListIntent
    data class OpenBudget(val budget: Budget) : BudgetListIntent
    data class ToggleAddModal(val isVisible: Boolean) : BudgetListIntent
    data class ToggleUpdateModal(val budget: Budget?) : BudgetListIntent
    data class ToggleSearchMode(val isSearchMode: Boolean) : BudgetListIntent
    data class UpdateSearchQuery(val query: String) : BudgetListIntent
    data object ClearFilter : BudgetListIntent
    data object SignOut : BudgetListIntent
    data object SignIn : BudgetListIntent
    data object SyncBudgets : BudgetListIntent
}
