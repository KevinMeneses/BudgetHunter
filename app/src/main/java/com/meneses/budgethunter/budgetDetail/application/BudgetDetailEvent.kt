package com.meneses.budgethunter.budgetDetail.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

sealed interface BudgetDetailEvent {
    data class UpdateBudgetAmount(val amount: Double) : BudgetDetailEvent
    data class FilterEntries(val filter: BudgetEntry) : BudgetDetailEvent
    data class ShowEntry(val budgetItem: BudgetEntry) : BudgetDetailEvent
    object ClearFilter : BudgetDetailEvent
    object DeleteBudget : BudgetDetailEvent
    object DeleteSelectedEntries : BudgetDetailEvent
    data class ToggleSelectEntry(val index: Int, val isSelected: Boolean) : BudgetDetailEvent
    data class ToggleBudgetModal(val isVisible: Boolean) : BudgetDetailEvent
    data class ToggleFilterModal(val isVisible: Boolean) : BudgetDetailEvent
    data class ToggleDeleteBudgetModal(val isVisible: Boolean) : BudgetDetailEvent
    data class ToggleDeleteEntriesModal(val isVisible: Boolean) : BudgetDetailEvent
    data class ToggleAllEntriesSelection(val isSelected: Boolean) : BudgetDetailEvent
    data class ToggleSelectionState(val isActivated: Boolean) : BudgetDetailEvent
}