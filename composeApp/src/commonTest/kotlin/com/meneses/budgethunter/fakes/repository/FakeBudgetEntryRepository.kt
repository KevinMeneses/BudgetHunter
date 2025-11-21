package com.meneses.budgethunter.fakes.repository

import com.meneses.budgethunter.budgetEntry.data.IBudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeBudgetEntryRepository : IBudgetEntryRepository {
    val createdEntries = mutableListOf<BudgetEntry>()
    val updatedEntries = mutableListOf<BudgetEntry>()

    override fun getAllByBudgetId(budgetId: Long): Flow<List<BudgetEntry>> {
        return flowOf(emptyList())
    }

    override suspend fun create(budgetEntry: BudgetEntry) {
        createdEntries.add(budgetEntry)
    }

    override suspend fun update(budgetEntry: BudgetEntry) {
        updatedEntries.add(budgetEntry)
    }
}
