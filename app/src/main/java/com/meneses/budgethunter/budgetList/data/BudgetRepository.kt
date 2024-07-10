package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.budgetDetail.data.CollaborationManager
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import com.meneses.budgethunter.commons.data.PreferencesManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BudgetRepository(
    private val localDataSource: BudgetLocalDataSource = BudgetLocalDataSource(),
    private val preferencesManager: PreferencesManager = PreferencesManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val collaborationManager: CollaborationManager = CollaborationManager()
) {
    val budgets: Flow<List<Budget>>
        get() = localDataSource.budgets

    fun getAll(): List<Budget> =
        localDataSource.getAll()

    fun getAllFilteredBy(filter: BudgetFilter) =
        localDataSource.getAllFilteredBy(filter)

    suspend fun create(budget: Budget) = withContext(ioDispatcher) {
        localDataSource.create(budget)
    }

    suspend fun update(budget: Budget) = withContext(ioDispatcher) {
        localDataSource.update(budget)
        if (preferencesManager.isCollaborationEnabled) {
            sendUpdate(budget)
        }
    }

    suspend fun delete(budget: Budget) = withContext(ioDispatcher) {
        localDataSource.delete(budget.id.toLong())
        if (preferencesManager.isCollaborationEnabled) {
            collaborationManager.stopCollaboration()
        }
    }

    private suspend fun sendUpdate(budget: Budget) {
        val jsonBudget = Json.encodeToString(budget)
        collaborationManager.sendUpdate("budget#$jsonBudget")
    }
}
