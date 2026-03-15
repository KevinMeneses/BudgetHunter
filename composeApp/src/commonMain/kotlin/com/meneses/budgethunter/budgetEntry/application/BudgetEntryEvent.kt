package com.meneses.budgethunter.budgetEntry.application

sealed interface BudgetEntryEvent {
    data object NavigateBack : BudgetEntryEvent
    data class ShowNotification(val message: String, val isError: Boolean) : BudgetEntryEvent
}
