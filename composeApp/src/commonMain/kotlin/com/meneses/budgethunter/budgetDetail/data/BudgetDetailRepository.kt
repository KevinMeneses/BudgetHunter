package com.meneses.budgethunter.budgetDetail.data

import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
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
    private val budgetEntryRepository: BudgetEntryRepository,
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
        if (ids.isEmpty()) {
            return@withContext
        }

        // Resolve the current entry models so each deletion can run through the repository
        // (which handles authenticated server deletes before removing the local row).
        val cachedEntries = getCachedDetail().entries
        val idsSet = ids.toSet()
        val entriesToDelete = cachedEntries.filter { it.id in idsSet }

        entriesToDelete.forEach { entry ->
            budgetEntryRepository.delete(entry)
        }

        // If any IDs were missing from the cached snapshot (e.g., stale selection),
        // fall back to direct DAO deletion to keep the database consistent.
        val deletedIds = entriesToDelete.map { it.id }.toSet()
        val missingIds = idsSet.minus(deletedIds)
        if (missingIds.isNotEmpty()) {
            entriesLocalDataSource.deleteByIds(missingIds.map { it.toLong() })
        }
    }

    suspend fun syncEntries(): Result<Unit> {
        val cached = getCachedDetail()
        val budget = cached.budget
        val serverId = budget.serverId
        return if (serverId != null) {
            budgetEntryRepository.sync(budget.id, serverId)
        } else {
            Result.failure(Exception("Budget must sync before syncing entries"))
        }
    }

    companion object {
        private var cachedBudgetDetail = BudgetDetail()
    }
}
