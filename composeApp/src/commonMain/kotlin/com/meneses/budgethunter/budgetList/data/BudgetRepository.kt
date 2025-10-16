package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
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
}