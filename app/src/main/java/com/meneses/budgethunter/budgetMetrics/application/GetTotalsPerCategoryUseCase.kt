package com.meneses.budgethunter.budgetMetrics.application

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class GetTotalsPerCategoryUseCase(
    private val budgetEntryLocalDataSource: BudgetEntryLocalDataSource,
    private val defaultDispatcher: CoroutineDispatcher
) {
    suspend fun execute(): Map<BudgetEntry.Category, Double> = withContext(defaultDispatcher) {
        val categories = BudgetEntry
            .getCategories()
            .map { it }
            .associateWith { 0.0 }
            .toMutableMap()

        budgetEntryLocalDataSource
            .getAllCached()
            .forEach {
                val previousAmount = categories[it.category] ?: 0.0
                val amountToAdd = it.amount.toDoubleOrNull() ?: 0.0
                categories[it.category] = previousAmount + amountToAdd
            }

        return@withContext categories.entries
            .filter { it.value != 0.0 }
            .sortedByDescending { it.value }
            .associate { it.key to it.value }
    }
}
