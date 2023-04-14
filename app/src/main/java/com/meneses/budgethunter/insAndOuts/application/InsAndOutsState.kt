package com.meneses.budgethunter.insAndOuts.application

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.insAndOuts.domain.BudgetItem

data class InsAndOutsState(
    val budget: Budget = Budget(),
    val itemList: List<BudgetItem> = listOf(),
    val isBudgetModalVisible: Boolean = false,
    val isFilterModalVisible: Boolean = false,
    val isDeleteModalVisible: Boolean = false,
    val filter: BudgetItem.Type? = null
)
