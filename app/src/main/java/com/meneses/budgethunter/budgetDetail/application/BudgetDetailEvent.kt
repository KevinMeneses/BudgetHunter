package com.meneses.budgethunter.budgetDetail.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetList.domain.Budget

sealed interface BudgetDetailEvent {
    data class SetBudget(val budget: Budget) : BudgetDetailEvent
    object GetBudgetDetail : BudgetDetailEvent
    data class UpdateBudgetAmount(val amount: Double) : BudgetDetailEvent
    data class FilterEntries(val filter: BudgetEntryFilter) : BudgetDetailEvent
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
    data class ToggleCollaborateModal(val isVisible: Boolean) : BudgetDetailEvent
    object HideCodeModal : BudgetDetailEvent
    object StartCollaboration : BudgetDetailEvent
    object StopCollaboration : BudgetDetailEvent
    object ClearNavigation : BudgetDetailEvent
}
