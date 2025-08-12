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
    data class ToggleJoinCollaborationModal(val isVisible: Boolean) : BudgetListEvent
    data class JoinCollaboration(val collaborationCode: String) : BudgetListEvent
    data class ToggleSearchMode(val isSearchMode: Boolean) : BudgetListEvent
    data class UpdateSearchQuery(val query: String) : BudgetListEvent
    object ClearFilter : BudgetListEvent
    object ClearNavigation : BudgetListEvent
}
