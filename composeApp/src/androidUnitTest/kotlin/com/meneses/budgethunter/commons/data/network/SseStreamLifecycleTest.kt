package com.meneses.budgethunter.commons.data.network

import com.meneses.budgethunter.commons.data.network.services.SseClient
import com.meneses.budgethunter.commons.data.sync.Logger
import com.sun.net.httpserver.HttpServer
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlin.test.assertNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.serverSentEvents
import io.ktor.client.plugins.timeout
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Lifecycle of the SSE streams against a local server, with the engine the Android platform
 * module gives the API client.
 *
 * - Cancelling must close the connection: the `Android` (HttpURLConnection) engine never closes
 *   the socket when a request is cancelled, so every stream left behind (switching budgets,
 *   leaving a budget) stayed open, on the phone and on the server, until the network dropped.
 * - A stream that stops receiving anything must fail so it gets reconnected.
 */
class SseStreamLifecycleTest {

    private val logger = mockk<Logger>(relaxed = true)

    @Test
    fun `cancelling an SSE stream closes its connection`() = runBlocking<Unit> {
        assertClosesOnCancel(OkHttp)
    }

    /**
     * Regression test: when the phone switched networks the socket on the old one never failed,
     * and with no read timeout the stream hung forever, receiving nothing and never reconnecting.
     *
     * Ktor sometimes ends the flow with an error and sometimes normally; RealTimeSyncManager
     * reconnects in both cases, so this only checks that the stream ends.
     */
    @Test
    fun `a stream that stops receiving keep-alives ends`() = runBlocking<Unit> {
        val server = startServer(keepAlives = 1)
        val client = createTestClient()
        try {
            val sseClient = SseClient(client, server.url, Json, logger, streamIdleTimeoutMillis = 500)

            val ended = withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
                runCatching { sseClient.subscribeToBudgetEntries(BUDGET_ID, onConnected = {}).collect { } }
            }

            assertNotNull(ended, "The silent stream was still open after $CONNECT_TIMEOUT_MS ms")
        } finally {
            client.close()
            server.stop()
        }
    }

    @Test
    fun `keep-alives keep a stream open past the idle timeout`() = runBlocking<Unit> {
        val server = startServer(keepAlives = Int.MAX_VALUE)
        val client = createTestClient()
        try {
            val sseClient = SseClient(client, server.url, Json, logger, streamIdleTimeoutMillis = 500)

            val result = withTimeoutOrNull(2_000) {
                sseClient.subscribeToBudgetEntries(BUDGET_ID, onConnected = {}).collect { }
            }

            assertNull(result, "The stream should still be open when the test gives up on it")
        } finally {
            client.close()
            server.stop()
        }
    }

    private class TestServer(val url: String, val stop: () -> Unit)

    /** SSE server that writes [keepAlives] keep-alive comments and then stays silent. */
    private fun startServer(keepAlives: Int): TestServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            executor = Executors.newCachedThreadPool()
            createContext(ApiEndpoints.budgetEntriesStream(BUDGET_ID)) { exchange ->
                exchange.responseHeaders.add("Content-Type", "text/event-stream")
                exchange.sendResponseHeaders(200, 0)
                runCatching {
                    repeat(keepAlives) {
                        exchange.responseBody.apply {
                            write(": keep-alive\n\n".toByteArray())
                            flush()
                        }
                        Thread.sleep(KEEP_ALIVE_INTERVAL_MS)
                    }
                    Thread.sleep(Long.MAX_VALUE)
                }
            }
            start()
        }
        return TestServer("http://127.0.0.1:${server.address.port}") { server.stop(0) }
    }

    private fun createTestClient() = HttpClient(OkHttp) {
        install(SSE)
        install(HttpTimeout)
    }

    private suspend fun assertClosesOnCancel(engine: HttpClientEngineFactory<*>) {
        val serverSawClose = CompletableDeferred<Unit>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            executor = Executors.newCachedThreadPool()
            createContext("/stream") { exchange ->
                exchange.responseHeaders.add("Content-Type", "text/event-stream")
                exchange.sendResponseHeaders(200, 0)
                try {
                    while (true) {
                        exchange.responseBody.apply {
                            write(": keep-alive\n\n".toByteArray())
                            flush()
                        }
                        Thread.sleep(KEEP_ALIVE_INTERVAL_MS)
                    }
                } catch (_: Exception) {
                    serverSawClose.complete(Unit)
                }
            }
            start()
        }
        val client = HttpClient(engine) {
            install(SSE)
            install(HttpTimeout)
        }

        try {
            val connected = CompletableDeferred<Unit>()
            val stream = CoroutineScope(Dispatchers.IO).launch {
                client.serverSentEvents(
                    urlString = "http://127.0.0.1:${server.address.port}/stream",
                    request = {
                        timeout {
                            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                            socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                        }
                    }
                ) {
                    connected.complete(Unit)
                    incoming.collect { }
                }
            }
            withTimeout(CONNECT_TIMEOUT_MS) { connected.await() }

            stream.cancel()

            assertNotNull(
                withTimeoutOrNull(CLOSE_TIMEOUT_MS) { serverSawClose.await() },
                "The server never saw the connection close after cancelling the stream"
            )
        } finally {
            client.close()
            server.stop(0)
        }
    }

    private companion object {
        const val KEEP_ALIVE_INTERVAL_MS = 50L
        const val CONNECT_TIMEOUT_MS = 5_000L
        const val CLOSE_TIMEOUT_MS = 3_000L
        const val BUDGET_ID = 23L
    }
}
