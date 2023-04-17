package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlinx.coroutines.flow.Flow

interface BudgetEntryRepository {
    val budgetEntries: Flow<List<BudgetEntry>>
    fun getEntriesByBudgetId(id: Int)
    fun getEntriesBy(budgetEntry: BudgetEntry)
    fun putEntry(budgetEntry: BudgetEntry)
}