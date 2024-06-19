package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.data.datasource.BudgetRemoteDataSource
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import com.meneses.budgethunter.commons.data.PreferencesManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withContext

class BudgetRepository(
    private val localDataSource: BudgetLocalDataSource = BudgetLocalDataSource(),
    private val remoteDataSource: BudgetRemoteDataSource = BudgetRemoteDataSource(),
    private val preferencesManager: PreferencesManager = PreferencesManager(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
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
            remoteDataSource.sendUpdate(budget)
        }
    }

    suspend fun delete(budget: Budget) = withContext(ioDispatcher) {
        localDataSource.delete(budget.id.toLong())
        if (preferencesManager.isCollaborationEnabled) {
            remoteDataSource.closeStream()
        }
    }

    suspend fun startCollaboration(): Int = withContext(ioDispatcher) {
        preferencesManager.collaborationCode = remoteDataSource.startCollaboration()
        return@withContext preferencesManager.collaborationCode
    }

    suspend fun consumeCollaborationStream() = withContext(ioDispatcher) {
        remoteDataSource.getBudgetStream()
            .takeWhile { preferencesManager.isCollaborationEnabled }
            .collect { budget ->
                val cachedList = getAll()
                val index = cachedList.indexOfFirst { it.id == budget.id }
                if (index != -1 && cachedList[index] != budget) {
                    localDataSource.update(budget)
                }
            }
    }

    suspend fun joinCollaboration(collaborationCode: Int): Boolean = withContext(ioDispatcher) {
        preferencesManager.isCollaborationEnabled = remoteDataSource.joinCollaboration(collaborationCode)
        return@withContext preferencesManager.isCollaborationEnabled
    }

    suspend fun stopCollaboration() {
        remoteDataSource.closeStream()
        preferencesManager.isCollaborationEnabled = false
    }
}
