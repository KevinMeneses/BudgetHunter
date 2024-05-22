package com.meneses.budgethunter.budgetEntry.data.repository

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryRemoteDataSource
import com.meneses.budgethunter.budgetEntry.data.toDb
import com.meneses.budgethunter.budgetEntry.data.toDomain
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.commons.data.PreferencesManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withContext

class BudgetEntryRepositoryImpl(
    private val localDataSource: BudgetEntryLocalDataSource = BudgetEntryLocalDataSource(),
    private val remoteDataSource: BudgetEntryRemoteDataSource = BudgetEntryRemoteDataSource(),
    private val preferencesManager: PreferencesManager = PreferencesManager(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BudgetEntryRepository {

    override fun getAll() = cachedEntries

    override fun getAllByBudgetId(budgetId: Int) = localDataSource
        .selectAllByBudgetId(budgetId.toLong())
        .map { it.toDomain() }
        .onEach {
            cachedEntries = it
            if (preferencesManager.isCollaborationEnabled) {
                remoteDataSource.sendUpdate(it)
            }
        }

    override fun getAllFilteredBy(filter: BudgetEntryFilter) =
        cachedEntries
            .filter {
                if (filter.description.isNullOrBlank()) true
                else it.description.lowercase()
                    .contains(filter.description.lowercase())
            }.filter {
                if (filter.type == null) true
                else it.type == filter.type
            }.filter {
                if (filter.startDate == null) true
                else it.date >= filter.startDate
            }.filter {
                if (filter.endDate == null) true
                else it.date <= filter.endDate
            }

    override suspend fun create(budgetEntry: BudgetEntry) = withContext(ioDispatcher) {
        localDataSource.insert(budgetEntry.toDb())
    }

    override suspend fun update(budgetEntry: BudgetEntry) = withContext(ioDispatcher) {
        localDataSource.update(budgetEntry.toDb())
    }

    override suspend fun deleteByIds(ids: List<Int>) = withContext(ioDispatcher) {
        val dbIds = ids.map { it.toLong() }
        localDataSource.deleteByIds(dbIds)
    }

    override suspend fun collaborate() = withContext(ioDispatcher) {
        remoteDataSource.getEntriesStream()
            .takeWhile { preferencesManager.isCollaborationEnabled }
            .collect { remoteEntries ->
                for (remoteEntry in remoteEntries) {
                    val index = cachedEntries.indexOfFirst { it.id == remoteEntry.id }
                    if (index == -1) create(remoteEntry)
                    else update(remoteEntry)
                }
            }
    }

    companion object {
        private var cachedEntries = emptyList<BudgetEntry>()
    }
}
