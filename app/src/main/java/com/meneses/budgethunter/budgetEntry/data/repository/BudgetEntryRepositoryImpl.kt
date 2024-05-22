package com.meneses.budgethunter.budgetEntry.data.repository

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryRemoteDataSource
import com.meneses.budgethunter.budgetEntry.data.toDb
import com.meneses.budgethunter.budgetEntry.data.toDomain
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.commons.data.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

class BudgetEntryRepositoryImpl(
    private val localDataSource: BudgetEntryLocalDataSource = BudgetEntryLocalDataSource(),
    private val remoteDataSource: BudgetEntryRemoteDataSource = BudgetEntryRemoteDataSource(),
    private val preferencesManager: PreferencesManager
) : BudgetEntryRepository {

    override fun getAll() = cachedEntries

    override fun getAllByBudgetId(budgetId: Int): Flow<List<BudgetEntry>> =
        if (preferencesManager.isCollaborationEnabled) {
            remoteDataSource.getBudgetStream()
                .onEach { entries ->
                    entries.forEachIndexed { index, remoteEntry ->
                        val localEntry = cachedEntries.getOrNull(index)
                        if (localEntry == null) create(remoteEntry)
                        if (localEntry != remoteEntry) update(remoteEntry)
                    }
                }.onCompletion {
                    remoteDataSource.closeStream()
                }
        } else {
            localDataSource
                .selectAllByBudgetId(budgetId.toLong())
                .map { it.toDomain() }
        }.onEach {
            cachedEntries = it
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

    override suspend fun create(budgetEntry: BudgetEntry) {
        if (preferencesManager.isCollaborationEnabled){
            val updatedEntries = cachedEntries.toMutableList()
            updatedEntries.add(budgetEntry)
            remoteDataSource.sendUpdate(updatedEntries)
        } else {
            localDataSource.insert(budgetEntry.toDb())
        }
    }

    override suspend fun update(budgetEntry: BudgetEntry) {
        if (preferencesManager.isCollaborationEnabled) {
            val index = cachedEntries.indexOf(budgetEntry)
            if (index != -1) {
                val updatedEntries = cachedEntries.toMutableList()
                updatedEntries[index] = budgetEntry
                remoteDataSource.sendUpdate(updatedEntries)
            }
        } else {
            localDataSource.update(budgetEntry.toDb())
        }
    }

    override suspend fun deleteByIds(ids: List<Int>) {
        if (preferencesManager.isCollaborationEnabled) {
            val updatedEntries = cachedEntries.toMutableList()
            val entriesToDelete = updatedEntries.filter { ids.contains(it.id) }
            updatedEntries.removeAll(entriesToDelete)
            remoteDataSource.sendUpdate(updatedEntries)
        } else {
            val dbIds = ids.map { it.toLong() }
            localDataSource.deleteByIds(dbIds)
        }
    }

    companion object {
        private var cachedEntries = emptyList<BudgetEntry>()
    }
}
