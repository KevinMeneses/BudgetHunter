package com.meneses.budgethunter.budgetDetail.application

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

data class InsAndOutsState(
    val budget: Budget = Budget(),
    val entries: List<BudgetEntry> = listOf(),
    val isBudgetModalVisible: Boolean = false,
    val isFilterModalVisible: Boolean = false,
    val isDeleteModalVisible: Boolean = false,
    val filter: BudgetEntry.Type? = null
)
