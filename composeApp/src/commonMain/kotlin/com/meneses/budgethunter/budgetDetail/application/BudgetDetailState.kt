package com.meneses.budgethunter.budgetDetail.application

import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter

data class BudgetDetailState(
    val budgetDetail: BudgetDetail = BudgetDetail(),
    val isLoading: Boolean = true,
    val isSyncingEntries: Boolean = false,
    val isBudgetModalVisible: Boolean = false,
    val isFilterModalVisible: Boolean = false,
    val isCollaborateModalVisible: Boolean = false,
    val isDeleteBudgetModalVisible: Boolean = false,
    val isDeleteEntriesModalVisible: Boolean = false,
    val filter: BudgetEntryFilter? = null,
    val isSelectionActive: Boolean = false,
    val listOrder: ListOrder = ListOrder.DEFAULT,
    val isAuthenticated: Boolean = false
) {
    enum class ListOrder {
        DEFAULT,
        AMOUNT_ASCENDANT,
        AMOUNT_DESCENDANT
    }
}
