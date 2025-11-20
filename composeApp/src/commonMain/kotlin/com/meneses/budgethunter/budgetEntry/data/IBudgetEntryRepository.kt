package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlinx.coroutines.flow.Flow

interface IBudgetEntryRepository {
    fun getAllByBudgetId(budgetId: Long): Flow<List<BudgetEntry>>
    suspend fun create(budgetEntry: BudgetEntry)
    suspend fun update(budgetEntry: BudgetEntry)
}
