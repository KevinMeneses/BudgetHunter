package com.meneses.budgethunter.commons.data.network

import com.meneses.budgethunter.auth.data.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
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
): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(json)
    }

    install(Auth) {
        bearer {
            loadTokens {
                val authToken = tokenStorage.getAuthToken()
                authToken?.let {
                    BearerTokens(accessToken = it, refreshToken = it)
                }
            }

            refreshTokens {
                // Token refresh logic will be implemented in AuthRepository
                // For now, return null to let the auth flow handle it
                null
            }
        }
    }

    defaultRequest {
        url(baseUrl)
        contentType(ContentType.Application.Json)
    }
}
