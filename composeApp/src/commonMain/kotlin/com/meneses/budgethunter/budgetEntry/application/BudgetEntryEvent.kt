package com.meneses.budgethunter.budgetEntry.application

import org.jetbrains.compose.resources.StringResource

sealed interface BudgetEntryEvent {
    data object NavigateBack : BudgetEntryEvent

    /** @property offersPremium the notice gets a "See Premium" action (no receipt scans left) */
    data class ShowNotification(
        val message: StringResource,
        val isError: Boolean,
        val offersPremium: Boolean = false
    ) : BudgetEntryEvent
}
