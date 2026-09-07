package com.meneses.budgethunter.commons.data.network

import com.meneses.budgethunter.auth.data.TokenStorage
import com.meneses.budgethunter.commons.data.network.models.AuthResponse
import com.meneses.budgethunter.commons.data.network.models.RefreshTokenRequest
import com.meneses.budgethunter.commons.data.sync.Logger as AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val TAG = "HttpClient"

fun createHttpClient(
    baseUrl: String,
    tokenStorage: TokenStorage,
    json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    },
    appLogger: AppLogger,
    logLevel: LogLevel
): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            logger = Logger.SIMPLE
            level = logLevel
        }

        // Install SSE plugin for Server-Sent Events support
        install(SSE)

        // Required so individual requests can override the engine's timeouts. Long-lived SSE
        // streams disable the socket timeout entirely; see SseClient.subscribeToBudgetEntries.
        // Installing it without values keeps the engine defaults for every other request.
        install(HttpTimeout)

        // Install Auth plugin with Bearer token and automatic refresh
        install(Auth) {
            bearer {
                loadTokens {
                    // Load current tokens from storage
                    val authToken = tokenStorage.getAuthToken()
                    val refreshToken = tokenStorage.getRefreshToken()

                    if (authToken != null && refreshToken != null) {
                        appLogger.debug(TAG, "Loaded tokens from storage")
                        BearerTokens(
                            accessToken = authToken,
                            refreshToken = refreshToken
                        )
                    } else {
                        appLogger.debug(TAG, "No tokens available in storage")
                        null
                    }
                }

                refreshTokens {
                    // This is called when a 401 response is received
                    appLogger.debug(TAG, "Refreshing tokens due to 401 response")

                    val currentRefreshToken = tokenStorage.getRefreshToken()
                    if (currentRefreshToken == null) {
                        appLogger.warn(TAG, "No refresh token available, cannot refresh")
                        // Clear tokens and force re-login
                        tokenStorage.clearTokens()
                        return@refreshTokens null
                    }

                    // Create a simple HTTP client for the refresh call (without auth to avoid recursion)
                    val refreshClient = HttpClient {
                        install(ContentNegotiation) {
                            json(json)
                        }
                        defaultRequest {
                            url(baseUrl)
                            contentType(ContentType.Application.Json)
                        }
                    }
                    try {
                        // Make the refresh token request
                        val response = refreshClient.post(ApiEndpoints.REFRESH_TOKEN) {
                            setBody(RefreshTokenRequest(currentRefreshToken))
                        }

                        val authResponse = response.body<AuthResponse>()

                        // Store new tokens (token rotation)
                        tokenStorage.saveAuthToken(authResponse.authToken)
                        tokenStorage.saveRefreshToken(authResponse.refreshToken)

                        appLogger.debug(TAG, "Token refresh successful")

                        // Return new tokens to retry the original request
                        BearerTokens(
                            accessToken = authResponse.authToken,
                            refreshToken = authResponse.refreshToken
                        )
                    } catch (e: Exception) {
                        appLogger.warn(TAG, "Token refresh failed", e)
                        // Clear tokens on refresh failure to force re-login
                        tokenStorage.clearTokens()
                        null
                    } finally {
                        refreshClient.close()
                    }
                }

                sendWithoutRequest { request ->
                    // Send tokens with all requests except auth endpoints.
                    // SIGN_IN_WITH_GOOGLE is listed on its own even though the SIGN_IN check
                    // already matches it as a substring today: that coupling is invisible and
                    // would break the moment either constant is reworded.
                    val path = request.url.toString()
                    !path.contains(ApiEndpoints.SIGN_IN) &&
                        !path.contains(ApiEndpoints.SIGN_IN_WITH_GOOGLE) &&
                        !path.contains(ApiEndpoints.SIGN_UP) &&
                        !path.contains(ApiEndpoints.REFRESH_TOKEN)
                }
            }
        }

        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }
    }
}
