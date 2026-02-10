package com.meneses.budgethunter.commons.data.network

import com.meneses.budgethunter.auth.data.TokenStorage
import com.meneses.budgethunter.commons.data.network.models.AuthResponse
import com.meneses.budgethunter.commons.data.network.models.RefreshTokenRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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

fun createHttpClient(
    baseUrl: String,
    tokenStorage: TokenStorage,
    json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }

        // Install SSE plugin for Server-Sent Events support
        install(SSE)

        // Install Auth plugin with Bearer token and automatic refresh
        install(Auth) {
            bearer {
                loadTokens {
                    // Load current tokens from storage
                    val authToken = tokenStorage.getAuthToken()
                    val refreshToken = tokenStorage.getRefreshToken()

                    if (authToken != null && refreshToken != null) {
                        println("HttpClient: Loaded tokens from storage")
                        BearerTokens(
                            accessToken = authToken,
                            refreshToken = refreshToken
                        )
                    } else {
                        println("HttpClient: No tokens available in storage")
                        null
                    }
                }

                refreshTokens {
                    // This is called when a 401 response is received
                    println("HttpClient: Refreshing tokens due to 401 response")

                    val currentRefreshToken = tokenStorage.getRefreshToken()
                    if (currentRefreshToken == null) {
                        println("HttpClient: No refresh token available, cannot refresh")
                        // Clear tokens and force re-login
                        tokenStorage.clearTokens()
                        return@refreshTokens null
                    }

                    try {
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

                        // Make the refresh token request
                        val response = refreshClient.post("/api/users/refresh_token") {
                            setBody(RefreshTokenRequest(currentRefreshToken))
                        }

                        val authResponse = response.body<AuthResponse>()

                        // Store new tokens (token rotation)
                        tokenStorage.saveAuthToken(authResponse.authToken)
                        tokenStorage.saveRefreshToken(authResponse.refreshToken)

                        println("HttpClient: Token refresh successful")

                        // Close the temporary client
                        refreshClient.close()

                        // Return new tokens to retry the original request
                        BearerTokens(
                            accessToken = authResponse.authToken,
                            refreshToken = authResponse.refreshToken
                        )
                    } catch (e: Exception) {
                        println("HttpClient: Token refresh failed: ${e.message}")
                        // Clear tokens on refresh failure to force re-login
                        tokenStorage.clearTokens()
                        null
                    }
                }

                sendWithoutRequest { request ->
                    // Send tokens with all requests except auth endpoints
                    val path = request.url.toString()
                    !path.contains("/api/users/sign_in") &&
                    !path.contains("/api/users/sign_up") &&
                    !path.contains("/api/users/refresh_token")
                }
            }
        }

        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }
    }
}
