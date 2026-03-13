package com.meneses.budgethunter.budgetDetail.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import org.jetbrains.compose.resources.StringResource

sealed interface BudgetDetailUiEvent {
    object NavigateBack : BudgetDetailUiEvent
    data class NavigateToEntry(val entry: BudgetEntry) : BudgetDetailUiEvent
    data class ShowError(
        val message: StringResource,
        val formatArgs: List<Any> = emptyList()
    ) : BudgetDetailUiEvent
    data class ShowMessage(
        val message: StringResource,
        val formatArgs: List<Any> = emptyList()
    ) : BudgetDetailUiEvent
}
