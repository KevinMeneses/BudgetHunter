package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.data.network.BudgetApiService
import com.meneses.budgethunter.budgetList.data.sync.BudgetSyncManager
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope

class BudgetRepository(
    private val localDataSource: BudgetLocalDataSource,
    private val budgetSyncManager: BudgetSyncManager,
    private val budgetApiService: BudgetApiService,
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher,
    private val scope: CoroutineScope
) {
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
            scope.launch(ioDispatcher) {
                budgetSyncManager.syncPendingBudgets()
            }
        }

        createdBudget
    }

    suspend fun update(budget: Budget) = withContext(ioDispatcher) {
        localDataSource.update(budget)

        // Trigger sync in background if authenticated
        if (authRepository.isAuthenticated()) {
            scope.launch(ioDispatcher) {
                budgetSyncManager.syncPendingBudgets()
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
        budgetSyncManager.performFullSync()
    }

    /**
     * Clear all budgets from local database.
     * Used when signing out to prevent data leaking between users.
     */
    suspend fun clearAllData() = withContext(ioDispatcher) {
        localDataSource.clearAllData()
    }

    /**
     * Delete a budget.
     * If the budget is synced to the server, it will be deleted from the server first,
     * then deleted locally. If the server deletion fails, local deletion proceeds anyway
     * and the inconsistency will be cleaned up on next sync.
     *
     * @param budgetId Local budget ID to delete
     */
    suspend fun delete(budgetId: Int) = withContext(ioDispatcher) {
        val budget = localDataSource.getById(budgetId)

        // If synced, delete from server first
        if (budget != null && authRepository.isAuthenticated() && budget.serverId != null) {
            try {
                budgetApiService.deleteBudget(budget.serverId).getOrThrow()
                println("BudgetRepository: Successfully deleted budget from server (ID: ${budget.serverId})")
            } catch (e: Exception) {
                println("BudgetRepository: Failed to delete budget from server, proceeding with local deletion - ${e.message}")
                // Continue with local deletion even if server deletion fails
            }
        }

        // Then delete locally
        localDataSource.delete(budgetId.toLong())
        println("BudgetRepository: Successfully deleted budget locally (ID: $budgetId)")
    }
}