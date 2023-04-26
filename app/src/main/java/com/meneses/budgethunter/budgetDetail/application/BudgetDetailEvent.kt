package com.meneses.budgethunter.budgetDetail.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

sealed interface BudgetDetailEvent {
    data class UpdateBudgetAmount(val amount: Double) : BudgetDetailEvent
    data class FilterEntries(val filter: BudgetEntry) : BudgetDetailEvent
    data class ShowEntry(val budgetItem: BudgetEntry) : BudgetDetailEvent
    data class SelectEntry(val index: Int) : BudgetDetailEvent
    data class UnselectEntry(val index: Int) : BudgetDetailEvent
    object ClearFilter : BudgetDetailEvent
    object DeleteBudget : BudgetDetailEvent
    object ShowBudgetModal : BudgetDetailEvent
    object HideBudgetModal : BudgetDetailEvent
    object ShowFilterModal : BudgetDetailEvent
    object HideFilterModal : BudgetDetailEvent
    object ShowDeleteModal : BudgetDetailEvent
    object HideDeleteModal : BudgetDetailEvent
    object SelectAllEntries : BudgetDetailEvent
    object UnselectAllEntries : BudgetDetailEvent
    object ActivateSelection : BudgetDetailEvent
    object DeactivateSelection : BudgetDetailEvent
}