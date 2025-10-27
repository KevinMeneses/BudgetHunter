package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.network.BudgetEntryApiService
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class BudgetEntryRepository(
    private val localDataSource: BudgetEntryLocalDataSource,
    private val syncManager: BudgetEntrySyncManager,
    private val authRepository: AuthRepository,
    private val budgetEntryApiService: BudgetEntryApiService,
    private val budgetLocalDataSource: BudgetLocalDataSource,
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

    /**
     * Deletes a budget entry locally and remotely (when possible).
     *
     * @param budgetEntry Entry to delete
     */
    suspend fun delete(budgetEntry: BudgetEntry) = withContext(ioDispatcher) {
        if (budgetEntry.id < 0) {
            println("BudgetEntryRepository: Entry ${budgetEntry.id} not persisted; skipping delete")
            return@withContext
        }

        if (authRepository.isAuthenticated()) {
            val entryServerId = budgetEntry.serverId
            val budgetServerId = budgetLocalDataSource.getById(budgetEntry.budgetId)?.serverId

            if (budgetServerId != null && entryServerId != null) {
                try {
                    budgetEntryApiService.deleteEntry(budgetServerId, entryServerId).getOrThrow()
                    println("BudgetEntryRepository: Deleted entry from server (budgetId=$budgetServerId, entryId=$entryServerId)")
                } catch (e: Exception) {
                    println("BudgetEntryRepository: Failed to delete entry from server, proceeding with local deletion - ${e.message}")
                }
            } else {
                println("BudgetEntryRepository: Skipping server delete for entry ${budgetEntry.id} - budgetServerId=$budgetServerId, entryServerId=$entryServerId")
            }
        }

        localDataSource.delete(budgetEntry.id.toLong())
        println("BudgetEntryRepository: Deleted entry locally (id=${budgetEntry.id})")
    }
}
