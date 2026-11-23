package com.meneses.budgethunter.commons.data.network

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.auth.data.TokenStorage
import com.meneses.budgethunter.budgetEntry.data.network.BudgetEntryApiService
import com.meneses.budgethunter.budgetList.data.network.BudgetApiService
import com.meneses.budgethunter.collaborator.data.network.CollaboratorApiService
import com.meneses.budgethunter.commons.data.network.models.CreateBudgetRequest
import com.meneses.budgethunter.commons.data.network.models.CurrentUser
import com.meneses.budgethunter.commons.data.network.models.UpdateBudgetEntryRequest
import com.meneses.budgethunter.commons.data.network.models.UpdateUserPreferencesRequest
import com.meneses.budgethunter.commons.data.sync.NoOpLogger
import com.meneses.budgethunter.settings.data.UserPreferencesRepository
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The services through the app's own client ([createHttpClient]), which does not set `expectSuccess`:
 * a refusal must reach the caller as its [ApiError], not as an unreadable body, and never as a success.
 */
class ApiStatusTest {

    private val tokens = object : TokenStorage {
        override suspend fun saveAuthToken(token: String) = Unit
        override suspend fun getAuthToken(): String = "access"
        override suspend fun saveRefreshToken(token: String) = Unit
        override suspend fun getRefreshToken(): String = "refresh"
        override suspend fun clearTokens() = Unit
    }

    private fun client(
        socketTimeoutMillis: Long? = null,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ) = createHttpClient(
        engine = object : HttpClientEngineFactory<MockEngineConfig> {
            override fun create(block: MockEngineConfig.() -> Unit): HttpClientEngine =
                MockEngine(MockEngineConfig().apply { addHandler(handler) }.apply(block))
        },
        baseUrl = "https://api.test",
        tokenStorage = tokens,
        appLogger = NoOpLogger(),
        logLevel = LogLevel.NONE,
        socketTimeoutMillis = socketTimeoutMillis
    )

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    /** The server's usual error body, which is not what any of these calls reads on a success. */
    private fun client(status: HttpStatusCode, body: String = """{"status":${status.value},"message":"No"}""") =
        client { respond(body, status, jsonHeaders) }

    private fun entries(status: HttpStatusCode) = BudgetEntryApiService(client(status), Dispatchers.Unconfined)
    private fun budgets(status: HttpStatusCode) = BudgetApiService(client(status), Dispatchers.Unconfined)
    private fun members(status: HttpStatusCode) =
        CollaboratorApiService(client(status), Dispatchers.Unconfined, NoOpLogger())
    private fun preferences(status: HttpStatusCode) = UserPreferencesRepository(client(status), Dispatchers.Unconfined)
    private fun auth(status: HttpStatusCode, body: String = "") =
        AuthRepository(client(status, body), tokens, Dispatchers.Unconfined, NoOpLogger())

    private val entryUpdate = UpdateBudgetEntryRequest(
        amount = 10.0,
        description = "Lunch",
        category = "FOOD",
        type = "OUTCOME",
        date = "2026-11-20"
    )

    @Test
    fun `an entry deleted elsewhere answers its edit with a 400 the sync can recognize`() = runTest {
        val result = entries(HttpStatusCode.BadRequest).updateEntry(1, 2, entryUpdate)

        assertEquals(ApiError.BadRequest, result.exceptionOrNull())
    }

    @Test
    fun `a categorization already running is a conflict and AI turned off is forbidden`() = runTest {
        assertEquals(ApiError.Conflict, entries(HttpStatusCode.Conflict).categorizeEntries(1).exceptionOrNull())
        assertEquals(ApiError.Forbidden, entries(HttpStatusCode.Forbidden).categorizeEntries(1).exceptionOrNull())
    }

    @Test
    fun `a deletion the server could not do is not taken as done`() = runTest {
        assertEquals(
            ApiError.ServerError,
            entries(HttpStatusCode.InternalServerError).deleteEntry(1, 2).exceptionOrNull()
        )
        assertEquals(ApiError.ServerError, budgets(HttpStatusCode.ServiceUnavailable).deleteBudget(1).exceptionOrNull())
        assertEquals(
            ApiError.ServerError,
            members(HttpStatusCode.InternalServerError).removeCollaborator(1, "friend@example.com").exceptionOrNull()
        )
    }

    @Test
    fun `a deletion of something already gone still counts as gone`() = runTest {
        assertTrue(entries(HttpStatusCode.NotFound).deleteEntry(1, 2).exceptionOrNull()!!.isGoneFromServer())
        assertTrue(budgets(HttpStatusCode.Forbidden).deleteBudget(1).exceptionOrNull()!!.isGoneFromServer())
        assertEquals(ApiError.NotFound, members(HttpStatusCode.NotFound).removeCollaborator(1, "a@b.c").exceptionOrNull())
    }

    @Test
    fun `a deletion the server confirms succeeds`() = runTest {
        assertTrue(entries(HttpStatusCode.NoContent).deleteEntry(1, 2).isSuccess)
        assertTrue(budgets(HttpStatusCode.NoContent).deleteBudget(1).isSuccess)
    }

