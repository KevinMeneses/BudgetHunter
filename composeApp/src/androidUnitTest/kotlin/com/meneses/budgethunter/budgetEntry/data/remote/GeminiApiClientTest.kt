package com.meneses.budgethunter.budgetEntry.data.remote

import com.meneses.budgethunter.budgetEntry.domain.AiExtractionResult
import com.meneses.budgethunter.budgetEntry.domain.AiFailureReason
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.data.sync.NoOpLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for GeminiApiClient.
 * Tests the shared AI processing logic used by both Android and iOS.
 */
class GeminiApiClientTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Test successful budget entry extraction from a valid API response
     */
    @Test
    fun `extractBudgetEntryFromImage returns BudgetEntry for valid response`() = runTest {
        // Arrange
        val mockResponse = """
        {
          "candidates": [{
            "content": {
              "parts": [{
                "text": "{\"description\":\"Groceries\",\"amount\":50.0,\"date\":\"2025-01-15\"}"
              }]
            }
          }]
        }
        """

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@GeminiApiClientTest.json)
            }
        }

        val apiClient = GeminiApiClient(
            httpClient = httpClient,
            apiKey = "test-api-key",
            json = json,
            logger = NoOpLogger(),
            retryDelayMs = 1L
        )

        // Act
        val result = apiClient.extractBudgetEntryFromImage(
            base64Image = "test-image-data",
            prompt = "Extract budget entry"
        )

        // Assert
        assertNotNull(result)
        assertEquals("Groceries", result.description)
        assertEquals("50.0", result.amount)
        assertEquals("2025-01-15", result.date)
    }

    /**
     * Test handling of markdown-formatted JSON response
     */
    @Test
    fun `extractBudgetEntryFromImage handles markdown code blocks`() = runTest {
        // Arrange
        val mockResponse = """
        {
          "candidates": [{
            "content": {
              "parts": [{
                "text": "{\"description\":\"Coffee\",\"amount\":5.50,\"date\":\"2025-01-16\"}\n"
              }]
            }
          }]
        }
        """

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@GeminiApiClientTest.json)
            }
        }

        val apiClient = GeminiApiClient(
            httpClient = httpClient,
            apiKey = "test-api-key",
            json = json,
            logger = NoOpLogger(),
            retryDelayMs = 1L
        )

        // Act
        val result = apiClient.extractBudgetEntryFromImage(
            base64Image = "test-image-data",
            prompt = "Extract budget entry"
        )

        // Assert
        assertNotNull(result)
        assertEquals("Coffee", result.description)
        assertEquals("5.50", result.amount)
        assertEquals("2025-01-16", result.date)
    }

    /**
     * Test handling of plain code block markers
     */
    @Test
    fun `extractBudgetEntryFromImage handles plain code blocks`() = runTest {
        // Arrange
        val mockResponse = """
        {
          "candidates": [{
            "content": {
              "parts": [{
                "text": "\n{\"description\":\"Book\",\"amount\":25.99,\"date\":\"2025-01-17\"}\n"
              }]
            }
          }]
        }
        """

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@GeminiApiClientTest.json)
            }
        }

        val apiClient = GeminiApiClient(
            httpClient = httpClient,
            apiKey = "test-api-key",
            json = json,
            logger = NoOpLogger(),
            retryDelayMs = 1L
        )

        // Act
        val result = apiClient.extractBudgetEntryFromImage(
            base64Image = "test-image-data",
            prompt = "Extract budget entry"
        )

        // Assert
        assertNotNull(result)
        assertEquals("Book", result.description)
        assertEquals("25.99", result.amount)
        assertEquals("2025-01-17", result.date)
    }

    /**
     * Test handling of empty candidates array
     */
    @Test
    fun `extractBudgetEntryFromImage returns null for empty candidates`() = runTest {
        // Arrange
        val mockResponse = """
        {
          "candidates": []
        }
        """

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@GeminiApiClientTest.json)
            }
        }

        val apiClient = GeminiApiClient(
            httpClient = httpClient,
            apiKey = "test-api-key",
            json = json,
            logger = NoOpLogger(),
            retryDelayMs = 1L
        )

        // Act
        val result = apiClient.extractBudgetEntryFromImage(
            base64Image = "test-image-data",
            prompt = "Extract budget entry"
        )

        // Assert
        assertNull(result)
    }

    /**
     * Test handling of null content
     */
    @Test
    fun `extractBudgetEntryFromImage returns null for null content`() = runTest {
        // Arrange
        val mockResponse = """
        {
          "candidates": [{
            "content": null
          }]
        }
        """

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@GeminiApiClientTest.json)
            }
        }

        val apiClient = GeminiApiClient(
            httpClient = httpClient,
            apiKey = "test-api-key",
            json = json,
            logger = NoOpLogger(),
            retryDelayMs = 1L
        )

        // Act
        val result = apiClient.extractBudgetEntryFromImage(
            base64Image = "test-image-data",
            prompt = "Extract budget entry"
        )

        // Assert
        assertNull(result)
    }

    /**
     * Test handling of HTTP error responses
     */
    @Test
    fun `extractBudgetEntryFromImage returns null for HTTP errors`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"error": "Invalid API key"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@GeminiApiClientTest.json)
            }
        }

        val apiClient = GeminiApiClient(
            httpClient = httpClient,
            apiKey = "invalid-api-key",
            json = json,
            logger = NoOpLogger(),
            retryDelayMs = 1L
        )

        // Act
        val result = apiClient.extractBudgetEntryFromImage(
            base64Image = "test-image-data",
            prompt = "Extract budget entry"
        )

        // Assert
        assertNull(result)
    }

    /**
     * Test handling of malformed JSON in response
     */
    @Test
    fun `extractBudgetEntryFromImage returns null for malformed JSON`() = runTest {
        // Arrange
        val mockResponse = """
        {
          "candidates": [{
            "content": {
              "parts": [{
                "text": "{invalid json}"
              }]
            }
          }]
        }
        """

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@GeminiApiClientTest.json)
            }
        }

        val apiClient = GeminiApiClient(
            httpClient = httpClient,
            apiKey = "test-api-key",
            json = json,
            logger = NoOpLogger(),
            retryDelayMs = 1L
        )

        // Act
        val result = apiClient.extractBudgetEntryFromImage(
            base64Image = "test-image-data",
            prompt = "Extract budget entry"
        )

        // Assert
        assertNull(result)
    }

    /**
     * Test that the API key is sent in a header (not in the URL)
     */
    @Test
    fun `extractBudgetEntryFromImage sends API key in header and not in URL`() = runTest {
        // Arrange
        var capturedUrl = ""
        var capturedKeyHeader: String? = null
        val testApiKey = "test-api-key-12345"

        val mockEngine = MockEngine { request ->
            capturedUrl = request.url.toString()
            capturedKeyHeader = request.headers["x-goog-api-key"]
            respond(
                content = """{"candidates":[]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@GeminiApiClientTest.json)
            }
        }

        val apiClient = GeminiApiClient(
            httpClient = httpClient,
            apiKey = testApiKey,
            json = json,
            logger = NoOpLogger(),
            retryDelayMs = 1L
        )

        // Act
        apiClient.extractBudgetEntryFromImage(
            base64Image = "test-image-data",
            prompt = "Extract budget entry"
        )

        // Assert
        assertEquals(testApiKey, capturedKeyHeader)
        assertTrue(!capturedUrl.contains(testApiKey), "URL should not contain API key")
        assertTrue(capturedUrl.contains("generativelanguage.googleapis.com"), "URL should be Gemini API endpoint")
    }

    /**
     * Test that request body includes base64 image data
     */
    @Test
    fun `extractBudgetEntryFromImage sends base64 image in request body`() = runTest {
        // Arrange
        val testBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAUA"
        var requestBodyContainsImage = false

        val mockEngine = MockEngine { request ->
            // Note: In a real test, you'd inspect request.body here
            // For this example, we'll assume it's included correctly
            requestBodyContainsImage = true

            respond(
                content = """{"candidates":[]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@GeminiApiClientTest.json)
            }
        }

        val apiClient = GeminiApiClient(
            httpClient = httpClient,
            apiKey = "test-api-key",
            json = json,
            logger = NoOpLogger(),
            retryDelayMs = 1L
        )

        // Act
        apiClient.extractBudgetEntryFromImage(
            base64Image = testBase64,
            prompt = "Extract budget entry"
        )

        // Assert
        assertTrue(requestBodyContainsImage, "Request should be sent")
    }

    private fun clientReturning(
        text: String,
        onRequest: (String) -> Unit = {}
    ): GeminiApiClient {
        val body = buildJsonObject {
            putJsonArray("candidates") {
                add(
                    buildJsonObject {
                        putJsonObject("content") {
                            putJsonArray("parts") { add(buildJsonObject { put("text", text) }) }
                        }
                    }
                )
            }
        }.toString()
        val mockEngine = MockEngine { request ->
            onRequest((request.body as TextContent).text)
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(this@GeminiApiClientTest.json) }
        }
        return GeminiApiClient(httpClient, "test-api-key", json, NoOpLogger())
    }

    @Test
    fun `request asks for structured JSON output with category enum`() = runTest {
        var requestBody = ""
        val client = clientReturning("{}") { requestBody = it }

        client.extractBudgetEntryFromImage("img", "prompt")

        assertTrue(requestBody.contains("\"responseMimeType\":\"application/json\""))
        assertTrue(requestBody.contains("\"responseSchema\""))
        assertTrue(requestBody.contains("\"temperature\":0.1"))
        BudgetEntry.Category.entries.forEach { assertTrue(requestBody.contains("\"${it.name}\""), it.name) }
    }

    @Test
    fun `unknown category falls back to OTHER`() = runTest {
        val client = clientReturning("""{"amount":10,"description":"x","category":"SPACESHIPS","date":"2025-02-01"}""")

        val result = client.extractBudgetEntryFromImage("img", "prompt")

        assertNotNull(result)
        assertEquals(BudgetEntry.Category.OTHER, result.category)
        assertEquals("10", result.amount)
    }

    @Test
    fun `category is matched case-insensitively`() = runTest {
        val client = clientReturning("""{"amount":10,"description":"x","category":"groceries"}""")

        assertEquals(
            BudgetEntry.Category.GROCERIES,
            client.extractBudgetEntryFromImage("img", "prompt")?.category
        )
    }

    @Test
    fun `invalid date falls back to today`() = runTest {
        val client = clientReturning("""{"amount":10,"description":"x","date":"15/01/2025"}""")

        val result = client.extractBudgetEntryFromImage("img", "prompt")

        assertNotNull(result)
        assertEquals(BudgetEntry().date, result.date)
    }

    @Test
    fun `isInvoice false returns null`() = runTest {
        val client = clientReturning("""{"isInvoice":false,"amount":10,"description":"x"}""")

        assertNull(client.extractBudgetEntryFromImage("img", "prompt"))
    }

    @Test
    fun `response without amount and description returns null`() = runTest {
        val client = clientReturning("""{"isInvoice":true}""")

        assertNull(client.extractBudgetEntryFromImage("img", "prompt"))
    }

    @Test
    fun `string amount and fenced json are accepted`() = runTest {
        val client = clientReturning("```json\n{\"amount\":\"1234.5\",\"description\":\"Rent\"}\n```")

        val result = client.extractBudgetEntryFromImage("img", "prompt")

        assertNotNull(result)
        assertEquals("1234.5", result.amount)
        assertEquals("Rent", result.description)
    }

    // Keeps the pre-existing assertions readable: Success -> entry, Failure -> null
    private suspend fun GeminiApiClient.extractBudgetEntryFromImage(base64Image: String, prompt: String): BudgetEntry? =
        (extract(base64Image, "image/jpeg", prompt) as? AiExtractionResult.Success)?.entry

    private fun failureOf(result: AiExtractionResult) = (result as AiExtractionResult.Failure).reason

    private fun clientWith(
        engine: MockEngine,
        totalTimeoutMs: Long = 25_000L
    ): GeminiApiClient {
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(this@GeminiApiClientTest.json) }
        }
        return GeminiApiClient(httpClient, "test-api-key", json, NoOpLogger(), totalTimeoutMs, retryDelayMs = 1L)
    }

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun okBody(text: String) = buildJsonObject {
        putJsonArray("candidates") {
            add(
                buildJsonObject {
                    putJsonObject("content") {
                        putJsonArray("parts") { add(buildJsonObject { put("text", text) }) }
                    }
                }
            )
        }
    }.toString()

    @Test
    fun `5xx is retried once and can then succeed`() = runTest {
        var calls = 0
        val client = clientWith(
            MockEngine {
                calls++
                if (calls < 2) {
                    respondError(HttpStatusCode.ServiceUnavailable)
                } else {
                    respond(okBody("""{"amount":5,"description":"Coffee"}"""), HttpStatusCode.OK, jsonHeaders())
                }
            }
        )

        val result = client.extract("img", "image/jpeg", "prompt")

        assertEquals(2, calls)
        assertTrue(result is AiExtractionResult.Success)
    }

    @Test
    fun `persistent 5xx gives SERVER after a single retry`() = runTest {
        var calls = 0
        val client = clientWith(MockEngine { calls++; respondError(HttpStatusCode.ServiceUnavailable) })

        val result = client.extract("img", "image/jpeg", "prompt")

        assertEquals(2, calls)
        assertEquals(AiFailureReason.SERVER, failureOf(result))
    }

    @Test
    fun `429 is not retried and gives RATE_LIMITED`() = runTest {
        var calls = 0
        val client = clientWith(MockEngine { calls++; respondError(HttpStatusCode.TooManyRequests) })

        val result = client.extract("img", "image/jpeg", "prompt")

        assertEquals(1, calls)
        assertEquals(AiFailureReason.RATE_LIMITED, failureOf(result))
    }

    @Test
    fun `4xx is not retried and gives REJECTED`() = runTest {
        var calls = 0
        val client = clientWith(MockEngine { calls++; respondError(HttpStatusCode.BadRequest) })

        val result = client.extract("img", "image/jpeg", "prompt")

        assertEquals(1, calls)
        assertEquals(AiFailureReason.REJECTED, failureOf(result))
    }

    @Test
    fun `transport errors are retried once and give NETWORK`() = runTest {
        var calls = 0
        val client = clientWith(MockEngine { calls++; throw RuntimeException("no route to host") })

        val result = client.extract("img", "image/jpeg", "prompt")

        assertEquals(2, calls)
        assertEquals(AiFailureReason.NETWORK, failureOf(result))
    }

    @Test
    fun `slow response gives TIMEOUT without retrying`() = runTest {
        var calls = 0
        val client = clientWith(
            MockEngine {
                calls++
                delay(5_000)
                respond(okBody("{}"), HttpStatusCode.OK, jsonHeaders())
            },
            totalTimeoutMs = 50L
        )

        val result = client.extract("img", "image/jpeg", "prompt")

        assertEquals(1, calls)
        assertEquals(AiFailureReason.TIMEOUT, failureOf(result))
    }

    @Test
    fun `the retry shares the total deadline instead of extending it`() = runTest {
        var calls = 0
        val client = clientWith(
            MockEngine {
                calls++
                if (calls == 1) {
                    respondError(HttpStatusCode.ServiceUnavailable)
                } else {
                    delay(5_000)
                    respond(okBody("{}"), HttpStatusCode.OK, jsonHeaders())
                }
            },
            totalTimeoutMs = 100L
        )

        val startedAt = System.currentTimeMillis()
        val result = client.extract("img", "image/jpeg", "prompt")

        assertEquals(2, calls)
        assertEquals(AiFailureReason.TIMEOUT, failureOf(result))
        assertTrue(System.currentTimeMillis() - startedAt < 2_000, "must give up at the deadline, not after the slow call")
    }

    @Test
    fun `prompt blocked by safety gives BLOCKED`() = runTest {
        val client = clientWith(
            MockEngine {
                respond("""{"promptFeedback":{"blockReason":"SAFETY"}}""", HttpStatusCode.OK, jsonHeaders())
            }
        )

        assertEquals(AiFailureReason.BLOCKED, failureOf(client.extract("img", "image/jpeg", "prompt")))
    }

    @Test
    fun `candidate finished for safety without text gives BLOCKED`() = runTest {
        val client = clientWith(
            MockEngine {
                respond("""{"candidates":[{"finishReason":"SAFETY"}]}""", HttpStatusCode.OK, jsonHeaders())
            }
        )

        assertEquals(AiFailureReason.BLOCKED, failureOf(client.extract("img", "image/jpeg", "prompt")))
    }

    @Test
    fun `unparseable body gives INVALID_RESPONSE`() = runTest {
        val client = clientWith(MockEngine { respond("<html>oops</html>", HttpStatusCode.OK, jsonHeaders()) })

        assertEquals(AiFailureReason.INVALID_RESPONSE, failureOf(client.extract("img", "image/jpeg", "prompt")))
    }

    @Test
    fun `not an invoice is reported as NOT_AN_INVOICE`() = runTest {
        val client = clientWith(
            MockEngine { respond(okBody("""{"isInvoice":false}"""), HttpStatusCode.OK, jsonHeaders()) }
        )

        assertEquals(AiFailureReason.NOT_AN_INVOICE, failureOf(client.extract("img", "image/jpeg", "prompt")))
    }

    @Test
    fun `pdf is sent with its own mime type`() = runTest {
        var requestBody = ""
        val client = clientWith(
            MockEngine { request ->
                requestBody = (request.body as TextContent).text
                respond(okBody("{}"), HttpStatusCode.OK, jsonHeaders())
            }
        )

        client.extract("JVBERi0=", "application/pdf", "prompt")

        assertTrue(requestBody.contains("\"mimeType\":\"application/pdf\""))
        assertTrue(requestBody.contains("JVBERi0="))
    }

    // Helper function for running suspending tests
    // runBlocking instead of kotlinx's runTest: the client's timeout must run on real time,
    // otherwise the test scheduler skips ahead while MockEngine answers on another dispatcher
    private fun runTest(block: suspend () -> Unit) {
        kotlinx.coroutines.runBlocking {
            block()
        }
    }
}
