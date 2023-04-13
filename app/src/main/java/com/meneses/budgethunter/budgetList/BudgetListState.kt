package com.meneses.budgethunter.budgetList

import com.meneses.budgethunter.model.Budget

data class BudgetListState(
    val budgetList: List<Budget> = emptyList(),
    val addModalVisibility: Boolean = false,
    val filterModalVisibility: Boolean = false,
    val filter: Budget? = null
)