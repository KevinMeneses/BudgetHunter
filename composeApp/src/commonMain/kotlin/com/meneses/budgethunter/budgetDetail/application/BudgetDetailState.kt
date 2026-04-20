package com.meneses.budgethunter.budgetDetail.application

import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter

data class BudgetDetailState(
    val budgetDetail: BudgetDetail = BudgetDetail(),
    val isLoading: Boolean = true,
    val isSyncingEntries: Boolean = false,
    val modal: ModalState = ModalState.None,
    val filter: BudgetEntryFilter? = null,
    val isSelectionActive: Boolean = false,
    val listOrder: ListOrder = ListOrder.DEFAULT,
    val isAuthenticated: Boolean = false
) {
    sealed interface ModalState {
        data object None : ModalState
        data object Filter : ModalState
        data object DeleteBudget : ModalState
        data object DeleteEntries : ModalState
        data object Budget : ModalState
    }

    enum class ListOrder {
        DEFAULT,
        AMOUNT_ASCENDANT,
        AMOUNT_DESCENDANT
    }
}
