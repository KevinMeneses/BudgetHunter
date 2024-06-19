package com.meneses.budgethunter.budgetList.data.datasource

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.KtorRealtimeMessagingClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BudgetRemoteDataSource(
    private val messagingClient: () -> KtorRealtimeMessagingClient = {
        KtorRealtimeMessagingClient.getInstance()
    },
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(Logging)
    }
) {
    suspend fun startCollaboration(): Int {
        val response = httpClient.get("http://192.168.1.12:8080/collaborate/start")
        val code = response.body<Int>()
        return code
    }

    suspend fun joinCollaboration(collaborationCode: Int) =
        messagingClient().joinCollaboration(collaborationCode)

    suspend fun getBudgetStream(): Flow<Budget> =
        messagingClient().getCollaborationStream()
            .filter { it.contains("budget#") }
            .map {
                val budgetJson = it.split("#").last()
                Json.decodeFromString(budgetJson)
            }

    suspend fun sendUpdate(budget: Budget) {
        val jsonBudget = Json.encodeToString(budget)
        messagingClient().sendUpdate("budget#$jsonBudget")
    }

    suspend fun closeStream() =
        messagingClient().close()
}
