package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetList.domain.Budget

sealed interface BudgetListEvent {
    data class NavigateToBudget(val budget: Budget) : BudgetListEvent
    data object NavigateToSignIn : BudgetListEvent
}
