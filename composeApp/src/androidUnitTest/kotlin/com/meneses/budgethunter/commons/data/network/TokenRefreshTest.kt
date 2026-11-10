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
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun `a refresh token refused as a bad request clears the session`() = runTest {
        val client = clientAnswering { respondError(HttpStatusCode.BadRequest) }

        client.get(ApiEndpoints.BUDGETS)

        assertNull(tokenStorage.refresh)
        assertEquals(1, sessionExpiredCalls)
    }

    @Test
    fun `a forbidden refresh clears the session`() = runTest {
        val client = clientAnswering { respondError(HttpStatusCode.Forbidden) }

        client.get(ApiEndpoints.BUDGETS)

        assertNull(tokenStorage.refresh)
        assertEquals(1, sessionExpiredCalls)
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
    fun `only a refusal of the token itself counts as rejected`() {
        assertTrue(isRefreshRejected(HttpStatusCode.Unauthorized))
        assertTrue(isRefreshRejected(HttpStatusCode.BadRequest))
        assertTrue(isRefreshRejected(HttpStatusCode.Forbidden))
        assertFalse(isRefreshRejected(HttpStatusCode.InternalServerError))
        assertFalse(isRefreshRejected(HttpStatusCode.BadGateway))
        assertFalse(isRefreshRejected(HttpStatusCode.TooManyRequests))
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
