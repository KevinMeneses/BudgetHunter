package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.data.sync.SyncResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class BudgetEntryRepository(
    private val localDataSource: BudgetEntryLocalDataSource,
    private val syncManager: BudgetEntrySyncManager,
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger
) {
    private val tag = "BudgetEntryRepository"

    val budgetsWithFailedSync = syncManager.budgetsWithFailedPush

    fun getAllByBudgetId(budgetId: Long) =
        localDataSource.selectAllByBudgetId(budgetId)

    suspend fun create(budgetEntry: BudgetEntry) = withContext(ioDispatcher) {
        localDataSource.create(budgetEntry)
        pushPendingEntries(budgetEntry.budgetId, operation = "create")
    }

    suspend fun update(budgetEntry: BudgetEntry) = withContext(ioDispatcher) {
        localDataSource.update(budgetEntry.copy(isSynced = false))
        pushPendingEntries(budgetEntry.budgetId, operation = "update")
    }

    private suspend fun pushPendingEntries(budgetId: Int, operation: String) {
        if (!authRepository.isAuthenticated()) {
            logger.debug(tag, "Not authenticated; entries of budget $budgetId stay pending after $operation")
            return
        }

        when (val result = syncManager.syncPendingEntries(budgetId)) {
            is SyncResult.Success -> Unit
            is SyncResult.PartialSuccess ->
                logger.warn(tag, "Background sync after $operation: ${result.succeeded} succeeded, ${result.failed} failed")
            is SyncResult.Failure -> logger.warn(tag, "Background sync failed after $operation", result.error)
        }
    }

    suspend fun sync(budgetId: Int, budgetServerId: Long): Result<Unit> = withContext(ioDispatcher) {
        when (val syncResult = syncManager.performFullSync(budgetId, budgetServerId)) {
            is SyncResult.Success -> Result.success(Unit)
            is SyncResult.PartialSuccess -> {
                logger.warn(tag, "Partial sync: ${syncResult.succeeded} succeeded, ${syncResult.failed} failed")
                Result.success(Unit)
            }
            is SyncResult.Failure -> Result.failure(syncResult.error)
        }
    }

    /** True when an entry was created or changed here and has not reached the server yet. */
    suspend fun hasUnsyncedChanges(): Boolean = withContext(ioDispatcher) {
        localDataSource.countUnsynced() > 0
    }

    /**
     * Clear all budget entries from local database.
     * Used when signing out to prevent data leaking between users.
     */
    suspend fun clearAllData() = withContext(ioDispatcher) {
        localDataSource.clearAllData()
        syncManager.clearFailedPushes()
    }

    /**
     * Deletes entries of a budget here at once. The deletion of those that reached the server is
     * recorded and pushed now when signed in, or with the next push of the budget if that fails:
     * it never blocks the local deletion, and the pull does not bring them back meanwhile.
     */
    suspend fun delete(budgetId: Int, ids: List<Int>) = withContext(ioDispatcher) {
        val persistedIds = ids.filter { it >= 0 }
        if (persistedIds.isEmpty()) return@withContext

        localDataSource.deleteRecordingPending(
            ids = persistedIds.map { it.toLong() }, // Domain Int → SQLite Long for query parameter
            deletedAt = Clock.System.now().toString()
        )
        pushPendingEntries(budgetId, operation = "delete")
    }
}
