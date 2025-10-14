package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetList.domain.Budget

sealed interface BudgetListEvent {
    data class CreateBudget(val budget: Budget) : BudgetListEvent
    data class UpdateBudget(val budget: Budget) : BudgetListEvent
    data class DuplicateBudget(val budget: Budget) : BudgetListEvent
    data class DeleteBudget(val budgetId: Long) : BudgetListEvent
    data class OpenBudget(val budget: Budget) : BudgetListEvent
    data class ToggleAddModal(val isVisible: Boolean) : BudgetListEvent
    data class ToggleUpdateModal(val budget: Budget?) : BudgetListEvent
    data class ToggleSearchMode(val isSearchMode: Boolean) : BudgetListEvent
    data class UpdateSearchQuery(val query: String) : BudgetListEvent
    data object ClearFilter : BudgetListEvent
    data object ClearNavigation : BudgetListEvent
    data object SignOut : BudgetListEvent
    data object SignIn : BudgetListEvent
    data object ClearSignInNavigation : BudgetListEvent
}