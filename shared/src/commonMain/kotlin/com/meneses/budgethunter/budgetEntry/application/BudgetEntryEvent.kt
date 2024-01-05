package com.meneses.budgethunter.budgetEntry.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

sealed interface BudgetEntryEvent {
    data class SetBudgetEntry(val budgetEntry: BudgetEntry) : BudgetEntryEvent
    data object SaveBudgetEntry : BudgetEntryEvent
    data class ValidateChanges(val budgetEntry: BudgetEntry) : BudgetEntryEvent
    data object HideDiscardChangesModal : BudgetEntryEvent
    data object GoBack : BudgetEntryEvent
}
