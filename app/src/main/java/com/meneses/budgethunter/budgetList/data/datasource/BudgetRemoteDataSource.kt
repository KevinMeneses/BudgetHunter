package com.meneses.budgethunter.budgetList.data.datasource

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.KtorRealtimeMessagingClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BudgetRemoteDataSource(
    private val messagingClient: () -> KtorRealtimeMessagingClient = {
        KtorRealtimeMessagingClient.getInstance()
    }
) {
    fun getBudgetStream(): Flow<Budget> =
        messagingClient().collaborationStream
            .filter { it.contains("budget#") }
            .map { Json.decodeFromString(it) }

    suspend fun sendUpdate(budget: Budget) {
        val jsonBudget = Json.encodeToString(budget)
        messagingClient().sendUpdate("budget#$jsonBudget")
    }

    suspend fun closeStream() =
        messagingClient().close()
}
