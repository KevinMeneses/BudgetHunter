package com.meneses.budgethunter.budgetDetail.application

import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter

data class BudgetDetailState(
    val budgetDetail: BudgetDetail = BudgetDetail(),
    val isLoading: Boolean = true,
    val isSyncingEntries: Boolean = false,
    val modal: ModalState = ModalState.None,
    /** What [budgetDetail]'s entries are narrowed to; kept while the user stays in the budget. */
    val filter: BudgetEntryFilter = BudgetEntryFilter(),
    /** Whether the search and filter bar is shown; hiding it keeps [filter]. */
    val isFilterBarVisible: Boolean = false,
    /** How many entries the budget has, before [filter]. */
    val totalEntryCount: Int = 0,
    val isSelectionActive: Boolean = false,
    val listOrder: ListOrder = ListOrder.DEFAULT,
    val isAuthenticated: Boolean = false
) {
    sealed interface ModalState {
        data object None : ModalState
        data object DeleteBudget : ModalState
        data object DeleteEntries : ModalState
        data object Budget : ModalState
    }

    enum class ListOrder {
        /** Newest first, as the database gives them. */
        DEFAULT,

        /** Smallest amount first, expenses and income alike. */
        AMOUNT_ASCENDANT,

        /** Largest amount first, expenses and income alike. */
        AMOUNT_DESCENDANT
    }
}
