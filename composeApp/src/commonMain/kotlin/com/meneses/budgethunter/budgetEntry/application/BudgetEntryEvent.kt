package com.meneses.budgethunter.budgetEntry.application

import org.jetbrains.compose.resources.StringResource

sealed interface BudgetEntryEvent {
    data object NavigateBack : BudgetEntryEvent
    data class ShowNotification(val message: StringResource, val isError: Boolean) : BudgetEntryEvent
}
