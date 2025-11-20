package com.meneses.budgethunter.budgetDetail.data

import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class BudgetDetailRepository(
    private val budgetLocalDataSource: BudgetLocalDataSource,
    private val entriesLocalDataSource: BudgetEntryLocalDataSource,
    private val ioDispatcher: CoroutineDispatcher,
    private val deleteBudgetUseCase: DeleteBudgetUseCase
) {
    private val cacheMutex = Mutex()

    suspend fun getCachedDetail(): BudgetDetail = cacheMutex.withLock {
        cachedBudgetDetail
    }

    fun getBudgetDetailById(budgetId: Int): Flow<BudgetDetail> =
        budgetLocalDataSource.budgets
            .mapNotNull { budgets ->
                budgets.find { it.id == budgetId }
            }.combine(
                entriesLocalDataSource
                    .selectAllByBudgetId(budgetId.toLong())
            ) { budget, entries ->
                BudgetDetail(budget, entries)
            }.onEach {
                cacheMutex.withLock {
                    if (it != cachedBudgetDetail) {
                        cachedBudgetDetail = it
                    }
                }
            }

    suspend fun getAllFilteredBy(filter: BudgetEntryFilter): BudgetDetail {
        val cached = getCachedDetail()
        return cached.copy(
            entries = entriesLocalDataSource.getAllFilteredBy(filter)
        )
    }

    suspend fun updateBudgetAmount(amount: Double) = withContext(ioDispatcher) {
        val cached = getCachedDetail()
        val budget = cached.budget.copy(amount = amount)
        budgetLocalDataSource.update(budget)
    }

    suspend fun deleteBudget(budgetId: Int) = withContext(ioDispatcher) {
        deleteBudgetUseCase.execute(budgetId.toLong())
    }

    suspend fun deleteEntriesByIds(ids: List<Int>) = withContext(ioDispatcher) {
        val dbIds = ids.map { it.toLong() }
        entriesLocalDataSource.deleteByIds(dbIds)
    }

    companion object {
        private var cachedBudgetDetail = BudgetDetail()

        // For testing purposes only
        internal fun clearCache() {
            cachedBudgetDetail = BudgetDetail()
        }
    }
}
