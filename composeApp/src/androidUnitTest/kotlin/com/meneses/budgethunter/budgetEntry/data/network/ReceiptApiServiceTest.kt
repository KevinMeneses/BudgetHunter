package com.meneses.budgethunter.budgetEntry.data.network

import com.meneses.budgethunter.budgetEntry.domain.AiExtractionResult
import com.meneses.budgethunter.budgetEntry.domain.AiFailureReason
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.data.network.ApiEndpoints
import com.meneses.budgethunter.commons.data.sync.NoOpLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/** The client side of `POST /api/receipts/extract`: what is sent and how every answer is read. */
class ReceiptApiServiceTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    private val today = LocalDate.parse("2026-11-03")
    private var lastRequest: HttpRequestData? = null

    private val usage = """{"plan":"FREE","period":"2026-11","receiptScansUsed":3,"receiptScansLimit":10,"receiptScansRemaining":7,"resetsOn":"2026-12-01"}"""

    private fun service(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = "",
        expectSuccess: Boolean = false,
        failWith: Exception? = null,
        delayMillis: Long = 0,
        timeoutMillis: Long = 90_000
    ): ReceiptApiService {
        val engine = MockEngine { request ->
            lastRequest = request
            failWith?.let { throw it }
            if (delayMillis > 0) delay(delayMillis)
            respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }
        val client = HttpClient(engine) {
            this.expectSuccess = expectSuccess
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout)
        }
        return ReceiptApiService(client, json, NoOpLogger(), timeoutMillis)
    }

    @Test
    fun `posts the file, its type and the user's date`() = runTest {
        service(body = """{"isReceipt":false,"usage":$usage}""").extract("QUJD", "image/jpeg", today)

        val request = assertNotNull(lastRequest)
        assertEquals(HttpMethod.Post, request.method)
        assertEquals(ApiEndpoints.RECEIPTS_EXTRACT, request.url.encodedPath)
        val sent = json.parseToJsonElement((request.body as TextContent).text).jsonObject
        assertEquals("QUJD", sent["data"]?.jsonPrimitive?.content)
        assertEquals("image/jpeg", sent["mimeType"]?.jsonPrimitive?.content)
        assertEquals("2026-11-03", sent["today"]?.jsonPrimitive?.content)
    }

    @Test
    fun `a read receipt fills the entry`() = runTest {
        val result = service(
            body = """{"isReceipt":true,"amount":45900.5,"description":" Éxito ","category":"GROCERIES","date":"2026-11-02","usage":$usage}"""
        ).extract("QUJD", "image/jpeg", today)

        val entry = assertIs<AiExtractionResult.Success>(result).entry
        assertEquals("45900.5", entry.amount)
        assertEquals("Éxito", entry.description)
        assertEquals(BudgetEntry.Category.GROCERIES, entry.category)
        assertEquals("2026-11-02", entry.date)
    }

    @Test
    fun `an unknown category becomes OTHER and a bad date keeps the default`() = runTest {
        val result = service(
            body = """{"isReceipt":true,"amount":10,"description":"x","category":"PETS","date":"02/11/2026"}"""
        ).extract("QUJD", "image/jpeg", today)

        val entry = assertIs<AiExtractionResult.Success>(result).entry
        assertEquals(BudgetEntry.Category.OTHER, entry.category)
        assertEquals(BudgetEntry().date, entry.date)
    }

    @Test
    fun `not a receipt`() = runTest {
        val result = service(body = """{"isReceipt":false,"usage":$usage}""").extract("QUJD", "image/jpeg", today)
        assertEquals(AiFailureReason.NOT_AN_INVOICE, assertIs<AiExtractionResult.Failure>(result).reason)
    }

    @Test
    fun `an unreadable success body is INVALID_RESPONSE`() = runTest {
        val result = service(body = "<html>").extract("QUJD", "image/jpeg", today)
        assertEquals(AiFailureReason.INVALID_RESPONSE, assertIs<AiExtractionResult.Failure>(result).reason)
    }

    @Test
    fun `every error status maps to a reason the user can be told`() = runTest {
        val cases = listOf(
            HttpStatusCode.Unauthorized to "" to AiFailureReason.SIGN_IN_REQUIRED,
            HttpStatusCode.PaymentRequired to """{"status":402,"code":"AI_QUOTA_EXCEEDED","message":"x","usage":$usage}""" to AiFailureReason.QUOTA_EXCEEDED,
            HttpStatusCode.PayloadTooLarge to """{"code":"TOO_LARGE"}""" to AiFailureReason.FILE,
            HttpStatusCode.UnsupportedMediaType to """{"code":"UNSUPPORTED_TYPE"}""" to AiFailureReason.FILE,
            HttpStatusCode.UnprocessableEntity to """{"code":"AI_BLOCKED"}""" to AiFailureReason.BLOCKED,
            HttpStatusCode.TooManyRequests to "" to AiFailureReason.RATE_LIMITED,
            HttpStatusCode.BadGateway to """{"code":"AI_INVALID_RESPONSE"}""" to AiFailureReason.INVALID_RESPONSE,
            HttpStatusCode.ServiceUnavailable to """{"code":"AI_BUSY"}""" to AiFailureReason.RATE_LIMITED,
            HttpStatusCode.ServiceUnavailable to """{"code":"AI_UNAVAILABLE"}""" to AiFailureReason.SERVER,
            HttpStatusCode.GatewayTimeout to """{"code":"AI_TIMEOUT"}""" to AiFailureReason.TIMEOUT,
            HttpStatusCode.InternalServerError to "" to AiFailureReason.SERVER,
            HttpStatusCode.BadRequest to "" to AiFailureReason.REJECTED
        )
        cases.forEach { (statusAndBody, expected) ->
            val (status, body) = statusAndBody
            listOf(false, true).forEach { expectSuccess ->
                val result = service(status, body, expectSuccess = expectSuccess).extract("QUJD", "image/jpeg", today)
                assertEquals(
                    expected,
                    assertIs<AiExtractionResult.Failure>(result).reason,
                    "HTTP ${status.value} $body (expectSuccess=$expectSuccess)"
                )
            }
        }
    }

    @Test
    fun `a transport error is NETWORK`() = runTest {
        val result = service(failWith = IOException("no route")).extract("QUJD", "image/jpeg", today)
        assertEquals(AiFailureReason.NETWORK, assertIs<AiExtractionResult.Failure>(result).reason)
    }

    @Test
    fun `a request over the deadline is TIMEOUT`() = runTest {
        val result = service(body = "{}", delayMillis = 5_000, timeoutMillis = 100).extract("QUJD", "image/jpeg", today)
        assertEquals(AiFailureReason.TIMEOUT, assertIs<AiExtractionResult.Failure>(result).reason)
    }
}
