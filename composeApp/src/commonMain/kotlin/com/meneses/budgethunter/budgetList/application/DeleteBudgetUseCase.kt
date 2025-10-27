package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class DeleteBudgetUseCase(
    private val budgetRepository: BudgetRepository,
    private val entriesLocalDataSource: BudgetEntryLocalDataSource,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun execute(budgetId: Long) = withContext(ioDispatcher) {
        budgetRepository.delete(budgetId.toInt())
        entriesLocalDataSource.deleteAllByBudgetId(budgetId)
    }
}
