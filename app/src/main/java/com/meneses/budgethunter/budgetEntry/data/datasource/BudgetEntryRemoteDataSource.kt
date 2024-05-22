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
    fun getBudgetStream(): Flow<List<BudgetEntry>> =
        messagingClient().collaborationStream
            .filter { it.contains("budget_entries#") }
            .map { Json.decodeFromString(it) }

    suspend fun sendUpdate(budgetEntries: List<BudgetEntry>) {
        val jsonEntries = Json.encodeToString(budgetEntries)
        messagingClient().sendUpdate("budget_entries#$jsonEntries")
    }

    suspend fun closeStream() =
        messagingClient().close()
}
