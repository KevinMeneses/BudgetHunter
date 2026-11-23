package com.meneses.budgethunter.commons.data.network

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.auth.data.TokenStorage
import com.meneses.budgethunter.budgetEntry.data.network.BudgetEntryApiService
import com.meneses.budgethunter.budgetList.data.network.BudgetApiService
import com.meneses.budgethunter.collaborator.data.network.CollaboratorApiService
import com.meneses.budgethunter.commons.data.network.models.CreateBudgetRequest
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
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

    /** The server's usual error body, which is not what any of these calls reads on a success. */
    private fun client(status: HttpStatusCode, body: String = """{"status":${status.value},"message":"No"}""") =
        client { respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())) }

    private fun entries(status: HttpStatusCode) = BudgetEntryApiService(client(status), Dispatchers.Unconfined)
    private fun budgets(status: HttpStatusCode) = BudgetApiService(client(status), Dispatchers.Unconfined)

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
        assertEquals(ApiError.ServerError, entries(HttpStatusCode.InternalServerError).deleteEntry(1, 2).exceptionOrNull())
        assertEquals(ApiError.ServerError, budgets(HttpStatusCode.ServiceUnavailable).deleteBudget(1).exceptionOrNull())
        val removal = CollaboratorApiService(client(HttpStatusCode.InternalServerError), Dispatchers.Unconfined, NoOpLogger())
            .removeCollaborator(1, "friend@example.com")
        assertEquals(ApiError.ServerError, removal.exceptionOrNull())
    }

    @Test
    fun `a deletion of something already gone still counts as gone`() = runTest {
        assertTrue(entries(HttpStatusCode.NotFound).deleteEntry(1, 2).exceptionOrNull()!!.isGoneFromServer())
        assertTrue(budgets(HttpStatusCode.Forbidden).deleteBudget(1).exceptionOrNull()!!.isGoneFromServer())
    }

    @Test
    fun `a deletion the server confirms succeeds`() = runTest {
        assertTrue(entries(HttpStatusCode.NoContent).deleteEntry(1, 2).isSuccess)
        assertTrue(budgets(HttpStatusCode.NoContent).deleteBudget(1).isSuccess)
    }

    @Test
    fun `budgets, entries, members and preferences keep their status`() = runTest {
        assertEquals(ApiError.Forbidden, budgets(HttpStatusCode.Forbidden).updateBudget(1, CreateBudgetRequest("Casa", 10.0)).exceptionOrNull())
        assertEquals(ApiError.ServerError, budgets(HttpStatusCode.BadGateway).getBudgets().exceptionOrNull())
        assertEquals(ApiError.Forbidden, entries(HttpStatusCode.Forbidden).getEntries(1).exceptionOrNull())
        assertEquals(
            ApiError.Forbidden,
            CollaboratorApiService(client(HttpStatusCode.Forbidden), Dispatchers.Unconfined, NoOpLogger()).getCollaborators(1).exceptionOrNull()
        )
        assertEquals(
            ApiError.ServerError,
            UserPreferencesRepository(client(HttpStatusCode.InternalServerError), Dispatchers.Unconfined)
                .save(UpdateUserPreferencesRequest(smsReadingEnabled = false, aiProcessingEnabled = true)).exceptionOrNull()
        )
    }

    @Test
    fun `a refused password change is not reported as changed`() = runTest {
        val repository = AuthRepository(client(HttpStatusCode.BadRequest), tokens, Dispatchers.Unconfined, NoOpLogger())

        assertTrue(repository.setPassword("wrong", "newPassword1").isFailure)
    }

    @Test
    fun `a request the engine gave up on is a timeout`() = runTest {
        val service = BudgetApiService(client { throw SocketTimeoutException("Request timed out") }, Dispatchers.Unconfined)

        assertEquals(ApiError.Timeout, service.getBudgets().exceptionOrNull())
    }

    @Test
    fun `the platform's socket timeout applies to plain requests, not to those that set their own`() = runTest {
        val timeouts = mutableMapOf<String, Long?>()
        val client = client(socketTimeoutMillis = 20_000) { request ->
            timeouts[request.url.encodedPath] = request.getCapabilityOrNull(HttpTimeoutCapability)?.socketTimeoutMillis
            respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }
        val entries = BudgetEntryApiService(client, Dispatchers.Unconfined)

        entries.getEntries(1)
        entries.categorizeEntries(1)

        assertEquals(20_000L, timeouts["/api/budgets/1/entries"])
        assertEquals(120_000L, timeouts["/api/budgets/1/entries/categorize"])
    }
}
