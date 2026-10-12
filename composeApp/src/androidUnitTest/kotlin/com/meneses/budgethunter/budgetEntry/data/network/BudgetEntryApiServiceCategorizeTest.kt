package com.meneses.budgethunter.budgetEntry.data.network

import com.meneses.budgethunter.commons.data.network.ApiEndpoints
import com.meneses.budgethunter.commons.data.network.ApiError
import com.meneses.budgethunter.commons.data.network.models.CategorizeEntriesResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The client side of `POST /api/budgets/{id}/entries/categorize`. */
class BudgetEntryApiServiceCategorizeTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var lastRequest: HttpRequestData? = null

    private fun serviceReturning(status: HttpStatusCode, body: String = ""): BudgetEntryApiService {
        val engine = MockEngine { request ->
            lastRequest = request
            if (status.value < 400) {
                respond(
                    content = body,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            } else {
                respondError(status, body)
            }
        }
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout)
        }
        return BudgetEntryApiService(client, Dispatchers.Unconfined)
    }

    @Test
    fun `posts to the categorize endpoint of the budget`() = runTest {
        val service = serviceReturning(HttpStatusCode.OK, """{"categorized":3,"pending":1}""")

        service.categorizeEntries(budgetId = 42L)

        val request = assertNotNull(lastRequest)
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/budgets/42/entries/categorize", request.url.encodedPath)
    }

    @Test
    fun `reads how many were categorized and how many are still pending`() = runTest {
        val service = serviceReturning(HttpStatusCode.OK, """{"categorized":12,"pending":3}""")

        val result = service.categorizeEntries(42L)

        assertEquals(CategorizeEntriesResponse(categorized = 12, pending = 3), result.getOrNull())
    }

    @Test
    fun `ignores fields it does not know`() = runTest {
        val service = serviceReturning(HttpStatusCode.OK, """{"categorized":1,"pending":0,"something":"new"}""")

        assertEquals(CategorizeEntriesResponse(1, 0), service.categorizeEntries(1L).getOrNull())
    }

    @Test
    fun `a 403 is Forbidden, which is how the server says AI processing is off or there is no access`() = runTest {
        val result = serviceReturning(HttpStatusCode.Forbidden).categorizeEntries(42L)

        assertIs<ApiError.Forbidden>(result.exceptionOrNull())
    }

    @Test
    fun `a 409 is Conflict, a run for this budget is already in flight`() = runTest {
        val result = serviceReturning(HttpStatusCode.Conflict).categorizeEntries(42L)

        assertIs<ApiError.Conflict>(result.exceptionOrNull())
    }

    @Test
    fun `a 401 is Unauthorized and a 5xx is a server error`() = runTest {
        assertIs<ApiError.Unauthorized>(serviceReturning(HttpStatusCode.Unauthorized).categorizeEntries(1L).exceptionOrNull())
        assertIs<ApiError.ServerError>(
            serviceReturning(HttpStatusCode.InternalServerError).categorizeEntries(1L).exceptionOrNull()
        )
    }

    @Test
    fun `an unreadable answer is a parse error, not a crash`() = runTest {
        val result = serviceReturning(HttpStatusCode.OK, """{"categorized":"many"}""").categorizeEntries(1L)

        assertTrue(result.isFailure)
        assertIs<ApiError.ParseError>(result.exceptionOrNull())
    }

    @Test
    fun `the endpoint path is built from the server budget id`() {
        assertEquals("/api/budgets/7/entries/categorize", ApiEndpoints.budgetEntriesCategorize(7L))
    }
}
