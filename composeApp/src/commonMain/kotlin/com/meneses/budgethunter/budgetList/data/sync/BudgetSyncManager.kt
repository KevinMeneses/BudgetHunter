package com.meneses.budgethunter.budgetList.data.sync

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.data.network.BudgetApiService
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.network.models.CreateBudgetRequest
import com.meneses.budgethunter.db.BudgetQueries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * Manages synchronization of budgets between local database and backend server.
 * Handles push (local -> server) and pull (server -> local) operations.
 */
class BudgetSyncManager(
    private val localDataSource: BudgetLocalDataSource,
    private val budgetApiService: BudgetApiService,
    private val authRepository: AuthRepository,
    private val budgetQueries: BudgetQueries,
    private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Pushes all unsynced local budgets to the server.
     * Updates local budgets with server IDs after successful creation.
     *
     * @return Result indicating success or failure with error details
     */
    suspend fun syncPendingBudgets(): Result<Unit> = withContext(ioDispatcher) {
        try {
            // Check authentication
            if (!authRepository.isAuthenticated()) {
                return@withContext Result.failure(Exception("User not authenticated"))
            }

            // Test connection by fetching existing budgets first
            println("BudgetSyncManager: Testing backend connection...")
            budgetApiService.getBudgets().fold(
                onSuccess = { serverBudgets ->
                    println("BudgetSyncManager: Successfully connected to backend, found ${serverBudgets.size} server budgets")
                },
                onFailure = { error ->
                    println("BudgetSyncManager: Failed to connect to backend: ${error.message}")
                    return@withContext Result.failure(Exception("Cannot connect to backend: ${error.message}"))
                }
            )

            // Get all unsynced budgets
            val unsyncedBudgets = budgetQueries.selectUnsynced(::mapSelectAllToBudget)
                .executeAsList()

            println("BudgetSyncManager: Found ${unsyncedBudgets.size} unsynced budgets")

            // Push each unsynced budget to server
            unsyncedBudgets.forEach { budget ->
                val request = CreateBudgetRequest(
                    name = budget.name,
                    amount = budget.amount
                )

                println("BudgetSyncManager: Syncing budget '${budget.name}' with amount ${budget.amount}")
                budgetApiService.createBudget(request).fold(
                    onSuccess = { response ->
                        // Mark budget as synced with server ID
                        budgetQueries.markAsSynced(
                            server_id = response.id,
                            last_synced_at = Clock.System.now().toString(),
                            id = budget.id.toLong()
                        )
                        println("BudgetSyncManager: Successfully synced budget ${budget.id}, server assigned ID: ${response.id}")
                    },
                    onFailure = { error ->
                        // Log error but continue with other budgets
                        println("BudgetSyncManager: Failed to sync budget ${budget.id}: ${error.message}")
                    }
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            println("BudgetSyncManager: Unexpected error during sync: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Pulls budgets from the server and merges with local database.
     * Updates existing budgets or creates new ones based on server ID.
     *
     * @return Result indicating success or failure with error details
     */
    suspend fun pullBudgetsFromServer(): Result<Unit> = withContext(ioDispatcher) {
        try {
            // Check authentication
            if (!authRepository.isAuthenticated()) {
                return@withContext Result.failure(Exception("User not authenticated"))
            }

            // Fetch budgets from server
            budgetApiService.getBudgets().fold(
                onSuccess = { serverBudgets ->
                    serverBudgets.forEach { serverBudget ->
                        // Check if budget already exists locally by server_id
                        val existingBudget = budgetQueries.selectByServerId(serverBudget.id)
                            .executeAsOneOrNull()
                            ?.let { mapSelectAllToBudget(
                                it.id,
                                it.amount,
                                it.name,
                                it.date,
                                it.server_id,
                                it.is_synced,
                                it.last_synced_at,
                                it.total_expenses
                            ) }

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
     * Performs a full bidirectional sync:
     * 1. Pushes unsynced local budgets to server
     * 2. Pulls all budgets from server and merges with local
     *
     * @return Result indicating success or failure with error details
     */
    suspend fun performFullSync(): Result<Unit> = withContext(ioDispatcher) {
        try {
            // Push local changes first
            syncPendingBudgets().getOrThrow()

            // Then pull server changes
            pullBudgetsFromServer().getOrThrow()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Helper function to map database result to Budget domain model.
     * This is needed because we're calling budgetQueries directly.
     */
    private fun mapSelectAllToBudget(
        id: Long,
        amount: Double,
        name: String,
        date: String,
        serverId: Long?,
        isSynced: Long,
        lastSyncedAt: String?,
        totalExpenses: Double
    ) = Budget(
        id = id.toInt(),
        amount = amount,
        name = name,
        totalExpenses = totalExpenses,
        date = date,
        serverId = serverId,
        isSynced = isSynced == 1L,
        lastSyncedAt = lastSyncedAt
    )
}
