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
            println("SseClient: Attempting to connect to SSE stream for budget $budgetServerId")

            val url = "$baseUrl/api/budgets/$budgetServerId/entries/stream"
            println("SseClient: Connecting to: $url")

            // Use Ktor's SSE plugin to establish a streaming connection
            httpClient.serverSentEvents(urlString = url) {
                println("SseClient: Successfully connected to SSE stream")

                // Process incoming SSE events as they arrive
                incoming.collect { event ->
                    try {
                        // SSE events have a "data" field containing the JSON payload
                        val eventData = event.data
                        if (eventData != null) {
                            println("SseClient: Received event data: $eventData")

                            // Parse the JSON data into a BudgetEntryEvent notification
                            val budgetEntryEvent = json.decodeFromString<BudgetEntryEvent>(eventData)
                            println("SseClient: Successfully parsed ${budgetEntryEvent.action} event for entry ${budgetEntryEvent.entryId}")

                            // Emit the notification to the flow
                            emit(budgetEntryEvent)
                        }
                    } catch (e: Exception) {
                        println("SseClient: Error parsing event data: ${e.message}")
                        e.printStackTrace()
                        // Continue processing other events despite parsing errors
                    }
                }
            }

            println("SseClient: SSE stream closed")
        } catch (e: Exception) {
            println("SseClient: Error in SSE stream: ${e.message}")
            println("SseClient: Error type: ${e::class.simpleName}")
            e.printStackTrace()
            throw e
        }
    }.catch { exception ->
        println("SseClient: Caught exception in flow: ${exception.message}")
        exception.printStackTrace()
        throw exception
    }
}
