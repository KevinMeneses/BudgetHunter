package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryRemoteDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.commons.data.PreferencesManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withContext

class BudgetEntryRepository(
    private val localDataSource: BudgetEntryLocalDataSource = BudgetEntryLocalDataSource(),
    private val remoteDataSource: BudgetEntryRemoteDataSource = BudgetEntryRemoteDataSource(),
    private val preferencesManager: PreferencesManager = PreferencesManager(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    fun getAll() = localDataSource.getAll()

    fun getAllByBudgetId(budgetId: Int) = localDataSource
        .selectAllByBudgetId(budgetId.toLong())

    fun getAllFilteredBy(filter: BudgetEntryFilter) =
        localDataSource.getAllFilteredBy(filter)

    suspend fun create(budgetEntry: BudgetEntry) = withContext(ioDispatcher) {
        localDataSource.create(budgetEntry)
        if (preferencesManager.isCollaborationEnabled) {
            remoteDataSource.sendUpdate(getAll())
        }
    }

    suspend fun update(budgetEntry: BudgetEntry) = withContext(ioDispatcher) {
        localDataSource.update(budgetEntry)
        if (preferencesManager.isCollaborationEnabled) {
            remoteDataSource.sendUpdate(getAll())
        }
    }

    suspend fun deleteByIds(ids: List<Int>) = withContext(ioDispatcher) {
        val dbIds = ids.map { it.toLong() }
        localDataSource.deleteByIds(dbIds)
        if (preferencesManager.isCollaborationEnabled) {
            remoteDataSource.sendUpdate(getAll())
        }
    }

    suspend fun consumeCollaborationStream() = withContext(ioDispatcher) {
        remoteDataSource.getEntriesStream()
            .takeWhile { preferencesManager.isCollaborationEnabled }
            .collect { remoteEntries ->
                for (remoteEntry in remoteEntries) {
                    val cachedList = getAll()
                    val index = cachedList.indexOfFirst { it.id == remoteEntry.id }
                    when {
                        index == -1 -> localDataSource.create(remoteEntry)
                        cachedList[index] != remoteEntry -> localDataSource.update(remoteEntry)
                    }
                }
            }
    }
}
