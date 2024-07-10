package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetDetail.data.CollaborationManager
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.commons.data.PreferencesManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BudgetEntryRepository(
    private val localDataSource: BudgetEntryLocalDataSource = BudgetEntryLocalDataSource(),
    private val preferencesManager: PreferencesManager = PreferencesManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val collaborationManager: CollaborationManager = CollaborationManager()
) {
    fun getAll() = localDataSource.getAll()

    fun getAllByBudgetId(budgetId: Int) = localDataSource
        .selectAllByBudgetId(budgetId.toLong())
        .onEach {
            if (it != getAll()) {
                localDataSource.updateCache(it)
                if (preferencesManager.isCollaborationEnabled) {
                    sendUpdate(it)
                }
            }
        }

    fun getAllFilteredBy(filter: BudgetEntryFilter) =
        localDataSource.getAllFilteredBy(filter)

    suspend fun create(budgetEntry: BudgetEntry) = withContext(ioDispatcher) {
        localDataSource.create(budgetEntry)
    }

    suspend fun update(budgetEntry: BudgetEntry) = withContext(ioDispatcher) {
        localDataSource.update(budgetEntry)
    }

    suspend fun deleteByIds(ids: List<Int>) = withContext(ioDispatcher) {
        val dbIds = ids.map { it.toLong() }
        localDataSource.deleteByIds(dbIds)
    }

    private suspend fun sendUpdate(budgetEntries: List<BudgetEntry>) {
        val jsonEntries = Json.encodeToString(budgetEntries)
        collaborationManager.sendUpdate("budget_entries#$jsonEntries")
    }
}
