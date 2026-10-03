package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.network.BudgetEntryApiService
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.commons.data.network.models.BudgetEntryResponse
import com.meneses.budgethunter.commons.data.sync.BaseSyncManager
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.data.sync.SyncException
import com.meneses.budgethunter.commons.data.sync.SyncResult
import com.meneses.budgethunter.commons.data.sync.SyncStats
import com.meneses.budgethunter.commons.util.toCalendarDate
import com.meneses.budgethunter.commons.util.toPlainString
import com.meneses.budgethunter.commons.util.today
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handles bidirectional synchronization for budget entries (local <-> backend).
 * Mirrors the behaviour of [com.meneses.budgethunter.budgetList.data.sync.BudgetSyncManager] for budget-level synchronization.
 */
class BudgetEntrySyncManager(
    private val localDataSource: BudgetEntryLocalDataSource,
    private val budgetEntryApiService: BudgetEntryApiService,
    private val budgetLocalDataSource: BudgetLocalDataSource,
    authRepository: AuthRepository,
    ioDispatcher: CoroutineDispatcher,
    logger: Logger
) : BaseSyncManager(authRepository, ioDispatcher, logger) {

    override val logTag = "BudgetEntrySyncManager"

    // Serializes every push (syncPendingEntries) and pull (pullEntriesFromServer), which overlap when
    // an entry create/update, SSE events, the screen's initial/resume sync or a manual refresh run
    // at the same time. Without it:
    // - two pushes could read the same unsynced entry and POST it twice;
    // - two pulls could both miss an entry in mergeServerEntry and insert it twice;
    // - a pull could see an entry a running push has just POSTed, before the push stores its
    //   serverId locally, and insert it again (the unique-fields fallback in mergeServerEntry can't
    //   match it because an unpushed entry has no creationDate yet).
    private val syncMutex = Mutex()

    private val _budgetsWithFailedPush = MutableStateFlow<Set<Int>>(emptySet())

    /**
     * Local ids of the budgets whose pending entries failed to be pushed on the last attempt. It is
     * state rather than an event so the failure is not lost when no screen is listening (e.g. an
     * entry created from an SMS while the app is in background), and it is tracked here so every
     * push path (entry create/update, full sync, sign-in sync, a newly created budget) keeps it
     * up to date.
     */
    val budgetsWithFailedPush: StateFlow<Set<Int>> = _budgetsWithFailedPush.asStateFlow()

    fun clearFailedPushes() = _budgetsWithFailedPush.update { emptySet() }

    /**
     * Pushes all unsynced entries for the given local budget to the server.
     *
     * @param budgetId Local database identifier for the budget owning these entries.
     * @return SyncResult with statistics about succeeded/failed syncs
     */
    suspend fun syncPendingEntries(budgetId: Int): SyncResult<SyncStats> = syncMutex.withLock {
        authenticatedSync {
            // Check that the parent budget has been synced to the server first
            val budget = budgetLocalDataSource.getById(budgetId)
            val budgetServerId = budget?.serverId
            if (budgetServerId == null) {
                logger.error(
                    logTag,
                    "Cannot sync entries for budget $budgetId - parent budget not synced"
                )
                throw SyncException.ParentNotSynced("Budget $budgetId must be synced to server before syncing its entries")
            }

            // Get all unsynced entries for this budget
            val pendingEntries = localDataSource.getUnsynced(budgetId)

            if (pendingEntries.isEmpty()) {
                return@authenticatedSync SyncStats(totalItems = 0)
            }

            // Use aggregateSync to push each entry and track partial failures
            aggregateSync(
                items = pendingEntries,
                itemIdentifier = { entry -> "BudgetEntry(id=${entry.id}, desc='${entry.description}')" },
                syncOperation = { entry -> pushEntry(budgetServerId, entry) }
            )
        }.fold(
            onSuccess = { stats -> statsToResult(stats) },
            onFailure = { error -> SyncResult.Failure(error) }
        ).also { result -> trackPushResult(budgetId, result) }
    }

    private fun trackPushResult(budgetId: Int, result: SyncResult<SyncStats>) =
        _budgetsWithFailedPush.update { failed ->
            if (result is SyncResult.Success) failed - budgetId else failed + budgetId
        }

    /**
     * Pushes a single entry to the server (creates if new, updates if existing).
     */
    private suspend fun pushEntry(budgetServerId: Long, entry: BudgetEntry): Result<Unit> {
        return if (entry.serverId == null) {
            pushNewEntry(budgetServerId, entry)
        } else {
            pushUpdatedEntry(budgetServerId, entry)
        }
    }

    /**
     * Pulls entries from the server for the given budget and merges them locally.
     *
     * @param budgetServerId Server identifier of the budget whose entries will be synced.
     * @param localBudgetId Optional local budget ID to avoid additional lookups.
     * @return SyncResult with statistics about succeeded/failed merges
     */
    suspend fun pullEntriesFromServer(
        budgetServerId: Long,
        localBudgetId: Int? = null
    ): SyncResult<SyncStats> = syncMutex.withLock {
        return authenticatedSync {
            // Find the local budget ID if not provided
            val budgetId = localBudgetId ?: budgetLocalDataSource
                .getAllCached()
                .firstOrNull { it.serverId == budgetServerId }
                ?.id

            if (budgetId == null) {
                logger.error(logTag, "Local budget not found for server ID $budgetServerId")
                throw Exception("Local budget not found for server ID $budgetServerId")
            }

            // Fetch entries from server
            budgetEntryApiService.getEntries(budgetServerId).fold(
                onSuccess = { serverEntries ->
                    if (serverEntries.isEmpty()) {
                        return@authenticatedSync SyncStats(totalItems = 0)
                    }

                    // Use aggregateSync to merge each entry and track partial failures
                    aggregateSync(
                        // Oldest first, so the local auto-increment ids follow the creation order
                        items = serverEntries.sortedBy { it.id },
                        itemIdentifier = { entry -> "ServerEntry(id=${entry.id}, desc='${entry.description}')" },
                        syncOperation = { entry -> mergeServerEntry(budgetId, entry) }
                    )
                },
                onFailure = { error ->
                    logger.error(logTag, "Failed to fetch entries from server", error)
                    throw error
                }
            )
        }.fold(
            onSuccess = { stats -> statsToResult(stats) },
            onFailure = { error -> SyncResult.Failure(error) }
        )
    }

    /**
     * Performs a full sync by pushing local changes first and then pulling server updates.
     * @return SyncResult aggregating both push and pull operations
     */
    suspend fun performFullSync(budgetId: Int, budgetServerId: Long): SyncResult<SyncStats> {
        // Push local changes first
        val pushResult = syncPendingEntries(budgetId)
        if (pushResult is SyncResult.Failure) {
            logger.error(
                logTag,
                "Full sync failed during push phase for budget $budgetId",
                pushResult.error
            )
            return pushResult
        }

        // Then pull server changes
        val pullResult = pullEntriesFromServer(budgetServerId, localBudgetId = budgetId)
        if (pullResult is SyncResult.Failure) {
            logger.error(
                logTag,
                "Full sync failed during pull phase for budget $budgetId",
                pullResult.error
            )
            return pullResult
        }

        // Aggregate statistics from both operations
        val pushStats = when (pushResult) {
            is SyncResult.Success -> pushResult.data
            is SyncResult.PartialSuccess -> pushResult.data
            is SyncResult.Failure -> SyncStats(totalItems = 0)
        }

        val pullStats = when (pullResult) {
            is SyncResult.Success -> pullResult.data
            is SyncResult.PartialSuccess -> pullResult.data
            is SyncResult.Failure -> SyncStats(totalItems = 0)
        }

        val aggregatedStats = SyncStats(
            totalItems = pushStats.totalItems + pullStats.totalItems,
            syncedItems = pushStats.syncedItems + pullStats.syncedItems,
            failedItems = pushStats.failedItems + pullStats.failedItems,
            errors = pushStats.errors + pullStats.errors
        )

        return statsToResult(aggregatedStats)
    }

    /**
     * Syncs entries for all budgets that have been synced to the server.
     * Called during sign-in to ensure all budget entries are up to date.
     * @return SyncResult aggregating results from all budgets
     */
    suspend fun syncAllBudgetsEntries(): SyncResult<SyncStats> {
        return authenticatedSync {
            val budgets = budgetLocalDataSource.getAllCached()
                .filter { it.serverId != null }

            if (budgets.isEmpty()) {
                return@authenticatedSync SyncStats(totalItems = 0)
            }

            // Aggregate sync results across all budgets
            val allStats = mutableListOf<SyncStats>()
            budgets.forEach { budget ->
                budget.serverId?.let { serverId ->
                    val result = performFullSync(
                        budgetId = budget.id,
                        budgetServerId = serverId
                    )
                    when (result) {
                        is SyncResult.Success -> allStats.add(result.data)
                        is SyncResult.PartialSuccess -> allStats.add(result.data)
                        is SyncResult.Failure -> logger.warn(
                            tag = logTag,
                            message = "Failed to sync entries for budget ${budget.id}",
                            error = result.error
                        )
                    }
                }
            }

            // Combine all stats
            SyncStats(
                totalItems = allStats.sumOf { it.totalItems },
                syncedItems = allStats.sumOf { it.syncedItems },
                failedItems = allStats.sumOf { it.failedItems },
                errors = allStats.flatMap { it.errors }
            )
        }.fold(
            onSuccess = { stats -> statsToResult(stats) },
            onFailure = { error -> SyncResult.Failure(error) }
        )
    }

    private suspend fun pushNewEntry(budgetServerId: Long, entry: BudgetEntry): Result<Unit> {
        val request = entry.toCreateRequest()
        return budgetEntryApiService.createEntry(
            budgetId = budgetServerId,
            request = request
        ).fold(
            onSuccess = { response ->
                updateLocalEntryFromResponse(entry, response)
                Result.success(Unit)
            },
            onFailure = { error ->
                logger.warn(logTag, "Failed to create entry ${entry.id}", error)
                Result.failure(error)
            }
        )
    }

    private suspend fun pushUpdatedEntry(budgetServerId: Long, entry: BudgetEntry): Result<Unit> {
        val entryServerId = entry.serverId
        if (entryServerId == null) {
            return pushNewEntry(budgetServerId, entry)
        }

        return budgetEntryApiService.updateEntry(
            budgetId = budgetServerId,
            entryId = entryServerId,
            request = entry.toUpdateRequest()
        ).fold(
            onSuccess = { response ->
                updateLocalEntryFromResponse(entry, response)
                Result.success(Unit)
            },
            onFailure = { error ->
                logger.warn(logTag, "Failed to update entry ${entry.id}", error)
                Result.failure(error)
            }
        )
    }

    private suspend fun mergeServerEntry(
        localBudgetId: Int,
        serverEntry: BudgetEntryResponse
    ): Result<Unit> {
        // First, try to find existing entry by server ID
        var existingEntry = localDataSource.selectByServerId(serverEntry.id)?.toDomain()

        // If not found by server ID, check by unique fields (budgetId + amount + description + creationDate)
        // This handles the case where an entry was just pushed to the server but the serverId
        // update hasn't been committed yet (race condition during sync/SSE)
        if (existingEntry == null) {
            existingEntry = localDataSource.selectByUniqueFields(
                budgetId = localBudgetId.toLong(),
                amount = serverEntry.amount,
                description = serverEntry.description,
                creationDate = serverEntry.creationDate
            )?.toDomain()
        }

        if (existingEntry != null) {
            // An entry still waiting to be pushed keeps its own date, since the local copy is the
            // newer one. Otherwise the server wins, falling back to the local date for entries the
            // server stored before it kept a date of its own.
            val date =
                if (!existingEntry.isSynced) existingEntry.date
                else serverEntry.date ?: existingEntry.date

            // Update existing entry with server data
            val updatedEntry = existingEntry.copy(
                amount = serverEntry.amount.toPlainString(),
                description = serverEntry.description,
                category = serverEntry.category.toBudgetEntryCategory(),
                type = serverEntry.type.toBudgetEntryType(),
                date = date,
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
                date = serverEntry.date
                    ?: serverEntry.creationDate.toCalendarDate()
                    ?: today(),
                serverId = serverEntry.id,
                isSynced = true,
                createdByEmail = serverEntry.createdByEmail,
                updatedByEmail = serverEntry.updatedByEmail,
                creationDate = serverEntry.creationDate,
                modificationDate = serverEntry.modificationDate
            )
            localDataSource.create(newEntry)
        }

        return Result.success(Unit)
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
            date = response.date ?: localEntry.date,
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
