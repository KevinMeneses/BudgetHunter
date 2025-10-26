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
                println("BudgetEntrySyncManager: Cannot sync entries – user not authenticated")
                return@withContext Result.failure(Exception("User not authenticated"))
            }

            val budget = budgetLocalDataSource.getById(budgetId)
            val budgetServerId = budget?.serverId
            if (budgetServerId == null) {
                println("BudgetEntrySyncManager: Budget $budgetId has no server ID; skipping entry sync")
                return@withContext Result.failure(Exception("Budget not yet synced with server"))
            }

            val pendingEntries = budgetEntryQueries
                .selectUnsyncedByBudgetId(budgetId.toLong())
                .executeAsList()
                .toDomain()

            println("BudgetEntrySyncManager: Found ${pendingEntries.size} unsynced entries for budget $budgetId")

            pendingEntries.forEach { entry ->
                if (entry.serverId == null) {
                    pushNewEntry(budgetServerId, entry)
                } else {
                    pushUpdatedEntry(budgetServerId, entry)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            println("BudgetEntrySyncManager: Unexpected error while syncing pending entries - ${e.message}")
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
                println("BudgetEntrySyncManager: Cannot pull entries – user not authenticated")
                return@withContext Result.failure(Exception("User not authenticated"))
            }

            val budgetId = localBudgetId ?: budgetLocalDataSource
                .getAllCached()
                .firstOrNull { it.serverId == budgetServerId }
                ?.id

            if (budgetId == null) {
                println("BudgetEntrySyncManager: No local budget for server ID $budgetServerId; skipping pull")
                return@withContext Result.failure(Exception("Local budget not found for server ID $budgetServerId"))
            }

            budgetEntryApiService.getEntries(budgetServerId).fold(
                onSuccess = { serverEntries ->
                    println("BudgetEntrySyncManager: Pulled ${serverEntries.size} entries from server for budget $budgetServerId")
                    serverEntries.forEach { mergeServerEntry(budgetId, it) }
                },
                onFailure = { error ->
                    println("BudgetEntrySyncManager: Failed to fetch entries - ${error.message}")
                    return@withContext Result.failure(error)
                }
            )

            Result.success(Unit)
        } catch (e: Exception) {
            println("BudgetEntrySyncManager: Unexpected error while pulling entries - ${e.message}")
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
            println("BudgetEntrySyncManager: Full sync failed - ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun pushNewEntry(budgetServerId: Long, entry: BudgetEntry) {
        val request = entry.toCreateRequest()
        budgetEntryApiService.createEntry(budgetServerId, request).fold(
            onSuccess = { response ->
                println("BudgetEntrySyncManager: Created entry on server with ID ${response.id}")
                updateLocalEntryFromResponse(entry, response)
            },
            onFailure = { error ->
                println("BudgetEntrySyncManager: Failed to create entry ${entry.id} - ${error.message}")
            }
        )
    }

    private suspend fun pushUpdatedEntry(budgetServerId: Long, entry: BudgetEntry) {
        val entryServerId = entry.serverId
        if (entryServerId == null) {
            println("BudgetEntrySyncManager: Entry ${entry.id} missing server ID; treating as new entry")
            pushNewEntry(budgetServerId, entry)
            return
        }

        val request = entry.toUpdateRequest()
        budgetEntryApiService.updateEntry(budgetServerId, entryServerId, request).fold(
            onSuccess = { response ->
                println("BudgetEntrySyncManager: Updated server entry ${response.id}")
                updateLocalEntryFromResponse(entry, response)
            },
            onFailure = { error ->
                println("BudgetEntrySyncManager: Failed to update entry ${entry.id} - ${error.message}")
            }
        )
    }

    private fun mergeServerEntry(localBudgetId: Int, serverEntry: BudgetEntryResponse) {
        val existingEntry = budgetEntryQueries
            .selectByServerId(serverEntry.id)
            .executeAsOneOrNull()
            ?.toDomain()

        if (existingEntry != null) {
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
