package com.meneses.budgethunter.commons.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow

class KtorRealtimeMessagingClient private constructor(
    private val client: HttpClient = HttpClient(CIO) {
        install(Logging)
        install(WebSockets)
    }
) {
    private var session: WebSocketSession? = null

    fun getCollaborationStream() = session!!.incoming
        .receiveAsFlow()
        .filterIsInstance<Frame.Text>()
        .map { it.readText() }

    suspend fun startCollaboration(): Int {
        val response = client.get("http://192.168.1.12:8080/collaborate/start")
        val code = response.body<Int>()
        joinCollaboration(code)
        return code
    }

    suspend fun joinCollaboration(collaborationCode: Int) {
        session = client.webSocketSession {
            url("ws://192.168.1.12:8080/collaborate?code=$collaborationCode")
        }
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
