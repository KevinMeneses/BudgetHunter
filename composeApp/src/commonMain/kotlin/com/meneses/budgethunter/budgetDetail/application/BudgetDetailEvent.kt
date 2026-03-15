package com.meneses.budgethunter.budgetDetail.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import org.jetbrains.compose.resources.StringResource

sealed interface BudgetDetailEvent {
    data object NavigateBack : BudgetDetailEvent
    data class ShowEntry(val entry: BudgetEntry) : BudgetDetailEvent
    data class ShowError(val message: StringResource) : BudgetDetailEvent
}
