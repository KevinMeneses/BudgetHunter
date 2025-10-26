package com.meneses.budgethunter.budgetDetail.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetList.domain.Budget

sealed interface BudgetDetailEvent {
    data class SetBudget(val budget: Budget) : BudgetDetailEvent
    data object GetBudgetDetail : BudgetDetailEvent
    data class UpdateBudgetAmount(val amount: Double) : BudgetDetailEvent
    data class FilterEntries(val filter: BudgetEntryFilter) : BudgetDetailEvent
    data class ShowEntry(val budgetItem: BudgetEntry) : BudgetDetailEvent
    data object ClearFilter : BudgetDetailEvent
    data object DeleteBudget : BudgetDetailEvent
    data object DeleteSelectedEntries : BudgetDetailEvent
    data class ToggleSelectEntry(val index: Int, val isSelected: Boolean) : BudgetDetailEvent
    data class ToggleBudgetModal(val isVisible: Boolean) : BudgetDetailEvent
    data class ToggleFilterModal(val isVisible: Boolean) : BudgetDetailEvent
    data class ToggleDeleteBudgetModal(val isVisible: Boolean) : BudgetDetailEvent
    data class ToggleDeleteEntriesModal(val isVisible: Boolean) : BudgetDetailEvent
    data class ToggleAllEntriesSelection(val isSelected: Boolean) : BudgetDetailEvent
    data class ToggleSelectionState(val isActivated: Boolean) : BudgetDetailEvent
    data object ClearNavigation : BudgetDetailEvent
    data object SortList : BudgetDetailEvent
    data object SyncEntries : BudgetDetailEvent
    data object ClearSyncError : BudgetDetailEvent
}
