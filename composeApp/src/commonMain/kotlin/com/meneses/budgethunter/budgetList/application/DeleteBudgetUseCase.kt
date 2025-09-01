package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class DeleteBudgetUseCase(
    private val budgetLocalDataSource: BudgetLocalDataSource,
    private val entriesLocalDataSource: BudgetEntryLocalDataSource,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun execute(budgetId: Long) = withContext(ioDispatcher) {
        budgetLocalDataSource.delete(budgetId)
        entriesLocalDataSource.deleteAllByBudgetId(budgetId)
    }
}