    @Test
    fun `budgets, entries, members and preferences keep their status`() = runTest {
        assertEquals(
            ApiError.Forbidden,
            budgets(HttpStatusCode.Forbidden).updateBudget(1, CreateBudgetRequest("Casa", 10.0)).exceptionOrNull()
        )
        assertEquals(ApiError.ServerError, budgets(HttpStatusCode.BadGateway).getBudgets().exceptionOrNull())
        assertEquals(ApiError.Forbidden, entries(HttpStatusCode.Forbidden).getEntries(1).exceptionOrNull())
        assertEquals(ApiError.Forbidden, members(HttpStatusCode.Forbidden).getCollaborators(1).exceptionOrNull())
        val preferencesUpdate = UpdateUserPreferencesRequest(smsReadingEnabled = false, aiProcessingEnabled = true)
        assertEquals(
            ApiError.ServerError,
            preferences(HttpStatusCode.InternalServerError).save(preferencesUpdate).exceptionOrNull()
        )
    }

    @Test
    fun `a wrong current password is not reported as changed`() = runTest {
        // The backend answers 401 for it: the client renews the session once, retries and still gets the 401
        var sessionExpired = false
        val client = createHttpClient(
            engine = object : HttpClientEngineFactory<MockEngineConfig> {
                override fun create(block: MockEngineConfig.() -> Unit): HttpClientEngine = MockEngine(
                    MockEngineConfig().apply {
                        addHandler { request ->
                            if (request.url.encodedPath == ApiEndpoints.REFRESH_TOKEN) {
                                respond(
                                    """{"authToken":"a2","refreshToken":"r2","email":"me@example.com","name":"Me"}""",
                                    HttpStatusCode.OK,
                                    jsonHeaders
                                )
                            } else {
                                respond(
                                    """{"status":401,"message":"Current password is incorrect"}""",
                                    HttpStatusCode.Unauthorized,
                                    jsonHeaders
                                )
                            }
                        }
                    }.apply(block)
                )
            },
            baseUrl = "https://api.test",
            tokenStorage = tokens,
            appLogger = NoOpLogger(),
            logLevel = LogLevel.NONE,
            onSessionExpired = { sessionExpired = true }
        )

        val repository = AuthRepository(client, tokens, Dispatchers.Unconfined, NoOpLogger())

        val result = repository.setPassword("wrong", "newPassword1")

        assertEquals(ApiError.Unauthorized, result.exceptionOrNull())
        assertFalse(sessionExpired)
    }

    @Test
    fun `a refused password change is not reported as changed`() = runTest {
        assertTrue(auth(HttpStatusCode.BadRequest).setPassword(null, "short").isFailure)
    }

    @Test
    fun `wrong credentials on sign in are unauthorized`() = runTest {
        val result = auth(HttpStatusCode.Unauthorized, """{"status":401,"message":"Invalid credentials"}""")
            .signIn("me@example.com", "wrong")

        assertEquals(ApiError.Unauthorized, result.exceptionOrNull())
    }

    @Test
    fun `a password change and the account read still succeed`() = runTest {
        assertTrue(auth(HttpStatusCode.NoContent).setPassword("old", "newPassword1").isSuccess)
        assertEquals(
            CurrentUser("me@example.com", "Me", hasPassword = true),
            auth(HttpStatusCode.OK, """{"email":"me@example.com","name":"Me","hasPassword":true}""")
                .getCurrentUser().getOrNull()
        )
    }

    @Test
    fun `a cancelled call is cancelled, not a failure`() = runTest {
        assertFailsWith<CancellationException> { safeApiCall { throw CancellationException("left the screen") } }
    }

    @Test
    fun `a request the engine gave up on is a timeout`() = runTest {
        val client = client { throw SocketTimeoutException("Request timed out") }
        val service = BudgetApiService(client, Dispatchers.Unconfined)

        assertEquals(ApiError.Timeout, service.getBudgets().exceptionOrNull())
    }

    @Test
    fun `the platform's socket timeout applies to plain requests, not to those that set their own`() = runTest {
        val timeouts = mutableMapOf<String, Long?>()
        val client = client(socketTimeoutMillis = 20_000) { request ->
            timeouts[request.url.encodedPath] = request.getCapabilityOrNull(HttpTimeoutCapability)?.socketTimeoutMillis
            respond("[]", HttpStatusCode.OK, jsonHeaders)
        }
        val entries = BudgetEntryApiService(client, Dispatchers.Unconfined)

        entries.getEntries(1)
        entries.categorizeEntries(1)

        assertEquals(20_000L, timeouts["/api/budgets/1/entries"])
        assertEquals(120_000L, timeouts["/api/budgets/1/entries/categorize"])
    }

    @Test
    fun `the session refresh gets the platform's socket timeout too`() = runTest {
        var refreshTimeout: Long? = null
        val client = client(socketTimeoutMillis = 20_000) { request ->
            if (request.url.encodedPath == ApiEndpoints.REFRESH_TOKEN) {
                refreshTimeout = request.getCapabilityOrNull(HttpTimeoutCapability)?.socketTimeoutMillis
                respond("", HttpStatusCode.InternalServerError, jsonHeaders)
            } else {
                respond("", HttpStatusCode.Unauthorized, jsonHeaders)
            }
        }

        BudgetApiService(client, Dispatchers.Unconfined).getBudgets()

        assertEquals(20_000L, refreshTimeout)
    }
}
