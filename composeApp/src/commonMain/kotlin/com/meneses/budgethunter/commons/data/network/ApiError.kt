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
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.TimeoutCancellationException
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
        is ClientRequestException -> {
            when (response.status) {
                HttpStatusCode.Unauthorized -> ApiError.Unauthorized
                HttpStatusCode.Forbidden -> ApiError.Forbidden
                HttpStatusCode.NotFound -> ApiError.NotFound
                HttpStatusCode.Conflict -> ApiError.Conflict
                else -> ApiError.Unknown
            }
        }

        // HTTP 500+ - Server errors
        is ServerResponseException -> {
            ApiError.ServerError
        }

        // Other HTTP errors
        is ResponseException -> {
            ApiError.Unknown
        }

        // Request timeout
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

/**
 * Wraps a network call with proper error handling
 * Converts technical exceptions to user-friendly ApiErrors
 */
suspend fun <T> safeApiCall(
    call: suspend () -> T
): Result<T> {
    return try {
        Result.success(call())
    } catch (e: Exception) {
        Result.failure(e.toApiError())
    }
}