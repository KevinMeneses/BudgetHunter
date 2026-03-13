package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetList.domain.Budget
import org.jetbrains.compose.resources.StringResource

sealed interface BudgetListUiEvent {
    data class NavigateToBudget(val budget: Budget) : BudgetListUiEvent
    data class ShowMessage(
        val message: StringResource,
        val formatArgs: List<Any> = emptyList()
    ) : BudgetListUiEvent
    data class ShowError(
        val message: StringResource,
        val formatArgs: List<Any> = emptyList()
    ) : BudgetListUiEvent
    object NavigateToSignIn : BudgetListUiEvent
}
