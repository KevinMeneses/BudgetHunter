package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.network.BudgetEntryApiService
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.commons.data.network.models.BudgetEntryResponse
import com.meneses.budgethunter.commons.util.toPlainString
import com.meneses.budgethunter.db.BudgetEntryQueries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Handles bidirectional synchronization for budget entries (local <-> backend).
 * Mirrors the behaviour of [BudgetSyncManager] for budget-level synchronization.
 */
class BudgetEntrySyncManager(
    private val localDataSource: BudgetEntryLocalDataSource,
    private val budgetEntryApiService: BudgetEntryApiService,
    private val authRepository: AuthRepository,
    private val budgetEntryQueries: BudgetEntryQueries,
    private val budgetLocalDataSource: BudgetLocalDataSource,
    private val ioDispatcher: CoroutineDispatcher
) {

    /**
     * Pushes all unsynced entries for the given local budget to the server.
     *
     * @param budgetId Local database identifier for the budget owning these entries.
     */
    suspend fun syncPendingEntries(budgetId: Int): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (!authRepository.isAuthenticated()) {
                return@withContext Result.failure(Exception("User not authenticated"))
            }

            val budget = budgetLocalDataSource.getById(budgetId)
            val budgetServerId = budget?.serverId
            if (budgetServerId == null) {
                return@withContext Result.failure(Exception("Budget not yet synced with server"))
            }

            val pendingEntries = budgetEntryQueries
                .selectUnsyncedByBudgetId(budgetId.toLong())
                .executeAsList()
                .toDomain()

            pendingEntries.forEach { entry ->
                if (entry.serverId == null) {
                    pushNewEntry(budgetServerId, entry)
                } else {
                    pushUpdatedEntry(budgetServerId, entry)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pulls entries from the server for the given budget and merges them locally.
     *
     * @param budgetServerId Server identifier of the budget whose entries will be synced.
     * @param localBudgetId Optional local budget ID to avoid additional lookups.
     */
    suspend fun pullEntriesFromServer(
        budgetServerId: Long,
        localBudgetId: Int? = null
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (!authRepository.isAuthenticated()) {
                return@withContext Result.failure(Exception("User not authenticated"))
            }

            val budgetId = localBudgetId ?: budgetLocalDataSource
                .getAllCached()
                .firstOrNull { it.serverId == budgetServerId }
                ?.id

            if (budgetId == null) {
                return@withContext Result.failure(Exception("Local budget not found for server ID $budgetServerId"))
            }

            budgetEntryApiService.getEntries(budgetServerId).fold(
                onSuccess = { serverEntries ->
                    serverEntries.forEach { serverEntry ->
                        mergeServerEntry(budgetId, serverEntry)
                    }
                },
                onFailure = { error ->
                    return@withContext Result.failure(error)
                }
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Performs a full sync by pushing local changes first and then pulling server updates.
     */
    suspend fun performFullSync(budgetId: Int, budgetServerId: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            syncPendingEntries(budgetId).getOrThrow()
            pullEntriesFromServer(budgetServerId, localBudgetId = budgetId).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Syncs entries for all budgets that have been synced to the server.
     * Called during sign-in to ensure all budget entries are up to date.
     */
    suspend fun syncAllBudgetsEntries(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val budgets = budgetLocalDataSource.getAllCached()
            budgets.forEach { budget ->
                budget.serverId?.let { serverId ->
                    performFullSync(
                        budgetId = budget.id,
                        budgetServerId = serverId
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun pushNewEntry(budgetServerId: Long, entry: BudgetEntry) {
        val request = entry.toCreateRequest()
        budgetEntryApiService.createEntry(budgetServerId, request).fold(
            onSuccess = { response ->
                updateLocalEntryFromResponse(entry, response)
            },
            onFailure = { /* Ignore push failures */ }
        )
    }

    private suspend fun pushUpdatedEntry(budgetServerId: Long, entry: BudgetEntry) {
        val entryServerId = entry.serverId
        if (entryServerId == null) {
            pushNewEntry(budgetServerId, entry)
            return
        }

        val request = entry.toUpdateRequest()
        budgetEntryApiService.updateEntry(budgetServerId, entryServerId, request).fold(
            onSuccess = { response ->
                updateLocalEntryFromResponse(entry, response)
            },
            onFailure = { /* Ignore push failures */ }
        )
    }

    private fun mergeServerEntry(localBudgetId: Int, serverEntry: BudgetEntryResponse) {
        // First, try to find existing entry by server ID
        var existingEntry = budgetEntryQueries
            .selectByServerId(serverEntry.id)
            .executeAsOneOrNull()
            ?.toDomain()

        // If not found by server ID, check by unique fields (budgetId + amount + description + creationDate)
        // This handles the case where an entry was just pushed to the server but the serverId
        // update hasn't been committed yet (race condition during sync/SSE)
        if (existingEntry == null) {
            existingEntry = budgetEntryQueries
                .selectByUniqueFields(
                    budgetId = localBudgetId.toLong(),
                    amount = serverEntry.amount,
                    description = serverEntry.description,
                    creationDate = serverEntry.creationDate
                )
                .executeAsOneOrNull()
                ?.toDomain()
        }

        if (existingEntry != null) {
            // Update existing entry with server data
            val updatedEntry = existingEntry.copy(
                amount = serverEntry.amount.toPlainString(),
                description = serverEntry.description,
                category = serverEntry.category.toBudgetEntryCategory(),
                type = serverEntry.type.toBudgetEntryType(),
                isSynced = true,
                serverId = serverEntry.id,
                createdByEmail = serverEntry.createdByEmail,
                updatedByEmail = serverEntry.updatedByEmail,
                creationDate = serverEntry.creationDate,
                modificationDate = serverEntry.modificationDate
            )
            localDataSource.update(updatedEntry)
        } else {
            // Create new entry from server (this is a new entry from another user/device)
            val newEntry = BudgetEntry(
                budgetId = localBudgetId,
                amount = serverEntry.amount.toPlainString(),
                description = serverEntry.description,
                type = serverEntry.type.toBudgetEntryType(),
                category = serverEntry.category.toBudgetEntryCategory(),
                serverId = serverEntry.id,
                isSynced = true,
                createdByEmail = serverEntry.createdByEmail,
                updatedByEmail = serverEntry.updatedByEmail,
                creationDate = serverEntry.creationDate,
                modificationDate = serverEntry.modificationDate
            )
            localDataSource.create(newEntry)
        }
    }

    private fun updateLocalEntryFromResponse(
        localEntry: BudgetEntry,
        response: BudgetEntryResponse
    ) {
        val updatedEntry = localEntry.copy(
            amount = response.amount.toPlainString(),
            description = response.description,
            category = response.category.toBudgetEntryCategory(),
            type = response.type.toBudgetEntryType(),
            serverId = response.id,
            isSynced = true,
            createdByEmail = response.createdByEmail,
            updatedByEmail = response.updatedByEmail,
            creationDate = response.creationDate,
            modificationDate = response.modificationDate
        )
        localDataSource.update(updatedEntry)
    }
}
