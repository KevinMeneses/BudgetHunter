package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter

data class BudgetListState(
    val budgetList: List<Budget> = emptyList(),
    val isLoading: Boolean = true,
    val addModalVisibility: Boolean = false,
    val budgetToUpdate: Budget? = null,
    val filterModalVisibility: Boolean = false,
    val joinCollaborationModalVisibility: Boolean = false,
    val isCollaborationActive: Boolean = false,
    val filter: BudgetFilter? = null,
    val navigateToBudget: Budget? = null,
    val collaborationError: String? = null
)
