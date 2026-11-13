package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.data.sync.BudgetSyncManager
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.data.sync.SyncResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class BudgetRepository(
    private val localDataSource: BudgetLocalDataSource,
    private val budgetSyncManager: BudgetSyncManager,
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger
) {
    private val tag = "BudgetRepository"

    val budgets: Flow<List<Budget>>
        get() = localDataSource.budgets

    suspend fun getById(id: Int): Budget? =
        localDataSource.getById(id)

    suspend fun getAllCached(): List<Budget> =
        localDataSource.getAllCached()

    suspend fun getAllFilteredBy(filter: BudgetFilter): List<Budget> =
        localDataSource.getAllFilteredBy(filter)

    suspend fun create(budget: Budget): Budget = withContext(ioDispatcher) {
        val createdBudget = localDataSource.create(budget)

        // Trigger sync in background if authenticated
        if (authRepository.isAuthenticated()) {
            val result = budgetSyncManager.syncPendingBudgets()
            if (result is SyncResult.Failure) {
                logger.warn(tag, "Background sync failed after create", result.error)
            }
        }

        createdBudget
    }

    suspend fun update(budget: Budget) = withContext(ioDispatcher) {
        localDataSource.update(budget)

        // Trigger sync in background if authenticated
        if (authRepository.isAuthenticated()) {
            val result = budgetSyncManager.syncPendingBudgets()
            if (result is SyncResult.Failure) {
                logger.warn(tag, "Background sync failed after update", result.error)
            }
        }
    }

    /**
     * Manually trigger a full sync with the server.
     * Pushes pending local changes and pulls server updates.
     *
     * @return Result indicating success or failure
     */
    suspend fun sync(): Result<Unit> = withContext(ioDispatcher) {
        when (val syncResult = budgetSyncManager.performFullSync()) {
            is SyncResult.Success -> Result.success(Unit)
            is SyncResult.PartialSuccess -> {
                logger.warn(
                    tag = tag,
                    message = "Partial sync: ${syncResult.succeeded} succeeded, ${syncResult.failed} failed"
                )
                Result.success(Unit)
            }

            is SyncResult.Failure -> Result.failure(syncResult.error)
        }
    }

    /**
     * True when a budget was created, changed or deleted here and that has not reached the server
     * yet. Counts the deletions of entries too, which live in the same table.
     */
    suspend fun hasUnsyncedChanges(): Boolean = withContext(ioDispatcher) {
        localDataSource.countUnsynced() > 0 || localDataSource.countPendingDeletions() > 0
    }

    /** Read from the database: the cache behind [getAllCached] may not have caught up yet. */
    suspend fun getByServerId(serverId: Long): Budget? = withContext(ioDispatcher) {
        localDataSource.getByServerId(serverId)
    }

    /**
     * Clear all budgets from local database.
     * Used when signing out to prevent data leaking between users.
     */
    suspend fun clearAllData() = withContext(ioDispatcher) {
        localDataSource.clearAllData()
    }

    /**
     * Drops the budgets that belong to the account that was signed in before, keeping the ones
     * created offline so they get pushed to whoever signs in next.
     */
    suspend fun clearSyncedData() = withContext(ioDispatcher) {
        localDataSource.deleteSynced()
    }

    /**
     * Deletes a budget with its entries here at once. If it reached the server, the deletion is
     * recorded and pushed now when signed in, or with the next sync if that fails (no connection, a
     * server error): it never blocks or undoes the local deletion, and the pull does not bring the
     * budget back meanwhile.
     *
     * @param budgetId Local budget ID to delete
     */
    suspend fun delete(budgetId: Int) = withContext(ioDispatcher) {
        localDataSource.deleteRecordingPending(budgetId, deletedAt = Clock.System.now().toString())

        if (authRepository.isAuthenticated()) {
            val result = budgetSyncManager.syncPendingDeletions()
            if (result !is SyncResult.Success) {
                logger.warn(tag, "Deletion of budget $budgetId not pushed yet; it is retried with the next sync")
            }
        }
    }
}
