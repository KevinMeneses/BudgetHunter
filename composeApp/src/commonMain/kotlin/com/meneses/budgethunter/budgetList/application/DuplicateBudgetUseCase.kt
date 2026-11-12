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
        // The copy is a new budget nobody else has: it must not carry the original's server id or
        // sync state, or it would never be pushed and two local rows would share one server id.
        val updatedBudget = budget.copy(
            id = -1,
            name = budget.name + " (copy)",
            serverId = null,
            isSynced = false,
            lastSyncedAt = null,
            collaboratorCount = 1
        )
        val copyBudgetId = budgetRepository.create(updatedBudget).id
        budgetEntryRepository
            .getAllByBudgetId(budget.id.toLong())
            .firstOrNull()?.forEach {
                val updatedEntry = it.copy(
                    id = -1,
                    budgetId = copyBudgetId,
                    serverId = null,
                    isSynced = false,
                    createdByEmail = null,
                    updatedByEmail = null,
                    creationDate = null,
                    modificationDate = null
                )
                budgetEntryRepository.create(updatedEntry)
            }
    }
}
