package com.meneses.budgethunter.billing.data

import com.meneses.budgethunter.commons.data.network.ApiEndpoints
import com.meneses.budgethunter.commons.data.network.models.AiUsageResponse
import com.meneses.budgethunter.commons.data.network.models.BillingStatusResponse
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/** The backend's billing endpoints. Every failure is a [BillingApiException]. */
class BillingApiService(
    private val httpClient: HttpClient,
    private val json: Json
) {
    /** The account's plan and its RevenueCat id. */
    suspend fun status(): BillingStatusResponse = call { httpClient.get(ApiEndpoints.BILLING_STATUS) }

    /** Makes the server re-read the plan from RevenueCat now (after a purchase or restore). */
    suspend fun sync(): BillingStatusResponse = call { httpClient.post(ApiEndpoints.BILLING_SYNC) }

    /** This month's receipt scans. */
    suspend fun receiptUsage(): AiUsageResponse = call { httpClient.get(ApiEndpoints.RECEIPTS_USAGE) }

    private suspend inline fun <reified T> call(request: suspend () -> HttpResponse): T {
        val response = try {
            request()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ResponseException) {
            // Only reached when the client is configured with expectSuccess
            throw BillingApiException(e.response.status.value, "HTTP ${e.response.status.value}", e)
        } catch (e: Exception) {
            throw BillingApiException(null, "Billing request failed", e)
        }
        val status = response.status.value
        if (status !in 200..299) throw BillingApiException(status, "HTTP $status")
        return try {
            json.decodeFromString<T>(response.bodyAsText())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw BillingApiException(status, "Unreadable billing response", e)
        }
    }
}

/** @property status the HTTP status, or null when the server was not reached */
class BillingApiException(val status: Int?, message: String, cause: Throwable? = null) : Exception(message, cause)
