package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class BudgetEntryRepository(
    private val localDataSource: BudgetEntryLocalDataSource,
    private val syncManager: BudgetEntrySyncManager,
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    fun getAllByBudgetId(budgetId: Long) =
        localDataSource.selectAllByBudgetId(budgetId)

    suspend fun create(budgetEntry: BudgetEntry) = withContext(ioDispatcher) {
        localDataSource.create(budgetEntry)

        if (authRepository.isAuthenticated()) {
            syncManager.syncPendingEntries(budgetEntry.budgetId)
        }
    }

    suspend fun update(budgetEntry: BudgetEntry) = withContext(ioDispatcher) {
        localDataSource.update(budgetEntry)

        if (authRepository.isAuthenticated()) {
            syncManager.syncPendingEntries(budgetEntry.budgetId)
        }
    }

    suspend fun sync(budgetId: Int, budgetServerId: Long): Result<Unit> = withContext(ioDispatcher) {
        syncManager.performFullSync(budgetId, budgetServerId)
    }

    /**
     * Clear all budget entries from local database.
     * Used when signing out to prevent data leaking between users.
     */
    suspend fun clearAllData() = withContext(ioDispatcher) {
        localDataSource.clearAllData()
    }
}
