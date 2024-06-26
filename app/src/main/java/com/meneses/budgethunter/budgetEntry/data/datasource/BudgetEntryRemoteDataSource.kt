package com.meneses.budgethunter.budgetEntry.data.datasource

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.data.KtorRealtimeMessagingClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BudgetEntryRemoteDataSource(
    private val messagingClient: () -> KtorRealtimeMessagingClient = {
        KtorRealtimeMessagingClient.getInstance()
    }
) {
    suspend fun getEntriesStream(): Flow<List<BudgetEntry>> =
        messagingClient().getCollaborationStream()
            .filter { it.contains("budget_entries#") }
            .map {
                val entriesJson = it.split("#").last()
                Json.decodeFromString(entriesJson)
            }

    suspend fun sendUpdate(budgetEntries: List<BudgetEntry>) {
        val jsonEntries = Json.encodeToString(budgetEntries)
        messagingClient().sendUpdate("budget_entries#$jsonEntries")
    }
}
