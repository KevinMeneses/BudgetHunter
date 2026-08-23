package com.meneses.budgethunter.budgetDetail.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetList.domain.Budget

sealed interface BudgetDetailIntent {
    data class SetBudget(val budget: Budget) : BudgetDetailIntent
    data object GetBudgetDetail : BudgetDetailIntent
    data class UpdateBudgetAmount(val amount: Double) : BudgetDetailIntent
    data class FilterEntries(val filter: BudgetEntryFilter) : BudgetDetailIntent
    data class ShowEntry(val budgetItem: BudgetEntry) : BudgetDetailIntent
    data object ClearFilter : BudgetDetailIntent
    data object DeleteBudget : BudgetDetailIntent
    data object DeleteSelectedEntries : BudgetDetailIntent
    data class ToggleSelectEntry(val index: Int, val isSelected: Boolean) : BudgetDetailIntent
    data class ToggleBudgetModal(val isVisible: Boolean) : BudgetDetailIntent
    data class ToggleFilterModal(val isVisible: Boolean) : BudgetDetailIntent
    data class ToggleDeleteBudgetModal(val isVisible: Boolean) : BudgetDetailIntent
    data class ToggleDeleteEntriesModal(val isVisible: Boolean) : BudgetDetailIntent
    data class ToggleAllEntriesSelection(val isSelected: Boolean) : BudgetDetailIntent
    data class ToggleSelectionState(val isActivated: Boolean) : BudgetDetailIntent
    data object SortList : BudgetDetailIntent
    data object SyncEntries : BudgetDetailIntent
}
