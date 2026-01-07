package com.meneses.budgethunter.commons.data.network.services

import com.meneses.budgethunter.commons.data.network.models.BudgetEntryEvent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.serverSentEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

/**
 * SSE Client for receiving real-time budget entry updates from the server.
 *
 * Implements Server-Sent Events protocol by parsing text/event-stream responses.
 * Establishes persistent connections to receive real-time budget entry updates from collaborators.
 *
 * @property httpClient Ktor HTTP client configured with auth and content negotiation
 * @property baseUrl Base URL of the backend API
 * @property json JSON serializer for parsing events
 */
class SseClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val json: Json
) {
    /**
     * Subscribe to real-time budget entry events for a specific budget.
     *
     * Establishes an SSE (Server-Sent Events) connection that receives real-time updates.
     * Events are parsed from text/event-stream format and emitted to the flow.
     *
     * @param budgetServerId Server-side ID of the budget to listen for updates
     * @return Flow of BudgetEntryEvent objects as they arrive from the server
     */
    fun subscribeToBudgetEntries(budgetServerId: Long): Flow<BudgetEntryEvent> = flow {
        try {
            val url = "$baseUrl/api/budgets/$budgetServerId/entries/stream"
            httpClient.serverSentEvents(urlString = url) {
                incoming.collect { event ->
                    try {
                        val eventData = event.data
                        if (eventData != null) {
                            val budgetEntryEvent = json.decodeFromString<BudgetEntryEvent>(eventData)
                            emit(budgetEntryEvent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }.catch { exception ->
        throw exception
    }
}
