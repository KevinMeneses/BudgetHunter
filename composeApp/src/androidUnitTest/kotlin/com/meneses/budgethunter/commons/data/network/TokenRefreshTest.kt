package com.meneses.budgethunter.commons.data.network

import com.meneses.budgethunter.auth.data.TokenStorage
import com.meneses.budgethunter.commons.data.sync.Logger
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException

/**
 * What a failed token refresh does to the stored session. Dropping it on any failure used to
 * sign the user out silently on a network hiccup; now only a refusal from the server does, and
 * then the app is told so it can send the user to sign in.
 */
class TokenRefreshTest {

    private val tokenStorage = InMemoryTokenStorage(auth = "old-auth", refresh = "old-refresh")
    private var sessionExpiredCalls = 0

    @Test
    fun `a refused refresh token clears the session and reports it expired`() = runTest {
        val client = clientAnswering { respondError(HttpStatusCode.Unauthorized) }

        client.get(ApiEndpoints.BUDGETS)

        assertNull(tokenStorage.auth)
        assertNull(tokenStorage.refresh)
        assertEquals(1, sessionExpiredCalls)
    }

    @Test
    fun `a bad request or forbidden while refreshing keeps the session`() = runTest {
        for (status in listOf(HttpStatusCode.BadRequest, HttpStatusCode.Forbidden)) {
            val client = clientAnswering { respondError(status) }

            client.get(ApiEndpoints.BUDGETS)

            assertEquals("old-refresh", tokenStorage.refresh, "after $status")
        }
        assertEquals(0, sessionExpiredCalls)
    }

    @Test
    fun `a session without its refresh token is reported expired`() = runTest {
        tokenStorage.refresh = null
        val client = clientAnswering { error("the refresh endpoint must not be called") }

        client.get(ApiEndpoints.BUDGETS)

        assertNull(tokenStorage.auth)
        assertEquals(1, sessionExpiredCalls)
    }

    @Test
    fun `no tokens at all is not a session expiring`() = runTest {
        tokenStorage.auth = null
        tokenStorage.refresh = null
        val client = clientAnswering { error("the refresh endpoint must not be called") }

        client.get(ApiEndpoints.BUDGETS)

        assertEquals(0, sessionExpiredCalls)
    }

    @Test
    fun `an unreadable successful answer keeps the session`() = runTest {
        val client = clientAnswering {
            respond("<html>proxy page</html>", headers = headersOf(HttpHeaders.ContentType, "text/html"))
        }

        client.get(ApiEndpoints.BUDGETS)

        assertEquals("old-auth", tokenStorage.auth)
        assertEquals("old-refresh", tokenStorage.refresh)
        assertEquals(0, sessionExpiredCalls)
    }

    @Test
    fun `a server error while refreshing keeps the session`() = runTest {
        val client = clientAnswering { respondError(HttpStatusCode.BadGateway) }

        client.get(ApiEndpoints.BUDGETS)

        assertEquals("old-auth", tokenStorage.auth)
        assertEquals("old-refresh", tokenStorage.refresh)
        assertEquals(0, sessionExpiredCalls)
    }

    @Test
    fun `no connection while refreshing keeps the session`() = runTest {
        val client = clientAnswering { throw IOException("Network is unreachable") }

        client.get(ApiEndpoints.BUDGETS)

        assertEquals("old-auth", tokenStorage.auth)
        assertEquals("old-refresh", tokenStorage.refresh)
        assertEquals(0, sessionExpiredCalls)
    }

    @Test
    fun `a successful refresh stores the new tokens and retries the request`() = runTest {
        var budgetCalls = 0
        val client = createClient { request ->
            when (request.url.encodedPath) {
                ApiEndpoints.REFRESH_TOKEN -> respond(
                    content = """{"authToken":"new-auth","refreshToken":"new-refresh","email":"a@b.c","name":"A"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> {
                    budgetCalls++
                    if (request.headers[HttpHeaders.Authorization] == "Bearer new-auth") {
                        respond("[]", headers = headersOf(HttpHeaders.ContentType, "application/json"))
                    } else {
                        respondError(HttpStatusCode.Unauthorized)
                    }
                }
            }
        }

        val response = client.get(ApiEndpoints.BUDGETS)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(2, budgetCalls)
        assertEquals("new-auth", tokenStorage.auth)
        assertEquals("new-refresh", tokenStorage.refresh)
        assertEquals(0, sessionExpiredCalls)
    }

    @Test
    fun `a request opting out of the refresh keeps its own token and is not refreshed on 401`() = runTest {
        val sent = mutableListOf<HttpRequestData>()
        val client = createClient { request ->
            sent += request
            respondError(HttpStatusCode.Unauthorized)
        }

        val response = client.delete(ApiEndpoints.ME) {
            attributes.put(AuthCircuitBreaker, Unit)
            bearerAuth("own-token")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("Bearer own-token", sent.single().headers[HttpHeaders.Authorization])
        assertEquals("old-auth", tokenStorage.auth)
        assertEquals(0, sessionExpiredCalls)
    }

    @Test
    fun `only a 401 counts as the token being refused`() {
        assertTrue(isRefreshRejected(HttpStatusCode.Unauthorized))
        assertFalse(isRefreshRejected(HttpStatusCode.BadRequest))
        assertFalse(isRefreshRejected(HttpStatusCode.Forbidden))
        assertFalse(isRefreshRejected(HttpStatusCode.InternalServerError))
        assertFalse(isRefreshRejected(HttpStatusCode.BadGateway))
    }

    /** The API answers 401 to everything; [refreshAnswer] answers the refresh call. */
    private fun clientAnswering(
        refreshAnswer: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ) = createClient { request ->
        if (request.url.encodedPath == ApiEndpoints.REFRESH_TOKEN) {
            refreshAnswer(request)
        } else {
            respondError(HttpStatusCode.Unauthorized)
        }
    }

    private fun createClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ) = createHttpClient(
        engine = mockEngineFactory(handler),
        baseUrl = "https://api.test",
        tokenStorage = tokenStorage,
        appLogger = SilentLogger,
        logLevel = LogLevel.NONE,
        onSessionExpired = { sessionExpiredCalls++ }
    )

    /**
     * A new engine per client: the refresh call builds its own client from the same factory and
     * closes it afterwards, which would close a shared engine under the main client.
     */
    private fun mockEngineFactory(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ) = object : HttpClientEngineFactory<MockEngineConfig> {
        override fun create(block: MockEngineConfig.() -> Unit): HttpClientEngine =
            MockEngine(MockEngineConfig().apply { addHandler(handler) }.apply(block))
    }

    private class InMemoryTokenStorage(var auth: String?, var refresh: String?) : TokenStorage {
        override suspend fun saveAuthToken(token: String) { auth = token }
        override suspend fun getAuthToken(): String? = auth
        override suspend fun saveRefreshToken(token: String) { refresh = token }
        override suspend fun getRefreshToken(): String? = refresh
        override suspend fun clearTokens() {
            auth = null
            refresh = null
        }
    }

    private object SilentLogger : Logger {
        override fun debug(tag: String, message: String) = Unit
        override fun info(tag: String, message: String) = Unit
        override fun warn(tag: String, message: String, error: Throwable?) = Unit
        override fun error(tag: String, message: String, error: Throwable?) = Unit
    }
}
