package com.meneses.budgethunter.commons.data.network

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.error_conflict
import budgethunter.composeapp.generated.resources.error_forbidden
import budgethunter.composeapp.generated.resources.error_not_found
import budgethunter.composeapp.generated.resources.error_parse
import budgethunter.composeapp.generated.resources.error_server
import budgethunter.composeapp.generated.resources.error_timeout
import budgethunter.composeapp.generated.resources.error_unauthorized
import budgethunter.composeapp.generated.resources.error_unknown
import budgethunter.composeapp.generated.resources.sharing_limit_generic
import com.meneses.budgethunter.billing.domain.SharingLimitType
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException
import org.jetbrains.compose.resources.StringResource

/**
 * Sealed class representing different types of API errors
 * Each error type holds a reference to a localized string resource
 */
sealed class ApiError(val messageResource: StringResource) : Exception() {

    /**
     * Authentication errors (401)
     */
    data object Unauthorized : ApiError(Res.string.error_unauthorized)

    /**
     * Rejected request (400). The server also answers it for an entry that no longer exists.
     */
    data object BadRequest : ApiError(Res.string.error_unknown)

    /**
     * Permission errors (403)
     */
    data object Forbidden : ApiError(Res.string.error_forbidden)

    /**
     * Resource not found (404)
     */
    data object NotFound : ApiError(Res.string.error_not_found)

    /**
     * Conflict errors (409) - typically when data is out of sync
     */
    data object Conflict : ApiError(Res.string.error_conflict)

    /**
     * Adding a member would take the budget's owner past their plan's sharing limit (402 SHARING_LIMIT_REACHED).
     * Only the owner's upgrade lifts it, so Premium is offered only when [callerIsOwner].
     *
     * @property limitType null when the server names a limit this app does not know
     */
    data class SharingLimitReached(
        val limitType: SharingLimitType?,
        val callerIsOwner: Boolean
    ) : ApiError(Res.string.sharing_limit_generic)

    /**
     * Server errors (500+)
     */
    data object ServerError : ApiError(Res.string.error_server)

    /**
     * Request timeout
     */
    data object Timeout : ApiError(Res.string.error_timeout)

    /**
     * Data parsing/serialization errors
     */
    data object ParseError : ApiError(Res.string.error_parse)

    /**
     * Unknown/unexpected errors
     */
    data object Unknown : ApiError(Res.string.error_unknown)
}

/**
 * Extension function to convert any exception to a user-friendly ApiError
 */
fun Throwable.toApiError(): ApiError {
    return when (this) {
        // HTTP 4xx - Client errors
        is ClientRequestException -> response.status.toApiError()

        // HTTP 500+ - Server errors
        is ServerResponseException -> {
            ApiError.ServerError
        }

        // Other HTTP errors
        is ResponseException -> {
            ApiError.Unknown
        }

        // Request timeout: Ktor's own (HttpTimeout, or the engine's, e.g. iOS's NSURLErrorTimedOut) or a coroutine's
        is HttpRequestTimeoutException,
        is SocketTimeoutException,
        is ConnectTimeoutException,
        is TimeoutCancellationException -> {
            ApiError.Timeout
        }

        // JSON parsing errors
        is SerializationException,
        is JsonConvertException -> {
            ApiError.ParseError
        }

        // Already an ApiError, return as-is
        is ApiError -> this

        // Unknown errors
        else -> ApiError.Unknown
    }
}

/** The [ApiError] for an HTTP status that is not a success. */
fun HttpStatusCode.toApiError(): ApiError = when {
    this == HttpStatusCode.BadRequest -> ApiError.BadRequest
    this == HttpStatusCode.Unauthorized -> ApiError.Unauthorized
    this == HttpStatusCode.Forbidden -> ApiError.Forbidden
    this == HttpStatusCode.NotFound -> ApiError.NotFound
    this == HttpStatusCode.Conflict -> ApiError.Conflict
    value >= 500 -> ApiError.ServerError
    else -> ApiError.Unknown
}

/**
 * The body of a successful answer; any other status throws its [ApiError]. The app's client does not set
 * `expectSuccess`, so a plain `body()` reads a refusal as if it were the answer: an error body fails as
 * [ApiError.ParseError], and `body<Unit>()` (deletions) takes a 404 or a 500 as done.
 */
suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
    if (!status.isSuccess()) throw status.toApiError()
    return body()
}

/**
 * Wraps a network call with proper error handling
 * Converts technical exceptions to user-friendly ApiErrors
 */
suspend fun <T> safeApiCall(
    call: suspend () -> T
): Result<T> {
    return try {
        Result.success(call())
    } catch (e: TimeoutCancellationException) {
        Result.failure(ApiError.Timeout)
    } catch (e: CancellationException) {
        // The caller's scope was cancelled: let it end instead of handing back a failure
        throw e
    } catch (e: Exception) {
        Result.failure(e.toApiError())
    }
}

/**
 * True when the server answered that the thing a request was about is gone for this account: no
 * longer exists (404, or 400 for an entry, see the backend's BudgetService) or is no longer
 * reachable (403: the budget was deleted, or the user removed from it). Retrying cannot change it.
 */
fun Throwable.isGoneFromServer(): Boolean =
    this == ApiError.NotFound || this == ApiError.BadRequest || this == ApiError.Forbidden
