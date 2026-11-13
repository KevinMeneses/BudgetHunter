package com.meneses.budgethunter.commons.data.network

import com.meneses.budgethunter.auth.data.TokenStorage
import com.meneses.budgethunter.commons.data.sync.Logger as AppLogger
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** The HTTP log, even at its most verbose (debug builds), must never print the access token. */
class HttpLoggingTest {

    private val logged = StringBuilder()

    @Test
    fun `the full log masks the authorization header`() = runTest {
        val client = createHttpClient(
            engine = object : HttpClientEngineFactory<MockEngineConfig> {
                override fun create(block: MockEngineConfig.() -> Unit): HttpClientEngine =
                    MockEngine(
                        MockEngineConfig().apply {
                            addHandler {
                                respond("[]", headers = headersOf(HttpHeaders.ContentType, "application/json"))
                            }
                        }.apply(block)
                    )
            },
            baseUrl = "https://api.test",
            tokenStorage = StaticTokenStorage,
            appLogger = SilentLogger,
            logLevel = LogLevel.ALL,
            httpLogger = object : Logger {
                override fun log(message: String) {
                    logged.appendLine(message)
                }
            }
        )

        client.get(ApiEndpoints.BUDGETS)

        val log = logged.toString()
        assertTrue(log.contains(HttpHeaders.Authorization), "the header is still listed:\n$log")
        assertFalse(log.contains(SECRET_TOKEN), "the token leaked to the log:\n$log")
    }

    private object StaticTokenStorage : TokenStorage {
        override suspend fun saveAuthToken(token: String) = Unit
        override suspend fun getAuthToken(): String = SECRET_TOKEN
        override suspend fun saveRefreshToken(token: String) = Unit
        override suspend fun getRefreshToken(): String = "secret-refresh"
        override suspend fun clearTokens() = Unit
    }

    private object SilentLogger : AppLogger {
        override fun debug(tag: String, message: String) = Unit
        override fun info(tag: String, message: String) = Unit
        override fun warn(tag: String, message: String, error: Throwable?) = Unit
        override fun error(tag: String, message: String, error: Throwable?) = Unit
    }

    private companion object {
        const val SECRET_TOKEN = "secret-access-token"
    }
}
