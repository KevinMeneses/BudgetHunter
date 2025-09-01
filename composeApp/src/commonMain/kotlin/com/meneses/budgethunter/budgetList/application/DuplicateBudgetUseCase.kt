package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class DuplicateBudgetUseCase(
    private val budgetRepository: BudgetRepository,
    private val budgetEntryRepository: BudgetEntryRepository,
    private val defaultDispatcher: CoroutineDispatcher
) {
    suspend fun execute(budget: Budget) = withContext(defaultDispatcher) {
        val updatedBudget = budget.copy(name = budget.name + " (copy)")
        val copyBudgetId = budgetRepository.create(updatedBudget).id
        budgetEntryRepository
            .getAllByBudgetId(budget.id.toLong())
            .firstOrNull()?.forEach {
                val updatedEntry = it.copy(budgetId = copyBudgetId)
                budgetEntryRepository.create(updatedEntry)
            }
    }
}