package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetList.domain.Budget

data class BudgetListState(
    val budgetList: List<Budget> = emptyList(),
    val addModalVisibility: Boolean = false,
    val filterModalVisibility: Boolean = false,
    val filter: Budget? = null,
    val navigateToBudget: Budget? = null
)