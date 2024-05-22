package com.meneses.budgethunter.commons.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull

class KtorRealtimeMessagingClient private constructor(
    private val client: HttpClient = HttpClient(CIO) {
        install(Logging)
        install(WebSockets)
    }
) {
    private var session: WebSocketSession? = null

    val collaborationStream: Flow<String> = flow {
        session = client.webSocketSession {
            url("ws://192.168.1.12/collaborate")
        }
        val serverUpdates = session!!
            .incoming
            .consumeAsFlow()
            .filterIsInstance<Frame.Text>()
            .mapNotNull { it.readText() }

        emitAll(serverUpdates)
    }

    suspend fun sendUpdate(update: String) {
        session?.outgoing?.send(Frame.Text(update))
    }

    suspend fun close() {
        session?.close()
        session = null
    }

    companion object {
        private var instance: KtorRealtimeMessagingClient? = null

        fun getInstance(): KtorRealtimeMessagingClient {
            if (instance == null) {
                instance = KtorRealtimeMessagingClient()
            }

            return instance!!
        }
    }
}
