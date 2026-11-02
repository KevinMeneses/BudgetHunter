package com.meneses.budgethunter.commons.data.network.services

import com.meneses.budgethunter.commons.data.network.ApiEndpoints
import com.meneses.budgethunter.commons.data.network.models.BudgetEntryEvent
import com.meneses.budgethunter.commons.data.sync.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.contentnegotiation.exclude
import io.ktor.client.plugins.sse.serverSentEvents
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.Flow
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
 * @property logger Logger for SSE connection events and errors
 * @property streamIdleTimeoutMillis Time without receiving anything, not even a keep-alive,
 *   after which a stream is considered dead and fails (see [configureStreamRequest])
 */
class SseClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val json: Json,
    private val logger: Logger,
    private val streamIdleTimeoutMillis: Long = STREAM_IDLE_TIMEOUT_MS
) {
    private val tag = "SseClient"

    /**
     * Subscribe to real-time budget entry events for a specific budget.
     *
     * Establishes an SSE (Server-Sent Events) connection that receives real-time updates.
     * Events are parsed from text/event-stream format and emitted to the flow.
     *
     * The connection is tied to the collecting coroutine's lifetime — cancel the Job
     * returned by [kotlinx.coroutines.flow.launchIn] to terminate the SSE connection.
     *
     * The flow fails when the connection drops; reconnection is the caller's responsibility
     * (see [com.meneses.budgethunter.budgetEntry.data.sync.RealTimeSyncManager]).
     *
     * @param budgetServerId Server-side ID of the budget to listen for updates
     * @param onConnected Called in the collecting coroutine once the server accepted the stream,
     *   before any event; lets the caller tell a working connection from a failed attempt
     * @return Flow of BudgetEntryEvent objects as they arrive from the server
     */
    fun subscribeToBudgetEntries(
        budgetServerId: Long,
        onConnected: () -> Unit
    ): Flow<BudgetEntryEvent> = flow {
        val url = "$baseUrl${ApiEndpoints.budgetEntriesStream(budgetServerId)}"
        httpClient.serverSentEvents(urlString = url, request = { configureStreamRequest() }) {
            onConnected()
            incoming.collect { event ->
                try {
                    val eventData = event.data
                    if (eventData != null) {
                        val budgetEntryEvent = json.decodeFromString<BudgetEntryEvent>(eventData)
                        emit(budgetEntryEvent)
                    }
                } catch (e: Exception) {
                    logger.warn(tag, "Failed to parse SSE event data", e)
                }
            }
        }
    }

    /**
     * Applies the request tweaks a long-lived stream needs.
     *
     * - No request timeout: the stream is meant to stay open indefinitely.
     * - The socket timeout is the time without receiving anything after which the stream is
     *   considered dead. The server sends a keep-alive comment every 15 s, so silence for
     *   [streamIdleTimeoutMillis] means the connection is gone even if no error surfaced: e.g.
     *   when the phone switches networks, the socket on the old one never fails by itself and
     *   the stream would hang forever without receiving events or reconnecting.
     * - `application/json` is excluded from content negotiation so the request carries only the
     *   SSE plugin's `Accept: text/event-stream` instead of two conflicting Accept headers.
     */
    private fun HttpRequestBuilder.configureStreamRequest() {
        timeout {
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            socketTimeoutMillis = streamIdleTimeoutMillis
        }
        exclude(ContentType.Application.Json)
    }

    private companion object {
        /** Three of the server's 15 s keep-alives; must stay well above that interval. */
        const val STREAM_IDLE_TIMEOUT_MS = 45_000L
    }
}
