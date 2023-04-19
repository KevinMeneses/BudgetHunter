package com.meneses.budgethunter.budgetEntry.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

data class BudgetEntryState(
    val budgetEntry: BudgetEntry? = null,
    val isDiscardChangesModalVisible: Boolean = false,
    val goBack: Boolean = false
)
