package com.meneses.budgethunter.commons.data.network

import com.meneses.budgethunter.auth.data.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
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
    // Create a custom plugin to add authentication token to each request
    val authPlugin = createClientPlugin("AuthTokenPlugin") {
        onRequest { request, _ ->
            val token = tokenStorage.getAuthToken()
            if (token != null) {
                request.headers.append(HttpHeaders.Authorization, "Bearer $token")
                println("HttpClient: Added Authorization header with token")
            } else {
                println("HttpClient: No token available")
            }
        }
    }

    return HttpClient {
        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }

        // Install our custom auth plugin
        install(authPlugin)

        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }
    }
}
