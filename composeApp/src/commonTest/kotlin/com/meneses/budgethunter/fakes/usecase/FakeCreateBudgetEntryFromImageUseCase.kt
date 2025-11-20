package com.meneses.budgethunter.fakes.usecase

import com.meneses.budgethunter.budgetEntry.application.ICreateBudgetEntryFromImageUseCase
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

class FakeCreateBudgetEntryFromImageUseCase : ICreateBudgetEntryFromImageUseCase {
    var processedEntry: BudgetEntry? = null

    override suspend fun execute(imageUri: String, budgetEntry: BudgetEntry): BudgetEntry {
        return processedEntry ?: budgetEntry.copy(description = "AI Processed")
    }
}
