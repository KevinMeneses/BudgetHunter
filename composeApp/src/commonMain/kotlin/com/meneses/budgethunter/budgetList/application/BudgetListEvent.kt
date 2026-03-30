package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetList.domain.Budget
import org.jetbrains.compose.resources.StringResource

sealed interface BudgetListEvent {
    data class NavigateToBudget(val budget: Budget) : BudgetListEvent
    data object NavigateToSignIn : BudgetListEvent
    data class ShowMessage(val message: StringResource) : BudgetListEvent
}
