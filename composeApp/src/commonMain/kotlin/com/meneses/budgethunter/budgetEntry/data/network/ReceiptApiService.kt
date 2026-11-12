package com.meneses.budgethunter.budgetEntry.data.network

import com.meneses.budgethunter.budgetEntry.domain.AiExtractionResult
import com.meneses.budgethunter.budgetEntry.domain.AiFailureReason
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.data.network.ApiEndpoints
import com.meneses.budgethunter.commons.data.network.models.AiUsageResponse
import com.meneses.budgethunter.commons.data.network.models.ReceiptErrorResponse
import com.meneses.budgethunter.commons.data.network.models.ReceiptExtractionRequest
import com.meneses.budgethunter.commons.data.network.models.ReceiptExtractionResponse
import com.meneses.budgethunter.commons.data.sync.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/**
 * Reads receipts through the BudgetHunter backend (`POST /api/receipts/extract`), which holds the AI key and
 * counts each scan against the account's plan. The app never talks to the AI provider itself.
 *
 * Every outcome maps to an [AiExtractionResult] so the caller can tell the user what happened. The server
 * already retries the provider, so nothing is retried here.
 */
class ReceiptApiService(
    private val httpClient: HttpClient,
    private val json: Json,
    private val logger: Logger,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS
) {
    private val tag = "ReceiptApiService"

    /**
     * @param base64Data Base64-encoded file content
     * @param mimeType MIME type of the file, e.g. "image/jpeg" or "application/pdf"
     * @param today The user's local date, for documents without one
     */
    suspend fun extract(base64Data: String, mimeType: String, today: LocalDate): AiExtractionResult {
        return try {
            val response = httpClient.post(ApiEndpoints.RECEIPTS_EXTRACT) {
                // Uploading a photo and waiting for the AI takes longer than any other request
                timeout {
                    requestTimeoutMillis = timeoutMillis
                    socketTimeoutMillis = timeoutMillis
                }
                contentType(ContentType.Application.Json)
                setBody(ReceiptExtractionRequest(base64Data, mimeType, today.toString()))
            }
            val body = response.bodyAsText()
            when (val status = response.status.value) {
                in 200..299 -> parseSuccess(body)
                else -> AiExtractionResult.Failure(failureFor(status, body))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ResponseException) {
            // Only reached when the client is configured with expectSuccess
            AiExtractionResult.Failure(failureFor(e.response.status.value, e.response.bodyAsText()))
        } catch (e: HttpRequestTimeoutException) {
            logger.warn(tag, "Receipt request timed out", e)
            AiExtractionResult.Failure(AiFailureReason.TIMEOUT)
        } catch (e: Exception) {
            logger.warn(tag, "Receipt request failed", e)
            AiExtractionResult.Failure(AiFailureReason.NETWORK)
        }
    }

    /**
     * The account's plan and this month's scans (`GET /api/receipts/usage`).
     *
     * @return null when it could not be read: no connection, no session or an unreadable answer.
     */
    suspend fun getUsage(): AiUsageResponse? =
        try {
            val response = httpClient.get(ApiEndpoints.RECEIPTS_USAGE)
            if (response.status.isSuccess()) {
                json.decodeFromString<AiUsageResponse>(response.bodyAsText())
            } else {
                logger.warn(tag, "Usage not read: HTTP ${response.status.value}")
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(tag, "Usage request failed", e)
            null
        }

    private fun parseSuccess(body: String): AiExtractionResult {
        val response = try {
            json.decodeFromString<ReceiptExtractionResponse>(body)
        } catch (e: SerializationException) {
            logger.warn(tag, "Unreadable receipt response", e)
            return AiExtractionResult.Failure(AiFailureReason.INVALID_RESPONSE)
        } catch (e: IllegalArgumentException) {
            logger.warn(tag, "Unreadable receipt response", e)
            return AiExtractionResult.Failure(AiFailureReason.INVALID_RESPONSE)
        }
        if (!response.isReceipt) return AiExtractionResult.Failure(AiFailureReason.NOT_AN_INVOICE)

        val category = BudgetEntry.getAssignableCategories()
            .firstOrNull { it.name.equals(response.category?.trim(), ignoreCase = true) }
            ?: BudgetEntry.Category.OTHER
        val defaults = BudgetEntry()
        return AiExtractionResult.Success(
            defaults.copy(
                amount = response.amount?.content?.trim().orEmpty(),
                description = response.description?.trim().orEmpty(),
                category = category,
                date = response.date?.trim()?.takeIf { ISO_DATE.matches(it) } ?: defaults.date
            )
        )
    }

    private fun failureFor(status: Int, body: String): AiFailureReason {
        val code = try {
            json.decodeFromString<ReceiptErrorResponse>(body).code
        } catch (_: Exception) {
            ""
        }
        logger.warn(tag, "Receipt not read: HTTP $status $code")
        return when (status) {
            401 -> AiFailureReason.SIGN_IN_REQUIRED
            402 -> AiFailureReason.QUOTA_EXCEEDED
            413, 415 -> AiFailureReason.FILE
            422 -> AiFailureReason.BLOCKED
            // The server's own rate limit, or the AI's quota / the server's per-minute budget
            429 -> AiFailureReason.RATE_LIMITED
            503 -> if (code == "AI_BUSY") AiFailureReason.RATE_LIMITED else AiFailureReason.SERVER
            504 -> AiFailureReason.TIMEOUT
            502 -> AiFailureReason.INVALID_RESPONSE
            in 500..599 -> AiFailureReason.SERVER
            else -> AiFailureReason.REJECTED
        }
    }

    private companion object {
        // The server allows the AI 30s per attempt; uploading a 16 MB PDF on mobile data adds more
        const val DEFAULT_TIMEOUT_MILLIS = 90_000L
        val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}""")
    }
}
