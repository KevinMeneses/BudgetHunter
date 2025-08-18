package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.MyApplication
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import com.meneses.budgethunter.commons.data.KtorRealtimeMessagingClient
import com.meneses.budgethunter.commons.data.PreferencesManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BudgetRepository(
    private val localDataSource: BudgetLocalDataSource = BudgetLocalDataSource(),
    private val preferencesManager: PreferencesManager = MyApplication.preferencesManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val messagingClient: () -> KtorRealtimeMessagingClient = {
        KtorRealtimeMessagingClient.getInstance()
    }
) {
    val budgets: Flow<List<Budget>>
        get() = localDataSource.budgets

    fun getById(id: Int): Budget? =
        localDataSource.getById(id)

    fun getAllCached(): List<Budget> =
        localDataSource.getAllCached()

    fun getAllFilteredBy(filter: BudgetFilter) =
        localDataSource.getAllFilteredBy(filter)

    suspend fun create(budget: Budget) = withContext(ioDispatcher) {
        localDataSource.create(budget)
    }

    suspend fun update(budget: Budget) = withContext(ioDispatcher) {
        localDataSource.update(budget)
    }

    suspend fun joinCollaboration(collaborationCode: Int) = withContext(ioDispatcher) {
        messagingClient().joinCollaboration(collaborationCode)
        preferencesManager.isCollaborationEnabled = true
    }
}
