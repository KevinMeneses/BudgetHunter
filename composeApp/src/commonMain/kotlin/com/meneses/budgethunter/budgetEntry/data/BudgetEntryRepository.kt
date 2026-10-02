package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.network.BudgetEntryApiService
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.data.sync.SyncResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class BudgetEntryRepository(
    private val localDataSource: BudgetEntryLocalDataSource,
    private val syncManager: BudgetEntrySyncManager,
    private val authRepository: AuthRepository,
    private val budgetEntryApiService: BudgetEntryApiService,
    private val budgetLocalDataSource: BudgetLocalDataSource,
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

    /**
     * Clear all budget entries from local database.
     * Used when signing out to prevent data leaking between users.
     */
    suspend fun clearAllData() = withContext(ioDispatcher) {
        localDataSource.clearAllData()
        syncManager.clearFailedPushes()
    }

    /**
     * Deletes a budget entry locally and remotely (when possible).
     *
     * @param budgetEntry Entry to delete
     */
    suspend fun delete(budgetEntry: BudgetEntry) = withContext(ioDispatcher) {
        if (budgetEntry.id < 0) {
            logger.debug(tag, "Entry ${budgetEntry.id} not persisted; skipping delete")
            return@withContext
        }

        if (authRepository.isAuthenticated()) {
            val entryServerId = budgetEntry.serverId
            val budgetServerId = budgetLocalDataSource.getById(budgetEntry.budgetId)?.serverId

            if (budgetServerId != null && entryServerId != null) {
                budgetEntryApiService
                    .deleteEntry(budgetServerId, entryServerId)
                    .onFailure {
                        logger.warn(tag, "Failed to delete entry from server, proceeding with local deletion", it)
                    }
            }
        }

        localDataSource.delete(budgetEntry.id.toLong()) // Domain Int → SQLite Long for query parameter
    }
}
