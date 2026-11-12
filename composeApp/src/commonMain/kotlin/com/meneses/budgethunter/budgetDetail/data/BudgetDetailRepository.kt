package com.meneses.budgethunter.budgetDetail.data

import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.data.BudgetRepository
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
    private val budgetEntryRepository: BudgetEntryRepository,
    private val budgetRepository: BudgetRepository,
    private val ioDispatcher: CoroutineDispatcher,
    private val deleteBudgetUseCase: DeleteBudgetUseCase
) {
    private val cacheMutex = Mutex()

    val budgetsWithFailedSync = budgetEntryRepository.budgetsWithFailedSync

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

    suspend fun updateBudgetAmount(amount: Double) = withContext(ioDispatcher) {
        val cached = getCachedDetail()
        val budget = cached.budget.copy(
            amount = amount,
            isSynced = false, // Mark as unsynced so it will be pushed to server
            lastSyncedAt = null
        )
        budgetRepository.update(budget)
    }

    suspend fun deleteBudget(budgetId: Int) = withContext(ioDispatcher) {
        deleteBudgetUseCase.execute(budgetId.toLong())
    }

    /**
     * Deletes the entries by id straight from the database (a stale selection may hold ids the
     * cached detail no longer has), recording the deletion of those on the server to be pushed.
     */
    suspend fun deleteEntriesByIds(ids: List<Int>) = withContext(ioDispatcher) {
        if (ids.isEmpty()) {
            return@withContext
        }
        budgetEntryRepository.delete(budgetId = getCachedDetail().budget.id, ids = ids)
    }

    suspend fun syncEntries(budgetId: Int? = null, serverId: Long? = null): Result<Unit> {
        val (finalBudgetId, finalServerId) = if (budgetId != null && serverId != null) {
            budgetId to serverId
        } else {
            val cached = getCachedDetail()
            cached.budget.id to cached.budget.serverId
        }

        return if (finalServerId != null) {
            budgetEntryRepository.sync(finalBudgetId, finalServerId)
        } else {
            Result.failure(Exception("Budget must sync before syncing entries"))
        }
    }

    companion object {
        private var cachedBudgetDetail = BudgetDetail()

        // For testing purposes only
        internal fun clearCache() {
            cachedBudgetDetail = BudgetDetail()
        }
    }
}
