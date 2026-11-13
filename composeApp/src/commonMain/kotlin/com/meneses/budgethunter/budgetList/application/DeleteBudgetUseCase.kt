package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetList.data.BudgetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class DeleteBudgetUseCase(
    private val budgetRepository: BudgetRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    /** Deletes the budget with its entries here at once; the server is told when it can be reached. */
    suspend fun execute(budgetId: Long) = withContext(ioDispatcher) {
        budgetRepository.delete(budgetId.toInt()) // UI passes Long; local delete uses domain Int ID
    }
}
