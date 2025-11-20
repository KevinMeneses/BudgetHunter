package com.meneses.budgethunter.budgetEntry.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

interface ICreateBudgetEntryFromImageUseCase {
    suspend fun execute(imageUri: String, budgetEntry: BudgetEntry): BudgetEntry
}
