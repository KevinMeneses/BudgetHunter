package com.meneses.budgethunter.fakes.usecase

import com.meneses.budgethunter.budgetEntry.application.CreateBudgetEntryFromImageUseCase
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

class FakeCreateBudgetEntryFromImageUseCase : CreateBudgetEntryFromImageUseCase(null!!, null!!) {
    var processedEntry: BudgetEntry? = null

    override suspend fun execute(imageUri: String, budgetEntry: BudgetEntry): BudgetEntry {
        return processedEntry ?: budgetEntry.copy(description = "AI Processed")
    }
}
