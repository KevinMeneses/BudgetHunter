package com.meneses.budgethunter.billing.data

import com.meneses.budgethunter.commons.data.network.ApiEndpoints
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BillingApiServiceTest {

    private val json = Json { ignoreUnknownKeys = true }
    private var lastRequest: HttpRequestData? = null

    private fun service(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = "",
        failWith: Exception? = null
    ): BillingApiService {
        val engine = MockEngine { request ->
            lastRequest = request
            failWith?.let { throw it }
            respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }
        return BillingApiService(HttpClient(engine), json)
    }

    @Test
    fun `status reads the RevenueCat id and the plan`() = runTest {
        val status = service(body = """{"appUserId":"abc-123","plan":"PREMIUM","premiumUntil":"2026-12-07T10:00:00Z","x":1}""")
            .status()

        assertEquals("abc-123", status.appUserId)
        assertEquals("PREMIUM", status.plan)
        assertEquals("2026-12-07T10:00:00Z", status.premiumUntil)
        assertEquals(HttpMethod.Get, lastRequest?.method)
        assertEquals(ApiEndpoints.BILLING_STATUS, lastRequest?.url?.encodedPath)
    }

    @Test
    fun `sync posts and reads the answer`() = runTest {
        val status = service(body = """{"appUserId":"abc-123","plan":"FREE"}""").sync()

        assertEquals("FREE", status.plan)
        assertNull(status.premiumUntil)
        assertEquals(HttpMethod.Post, lastRequest?.method)
        assertEquals(ApiEndpoints.BILLING_SYNC, lastRequest?.url?.encodedPath)
    }

    @Test
    fun `an error status, an unreadable body and no connection are BillingApiExceptions`() = runTest {
        val throttled = assertFailsWith<BillingApiException> {
            service(status = HttpStatusCode.TooManyRequests, body = "{}").sync()
        }
        assertEquals(429, throttled.status)

        val unreadable = assertFailsWith<BillingApiException> { service(body = "<html>").status() }
        assertEquals(200, unreadable.status)

        val offline = assertFailsWith<BillingApiException> { service(failWith = IOException("offline")).status() }
        assertNull(offline.status)
    }
}
