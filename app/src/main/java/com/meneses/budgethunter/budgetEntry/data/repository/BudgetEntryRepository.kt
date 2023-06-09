package com.meneses.budgethunter.budgetEntry.data.repository

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import kotlinx.coroutines.flow.Flow

interface BudgetEntryRepository {
    fun getAll(): List<BudgetEntry>
    fun getAllByBudgetId(budgetId: Int): Flow<List<BudgetEntry>>
    fun getAllFilteredBy(filter: BudgetEntryFilter): List<BudgetEntry>
    fun create(budgetEntry: BudgetEntry)
    fun update(budgetEntry: BudgetEntry)
    fun deleteByIds(ids: List<Int>)
}
