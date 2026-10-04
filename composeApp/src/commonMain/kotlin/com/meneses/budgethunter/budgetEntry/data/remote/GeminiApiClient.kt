package com.meneses.budgethunter.budgetEntry.data.remote

import com.meneses.budgethunter.budgetEntry.domain.AiExtractionResult
import com.meneses.budgethunter.budgetEntry.domain.AiFailureReason
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.data.sync.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/**
 * Remote data source for Gemini API.
 * Handles HTTP requests to Google's Gemini REST API for AI-powered budget entry extraction.
 *
 * The whole operation has a single deadline ([totalTimeoutMs]) so the user never waits long for a
 * failure. Only 5xx and transport errors, which fail fast, get one retry; timeouts, rate limits and
 * every other outcome are mapped straight to an [AiFailureReason] so callers can tell the user what happened.
 */
class GeminiApiClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val json: Json,
    private val logger: Logger,
    private val totalTimeoutMs: Long = DEFAULT_TOTAL_TIMEOUT_MS,
    private val retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS
) {
    private val tag = "GeminiApiClient"

    private sealed interface Attempt {
        data class Done(val result: AiExtractionResult) : Attempt
        data class Retry(val reason: AiFailureReason) : Attempt
    }

    /**
     * Sends a document (image or PDF) and a prompt to the Gemini API.
     *
     * @param base64Data Base64-encoded file content
     * @param mimeType MIME type of the file, e.g. "image/jpeg" or "application/pdf"
     * @param prompt The extraction prompt
     */
    suspend fun extract(
        base64Data: String,
        mimeType: String,
        prompt: String
    ): AiExtractionResult {
        val requestBody = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt),
                        GeminiPart(inlineData = InlineData(mimeType = mimeType, data = base64Data))
                    )
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                responseSchema = EXTRACTION_SCHEMA,
                temperature = 0.1
            )
        )

        return try {
            // One deadline for everything (upload, model time, retry) so retries can't add up
            withTimeout(totalTimeoutMs) { requestWithRetry(requestBody) }
        } catch (e: TimeoutCancellationException) {
            logger.warn(tag, "Gemini request timed out after ${totalTimeoutMs}ms", e)
            AiExtractionResult.Failure(AiFailureReason.TIMEOUT)
        }
    }

    private suspend fun requestWithRetry(requestBody: GeminiRequest): AiExtractionResult {
        var lastReason = AiFailureReason.UNKNOWN
        repeat(MAX_ATTEMPTS) { attempt ->
            when (val outcome = attemptRequest(requestBody)) {
                is Attempt.Done -> return outcome.result
                is Attempt.Retry -> {
                    lastReason = outcome.reason
                    if (attempt < MAX_ATTEMPTS - 1) delay(retryDelayMs)
                }
            }
        }
        logger.warn(tag, "Gemini request failed after $MAX_ATTEMPTS attempts: $lastReason")
        return AiExtractionResult.Failure(lastReason)
    }

    private suspend fun attemptRequest(requestBody: GeminiRequest): Attempt {
        return try {
            val response = httpClient.post(ENDPOINT) {
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            val status = response.status.value
            val body = response.bodyAsText()
            when {
                status in 200..299 -> Attempt.Done(parseResponse(body))
                // Quota errors don't clear in a second, retrying only makes the user wait longer
                status == 429 -> Attempt.Done(AiExtractionResult.Failure(AiFailureReason.RATE_LIMITED))
                status >= 500 -> Attempt.Retry(AiFailureReason.SERVER)
                else -> {
                    logger.warn(tag, "Gemini rejected the request with HTTP $status")
                    Attempt.Done(AiExtractionResult.Failure(AiFailureReason.REJECTED))
                }
            }
        } catch (e: CancellationException) {
            // Also covers the total deadline: extract() turns it into TIMEOUT
            throw e
        } catch (e: Exception) {
            logger.warn(tag, "Gemini request failed", e)
            Attempt.Retry(AiFailureReason.NETWORK)
        }
    }

    private fun parseResponse(body: String): AiExtractionResult {
        val response = try {
            json.decodeFromString<GeminiResponse>(body)
        } catch (e: SerializationException) {
            logger.warn(tag, "Unreadable Gemini response", e)
            return failure(AiFailureReason.INVALID_RESPONSE)
        }

        if (response.promptFeedback?.blockReason != null) return failure(AiFailureReason.BLOCKED)

        val candidate = response.candidates?.firstOrNull()
        val text = candidate?.content?.parts?.firstOrNull { it.text != null }?.text
        if (text == null) {
            val blocked = candidate?.finishReason in BLOCKED_FINISH_REASONS
            return failure(if (blocked) AiFailureReason.BLOCKED else AiFailureReason.INVALID_RESPONSE)
        }
        return parseExtraction(text)
    }

    private fun parseExtraction(responseText: String): AiExtractionResult {
        // Structured output should already be plain JSON; strip code fences defensively
        val refinedText = responseText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        if (refinedText.isBlank() || refinedText == "{}") return failure(AiFailureReason.NOT_AN_INVOICE)

        val extraction = try {
            json.decodeFromString<GeminiExtraction>(refinedText)
        } catch (e: SerializationException) {
            logger.warn(tag, "Unreadable extraction JSON", e)
            return failure(AiFailureReason.INVALID_RESPONSE)
        }
        if (!extraction.isInvoice) return failure(AiFailureReason.NOT_AN_INVOICE)

        val amount = extraction.amount?.content?.trim().orEmpty()
        val description = extraction.description?.trim().orEmpty()
        if (amount.isEmpty() && description.isEmpty()) return failure(AiFailureReason.INVALID_RESPONSE)

        val category = BudgetEntry.Category.entries
            .firstOrNull { it.name.equals(extraction.category?.trim(), ignoreCase = true) }
            ?: BudgetEntry.Category.OTHER

        val defaults = BudgetEntry()
        return AiExtractionResult.Success(
            defaults.copy(
                amount = amount,
                description = description,
                category = category,
                date = extraction.date?.takeIf { ISO_DATE.matches(it.trim()) }?.trim() ?: defaults.date
            )
        )
    }

    private fun failure(reason: AiFailureReason) = AiExtractionResult.Failure(reason)

    private companion object {
        const val MAX_ATTEMPTS = 2
        const val DEFAULT_TOTAL_TIMEOUT_MS = 30_000L
        const val DEFAULT_RETRY_DELAY_MS = 1_000L
        val BLOCKED_FINISH_REASONS = setOf("SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII", "RECITATION")
        const val ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}""")

        val EXTRACTION_SCHEMA = GeminiSchema(
            type = "OBJECT",
            properties = mapOf(
                "isInvoice" to GeminiSchema(
                    type = "BOOLEAN",
                    description = "true only if the image is a receipt, invoice or bill"
                ),
                "amount" to GeminiSchema(
                    type = "NUMBER",
                    description = "Final total paid as a plain number: dot as decimal separator, no thousands separators, no currency symbol"
                ),
                "description" to GeminiSchema(
                    type = "STRING",
                    description = "Short description of what was paid"
                ),
                "category" to GeminiSchema(
                    type = "STRING",
                    enum = BudgetEntry.Category.entries.map { it.name }
                ),
                "date" to GeminiSchema(
                    type = "STRING",
                    description = "Date of the purchase in YYYY-MM-DD format"
                )
            ),
            required = listOf("isInvoice")
        )
    }
}
