package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BudgetEntryRepository(
    private val localDataSource: BudgetEntryLocalDataSource = BudgetEntryLocalDataSource(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    fun getAllByBudgetId(budgetId: Long) =
        localDataSource.selectAllByBudgetId(budgetId)

    suspend fun create(budgetEntry: BudgetEntry) = withContext(ioDispatcher) {
        localDataSource.create(budgetEntry)
    }

    suspend fun update(budgetEntry: BudgetEntry) = withContext(ioDispatcher) {
        localDataSource.update(budgetEntry)
    }
}
