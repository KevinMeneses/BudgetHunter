package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteBudgetUseCase(
    private val budgetLocalDataSource: BudgetLocalDataSource = BudgetLocalDataSource(),
    private val entriesLocalDataSource: BudgetEntryLocalDataSource = BudgetEntryLocalDataSource(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun execute(budgetId: Long) = withContext(ioDispatcher) {
        budgetLocalDataSource.delete(budgetId)
        entriesLocalDataSource.deleteAllByBudgetId(budgetId)
    }
}
