package com.meneses.budgethunter.budgetDetail.data

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.KtorRealtimeMessagingClient
import com.meneses.budgethunter.commons.data.PreferencesManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class CollaborationManager(
    private val messagingClient: () -> KtorRealtimeMessagingClient = {
        KtorRealtimeMessagingClient.getInstance()
    },
    private val preferencesManager: PreferencesManager = PreferencesManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val budgetLocalDataSource: BudgetLocalDataSource = BudgetLocalDataSource(),
    private val budgetEntryLocalDataSource: BudgetEntryLocalDataSource = BudgetEntryLocalDataSource()
) {

    suspend fun startCollaboration(): Int = withContext(ioDispatcher) {
        if (preferencesManager.isCollaborationEnabled) {
            throw CollaborationException("Collaboration already started")
        }
        val collaborationCode = messagingClient().startCollaboration()
        preferencesManager.isCollaborationEnabled = true
        return@withContext collaborationCode
    }

    suspend fun joinCollaboration(collaborationCode: Int) = withContext(ioDispatcher) {
        messagingClient().joinCollaboration(collaborationCode)
        preferencesManager.isCollaborationEnabled = true
    }

    suspend fun stopCollaboration() {
        messagingClient().close()
        preferencesManager.isCollaborationEnabled = false
    }

    suspend fun sendUpdate(update: String) {
        messagingClient().sendUpdate(update)
    }

    fun consumeCollaborationStream() = CoroutineScope(ioDispatcher).launch {
        messagingClient().getCollaborationStream().collect {
            when {
                it.contains("budget#") -> {
                    val budgetJson = it.split("#").last()
                    val budget = Json.decodeFromString<Budget>(budgetJson)
                    val cachedList = budgetLocalDataSource.getAll()
                    val index = cachedList.indexOfFirst { cached -> cached.id == budget.id }
                    if (index != -1 && cachedList[index] != budget) {
                        budgetLocalDataSource.update(budget)
                    }
                }

                it.contains("budget_entries#") -> {
                    val entriesJson = it.split("#").last()
                    val remoteEntries = Json.decodeFromString<List<BudgetEntry>>(entriesJson)
                    for (remoteEntry in remoteEntries) {
                        val cachedList = budgetEntryLocalDataSource.getAll()
                        val index = cachedList.indexOfFirst { cached ->
                            cached.id == remoteEntry.id
                        }

                        when {
                            index == -1 -> {
                                val updatedEntries = cachedList.toMutableList()
                                updatedEntries.add(remoteEntry)
                                budgetEntryLocalDataSource.updateCache(updatedEntries)
                                budgetEntryLocalDataSource.create(remoteEntry)
                            }

                            cachedList[index] != remoteEntry -> {
                                val updatedEntries = cachedList.toMutableList()
                                updatedEntries[index] = remoteEntry
                                budgetEntryLocalDataSource.updateCache(updatedEntries)
                                budgetEntryLocalDataSource.update(remoteEntry)
                            }
                        }
                    }
                }
            }
        }
    }
}
