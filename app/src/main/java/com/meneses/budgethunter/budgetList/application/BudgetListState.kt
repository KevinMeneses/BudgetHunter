package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter

data class BudgetListState(
    val budgetList: List<Budget> = emptyList(),
    val isLoading: Boolean = true,
    val addModalVisibility: Boolean = false,
    val budgetToUpdate: Budget? = null,
    val filter: BudgetFilter? = null,
    val navigateToBudget: Budget? = null,
    val isSearchMode: Boolean = false,
    val searchQuery: String = ""
)
