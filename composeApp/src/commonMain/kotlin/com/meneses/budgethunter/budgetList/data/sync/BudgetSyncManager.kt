package com.meneses.budgethunter.budgetList.data.sync

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.data.network.BudgetApiService
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.network.models.BudgetResponse
import com.meneses.budgethunter.commons.data.network.models.CreateBudgetRequest
import com.meneses.budgethunter.commons.data.sync.BaseSyncManager
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.data.sync.SyncResult
import com.meneses.budgethunter.commons.data.sync.SyncStats
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.datetime.Clock

/**
 * Manages synchronization of budgets between local database and backend server.
 * Handles push (local -> server) and pull (server -> local) operations.
 */
class BudgetSyncManager(
    private val localDataSource: BudgetLocalDataSource,
    private val budgetApiService: BudgetApiService,
    authRepository: AuthRepository,
    ioDispatcher: CoroutineDispatcher,
    logger: Logger
) : BaseSyncManager(authRepository, ioDispatcher, logger) {

    override val logTag = "BudgetSyncManager"

    /**
     * Pushes all unsynced local budgets to the server.
     * Updates local budgets with server IDs after successful creation.
     *
     * @return SyncResult with statistics about succeeded/failed syncs
     */
    suspend fun syncPendingBudgets(): SyncResult<SyncStats> {
        return authenticatedSync {
            // Get all unsynced budgets
            val unsyncedBudgets = localDataSource.getUnsynced()

            if (unsyncedBudgets.isEmpty()) {
                return@authenticatedSync SyncStats(totalItems = 0)
            }

            // Use aggregateSync to push each budget and track partial failures
            aggregateSync(
                items = unsyncedBudgets,
                itemIdentifier = { budget -> "Budget(id=${budget.id}, name='${budget.name}')" },
                syncOperation = { budget -> syncSingleBudget(budget) }
            )
        }.fold(
            onSuccess = { stats -> statsToResult(stats) },
            onFailure = { error -> SyncResult.Failure(error) }
        )
    }

    /**
     * Syncs a single budget to the server.
     */
    private suspend fun syncSingleBudget(budget: Budget): Result<Unit> {
        val request = CreateBudgetRequest(
            name = budget.name,
            amount = budget.amount
        )

        return budgetApiService.createBudget(request).fold(
            onSuccess = { response ->
                // Mark budget as synced with server ID
                localDataSource.markAsSynced(
                    id = budget.id,
                    serverId = response.id,
                    lastSyncedAt = Clock.System.now().toString()
                )
                Result.success(Unit)
            },
            onFailure = { error ->
                logger.warn(logTag, "Failed to sync budget ${budget.id}", error)
                Result.failure(error)
            }
        )
    }

    /**
     * Pulls budgets from the server and merges with local database.
     * Updates existing budgets or creates new ones based on server ID.
     *
     * @return SyncResult with statistics about succeeded/failed merges
     */
    suspend fun pullBudgetsFromServer(): SyncResult<SyncStats> {
        return authenticatedSync {
            // Fetch budgets from server
            budgetApiService.getBudgets().fold(
                onSuccess = { serverBudgets ->
                    if (serverBudgets.isEmpty()) {
                        return@authenticatedSync SyncStats(totalItems = 0)
                    }

                    // Use aggregateSync to merge each budget and track partial failures
                    aggregateSync(
                        items = serverBudgets,
                        itemIdentifier = { serverBudget -> "ServerBudget(id=${serverBudget.id}, name='${serverBudget.name}')" },
                        syncOperation = { serverBudget -> mergeServerBudget(serverBudget) }
                    )
                },
                onFailure = { error ->
                    logger.error(logTag, "Failed to fetch budgets from server", error)
                    throw error
                }
            )
        }.fold(
            onSuccess = { stats -> statsToResult(stats) },
            onFailure = { error -> SyncResult.Failure(error) }
        )
    }

    /**
     * Merges a single server budget into local database.
     */
    private fun mergeServerBudget(serverBudget: BudgetResponse): Result<Unit> {
        // Check if budget already exists locally by server_id
        val existingBudget = localDataSource.getByServerId(serverBudget.id)

        if (existingBudget != null) {
            // Update existing budget
            localDataSource.update(
                existingBudget.copy(
                    amount = serverBudget.amount,
                    name = serverBudget.name,
                    isSynced = true,
                    lastSyncedAt = Clock.System.now().toString()
                )
            )
        } else {
            // Create new budget from server
            localDataSource.create(
                Budget(
                    id = -1, // Auto-generated by database
                    name = serverBudget.name,
                    amount = serverBudget.amount,
                    serverId = serverBudget.id,
                    isSynced = true,
                    lastSyncedAt = Clock.System.now().toString()
                )
            )
        }

        return Result.success(Unit)
    }

    /**
     * Performs a full bidirectional sync:
     * 1. Pushes unsynced local budgets to server
     * 2. Pulls all budgets from server and merges with local
     *
     * @return SyncResult aggregating both push and pull operations
     */
    suspend fun performFullSync(): SyncResult<SyncStats> {
        // Push local changes first
        val pushResult = syncPendingBudgets()
        if (pushResult is SyncResult.Failure) {
            logger.error(logTag, "Full sync failed during push phase", pushResult.error)
            return pushResult
        }

        // Then pull server changes
        val pullResult = pullBudgetsFromServer()
        if (pullResult is SyncResult.Failure) {
            logger.error(logTag, "Full sync failed during pull phase", pullResult.error)
            return pullResult
        }

        // Aggregate statistics from both operations
        val pushStats = when (pushResult) {
            is SyncResult.Success -> pushResult.data
            is SyncResult.PartialSuccess -> pushResult.data
            is SyncResult.Failure -> SyncStats(totalItems = 0) // Already returned above
        }

        val pullStats = when (pullResult) {
            is SyncResult.Success -> pullResult.data
            is SyncResult.PartialSuccess -> pullResult.data
            is SyncResult.Failure -> SyncStats(totalItems = 0) // Already returned above
        }

        val aggregatedStats = SyncStats(
            totalItems = pushStats.totalItems + pullStats.totalItems,
            syncedItems = pushStats.syncedItems + pullStats.syncedItems,
            failedItems = pushStats.failedItems + pullStats.failedItems,
            errors = pushStats.errors + pullStats.errors
        )

        return statsToResult(aggregatedStats)
    }
}
