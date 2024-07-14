package com.meneses.budgethunter.budgetDetail.application

import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter

data class BudgetDetailState(
    val budgetDetail: BudgetDetail = BudgetDetail(),
    val isBudgetModalVisible: Boolean = false,
    val isFilterModalVisible: Boolean = false,
    val isCollaborateModalVisible: Boolean = false,
    val isDeleteBudgetModalVisible: Boolean = false,
    val isDeleteEntriesModalVisible: Boolean = false,
    val filter: BudgetEntryFilter? = null,
    val isSelectionActive: Boolean = false,
    val isCollaborationActive: Boolean = false,
    val collaborationCode: Int? = null,
    val collaborationError: String? = null,
    val goBack: Boolean = false,
    val showEntry: BudgetEntry? = null
)
