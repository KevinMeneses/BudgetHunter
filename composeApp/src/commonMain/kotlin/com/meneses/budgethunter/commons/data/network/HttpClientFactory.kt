package com.meneses.budgethunter.commons.data.network

import com.meneses.budgethunter.auth.data.TokenStorage
import com.meneses.budgethunter.commons.data.network.models.AuthResponse
import com.meneses.budgethunter.commons.data.network.models.RefreshTokenRequest
import com.meneses.budgethunter.commons.data.sync.Logger as AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngineFactory
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
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

private const val TAG = "HttpClient"

/**
 * Creates the client for the BudgetHunter API, including the SSE streams.
 *
 * @param engine Given by each platform on purpose, instead of letting Ktor pick whichever engine
 *   it finds first on the classpath: on Android that was the `Android` (HttpURLConnection)
 *   engine, which never closes the socket when a request is cancelled, so every SSE stream
 *   left behind (e.g. when switching budgets) stayed open until the network dropped.
 * @param onSessionExpired Called when the server rejects the refresh token, after the tokens are
 *   cleared. Not called for network or server errors, which keep the session.
 */
fun createHttpClient(
    engine: HttpClientEngineFactory<*>,
    baseUrl: String,
    tokenStorage: TokenStorage,
    json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    },
    appLogger: AppLogger,
    logLevel: LogLevel,
    onSessionExpired: () -> Unit = {}
): HttpClient {
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            logger = Logger.SIMPLE
            level = logLevel
            // A receipt scan carries the whole photo or PDF (megabytes of Base64): never dump it to the log
            filter { request -> !request.url.encodedPath.endsWith(ApiEndpoints.RECEIPTS_EXTRACT) }
        }

        // Install SSE plugin for Server-Sent Events support
        install(SSE)

        // Required so individual requests can override the engine's timeouts. Long-lived SSE
        // streams set their own; see SseClient.configureStreamRequest.
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
                        // A session without its refresh token cannot be renewed: send the user to
                        // sign in instead of leaving them inside the app with nothing syncing.
                        // With no tokens at all there was no session (offline mode): nothing to say.
                        val hadSession = tokenStorage.getAuthToken() != null
                        tokenStorage.clearTokens()
                        if (hadSession) onSessionExpired()
                        return@refreshTokens null
                    }

                    // Create a simple HTTP client for the refresh call (without auth to avoid recursion).
                    // Same engine as the main client, not whichever one Ktor finds first.
                    val refreshClient = HttpClient(engine) {
                        install(ContentNegotiation) {
                            json(json)
                        }
                        defaultRequest {
                            url(baseUrl)
                            contentType(ContentType.Application.Json)
                        }
                    }
                    try {
                        val response = refreshClient.post(ApiEndpoints.REFRESH_TOKEN) {
                            setBody(RefreshTokenRequest(currentRefreshToken))
                        }

                        when {
                            response.status.isSuccess() -> {
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
                            }
                            isRefreshRejected(response.status) -> {
                                // The server says this session is over: the refresh token expired
                                // or a sign in elsewhere replaced it. Only now are the tokens dropped.
                                appLogger.warn(TAG, "Session rejected by the server (${response.status.value})")
                                tokenStorage.clearTokens()
                                onSessionExpired()
                                null
                            }
                            else -> {
                                // A server error says nothing about the session: keep it and try
                                // again on the next 401.
                                appLogger.warn(TAG, "Token refresh failed with ${response.status.value}, keeping the session")
                                null
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // No connection, a timeout or an unreadable answer. Dropping the tokens here
                        // signed the user out on any network hiccup, silently, and every change
                        // after that stayed on the device.
                        // One case still ends the session: the server rotated the token but its
                        // answer was lost, so the next refresh sends a token it no longer knows.
                        appLogger.warn(TAG, "Token refresh failed, keeping the session", e)
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

/**
 * Whether the refresh endpoint refused the token itself, as opposed to failing for a reason that
 * says nothing about the session. Only 401: the backend answers it for an unknown, replaced or
 * expired refresh token, while a 400 or 403 may come from a proxy or a captive portal.
 */
internal fun isRefreshRejected(status: HttpStatusCode): Boolean =
    status == HttpStatusCode.Unauthorized
