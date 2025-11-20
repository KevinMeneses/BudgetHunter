package com.meneses.budgethunter.fakes.repository

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

class FakeBudgetEntryRepository : BudgetEntryRepository {
    val createdEntries = mutableListOf<BudgetEntry>()
    val updatedEntries = mutableListOf<BudgetEntry>()

    override suspend fun create(budgetEntry: BudgetEntry) {
        createdEntries.add(budgetEntry)
    }

    override suspend fun update(budgetEntry: BudgetEntry) {
        updatedEntries.add(budgetEntry)
    }
}
