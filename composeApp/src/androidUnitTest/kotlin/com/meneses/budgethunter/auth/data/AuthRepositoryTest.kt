package com.meneses.budgethunter.auth.data

import com.meneses.budgethunter.commons.data.network.ApiEndpoints
import com.meneses.budgethunter.commons.data.sync.NoOpLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for AuthRepository.
 * Tests authentication flows including sign up, sign in, token refresh, and sign out.
 */
class AuthRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun createTestTokenStorage(): TokenStorage = FakeTokenStorage()

    private class FakeTokenStorage : TokenStorage {
        private var authToken: String? = null
        private var refreshToken: String? = null

        override suspend fun saveAuthToken(token: String) { authToken = token }
        override suspend fun getAuthToken(): String? = authToken
        override suspend fun saveRefreshToken(token: String) { refreshToken = token }
        override suspend fun getRefreshToken(): String? = refreshToken
        override suspend fun clearTokens() { authToken = null; refreshToken = null }
    }

    /**
     * Regression test: Ktor's BearerAuthProvider caches the result of `loadTokens` in memory and
     * only reloads it after a 401. Signing in as a different user while the previous token is
     * still valid used to keep sending the previous user's token, so the server answered with the
     * previous user's data.
     */
    @Test
    fun `signIn drops the cached bearer token so later requests use the new session`() = runTest {
        // Arrange: storage already holds a previous, still-valid session
        val tokenStorage = createTestTokenStorage()
        tokenStorage.saveAuthToken("token-user-a")
        tokenStorage.saveRefreshToken("refresh-user-a")

        val sentAuthorizationHeaders = mutableListOf<String?>()

        val mockEngine = MockEngine { request ->
            sentAuthorizationHeaders += request.headers[HttpHeaders.Authorization]
            val body = if (request.url.encodedPath == ApiEndpoints.SIGN_IN) {
                """
                {
                  "authToken": "token-user-b",
                  "refreshToken": "refresh-user-b",
                  "email": "b@b.com",
                  "name": "b"
                }
                """
            } else {
                "[]"
            }
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@AuthRepositoryTest.json)
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        val authToken = tokenStorage.getAuthToken()
                        val refreshToken = tokenStorage.getRefreshToken()
                        if (authToken != null && refreshToken != null) {
                            BearerTokens(authToken, refreshToken)
                        } else {
                            null
                        }
                    }
                    sendWithoutRequest { !it.url.toString().contains(ApiEndpoints.SIGN_IN) }
                }
            }
            defaultRequest {
                url("http://localhost:8080")
                contentType(ContentType.Application.Json)
            }
        }

        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined, NoOpLogger())

        // Warm up the provider cache with user A's token
        httpClient.get(ApiEndpoints.BUDGETS)
        assertEquals("Bearer token-user-a", sentAuthorizationHeaders.last())

        // Act: sign in as user B and hit an authenticated endpoint again
        val result = repository.signIn(email = "b@b.com", password = "password123")
        httpClient.get(ApiEndpoints.BUDGETS)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals("Bearer token-user-b", sentAuthorizationHeaders.last())
    }

    /**
     * Test successful user sign up
     */
    @Test
    fun `signUp returns success for valid credentials`() = runTest {
        // Arrange
        val mockResponse = """
        {
          "email": "test@example.com",
          "name": "Test User"
        }
        """

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@AuthRepositoryTest.json)
            }
            defaultRequest {
                url("http://localhost:8080")
                contentType(ContentType.Application.Json)
            }
        }

        val tokenStorage = createTestTokenStorage()
        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined, NoOpLogger())

        // Act
        val result = repository.signUp(
            email = "test@example.com",
            name = "Test User",
            password = "password123"
        )

        // Assert
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals("test@example.com", response.email)
        assertEquals("Test User", response.name)
    }

    /**
     * Test sign up failure with HTTP error
     */
    @Test
    fun `signUp returns failure for HTTP error`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"error": "Email already exists"}""",
                status = HttpStatusCode.Conflict,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@AuthRepositoryTest.json)
            }
            defaultRequest {
                url("http://localhost:8080")
                contentType(ContentType.Application.Json)
            }
        }

        val tokenStorage = createTestTokenStorage()
        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined, NoOpLogger())

        // Act
        val result = repository.signUp(
            email = "existing@example.com",
            name = "Test User",
            password = "password123"
        )

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is EmailAlreadyRegisteredException)
    }

    private suspend fun signUpAnswering(status: HttpStatusCode, body: String): Result<*> {
        val mockEngine = MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@AuthRepositoryTest.json)
            }
            defaultRequest {
                url("http://localhost:8080")
                contentType(ContentType.Application.Json)
            }
        }
        val repository = AuthRepository(httpClient, createTestTokenStorage(), Dispatchers.Unconfined, NoOpLogger())
        return repository.signUp("a@example.com", "Test User", "password123")
    }

    @Test
    fun `signUp 400 with the email exists message returns EmailAlreadyRegisteredException`() = runTest {
        val result = signUpAnswering(
            HttpStatusCode.BadRequest,
            """{"status":400,"message":"Email already exists"}"""
        )

        assertTrue(result.exceptionOrNull() is EmailAlreadyRegisteredException)
    }

    @Test
    fun `signUp 400 with another message is not EmailAlreadyRegisteredException`() = runTest {
        val result = signUpAnswering(
            HttpStatusCode.BadRequest,
            """{"status":400,"message":"Password too short"}"""
        )

        assertTrue(result.isFailure)
        assertFalse(result.exceptionOrNull() is EmailAlreadyRegisteredException)
    }

    @Test
    fun `signUp 500 is not EmailAlreadyRegisteredException`() = runTest {
        val result = signUpAnswering(HttpStatusCode.InternalServerError, """{"message":"boom"}""")

        assertTrue(result.isFailure)
        assertFalse(result.exceptionOrNull() is EmailAlreadyRegisteredException)
    }

    /**
     * Test successful sign in with token storage
     */
    @Test
    fun `signIn returns success and stores tokens`() = runTest {
        // Arrange
        val mockResponse = """
        {
          "authToken": "auth-token-123",
          "refreshToken": "refresh-token-456",
          "email": "test@example.com",
          "name": "Test User"
        }
        """

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@AuthRepositoryTest.json)
            }
            defaultRequest {
                url("http://localhost:8080")
                contentType(ContentType.Application.Json)
            }
        }

        val tokenStorage = createTestTokenStorage()
        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined, NoOpLogger())

        // Act
        val result = repository.signIn(
            email = "test@example.com",
            password = "password123"
        )

        // Assert
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals("auth-token-123", response.authToken)
        assertEquals("refresh-token-456", response.refreshToken)
        assertEquals("test@example.com", response.email)
        assertEquals("Test User", response.name)

        // Verify tokens are stored
        assertEquals("auth-token-123", tokenStorage.getAuthToken())
        assertEquals("refresh-token-456", tokenStorage.getRefreshToken())
    }

    /**
     * Test sign in failure with invalid credentials
     */
    @Test
    fun `signIn returns failure for invalid credentials`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"error": "Invalid credentials"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@AuthRepositoryTest.json)
            }
            defaultRequest {
                url("http://localhost:8080")
                contentType(ContentType.Application.Json)
            }
        }

        val tokenStorage = createTestTokenStorage()
        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined, NoOpLogger())

        // Act
        val result = repository.signIn(
            email = "test@example.com",
            password = "wrong-password"
        )

        // Assert
        assertTrue(result.isFailure)
        // Verify tokens are not stored
        assertNull(tokenStorage.getAuthToken())
        assertNull(tokenStorage.getRefreshToken())
    }

    /**
     * Test successful token refresh
     */
    @Test
    fun `refreshToken returns success and updates tokens`() = runTest {
        // Arrange
        val mockResponse = """
        {
          "authToken": "new-auth-token-789",
          "refreshToken": "new-refresh-token-012",
          "email": "test@example.com",
          "name": "Test User"
        }
        """

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@AuthRepositoryTest.json)
            }
            defaultRequest {
                url("http://localhost:8080")
                contentType(ContentType.Application.Json)
            }
        }

        val tokenStorage = createTestTokenStorage()
        // Set initial refresh token
        tokenStorage.saveRefreshToken("old-refresh-token-456")

        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined, NoOpLogger())

        // Act
        val result = repository.refreshToken()

        // Assert
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals("new-auth-token-789", response.authToken)
        assertEquals("new-refresh-token-012", response.refreshToken)

        // Verify new tokens are stored (token rotation)
        assertEquals("new-auth-token-789", tokenStorage.getAuthToken())
        assertEquals("new-refresh-token-012", tokenStorage.getRefreshToken())
    }

    /**
     * Test token refresh failure when no refresh token available
     */
    @Test
    fun `refreshToken returns failure when no refresh token available`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            throw AssertionError("HTTP request should not be made")
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@AuthRepositoryTest.json)
            }
            defaultRequest {
                url("http://localhost:8080")
                contentType(ContentType.Application.Json)
            }
        }

        val tokenStorage = createTestTokenStorage()
        // No refresh token stored

        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined, NoOpLogger())

        // Act
        val result = repository.refreshToken()

        // Assert
        assertTrue(result.isFailure)
        assertEquals("No refresh token available", result.exceptionOrNull()?.message)
    }

    /**
     * Test token refresh failure with HTTP error
     */
    @Test
    fun `refreshToken returns failure for HTTP error`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"error": "Invalid refresh token"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@AuthRepositoryTest.json)
            }
            defaultRequest {
                url("http://localhost:8080")
                contentType(ContentType.Application.Json)
            }
        }

        val tokenStorage = createTestTokenStorage()
        tokenStorage.saveRefreshToken("invalid-refresh-token")

        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined, NoOpLogger())

        // Act
        val result = repository.refreshToken()

        // Assert
        assertTrue(result.isFailure)
    }

    /**
     * Test sign out clears tokens
     */
    @Test
    fun `signOut clears all tokens`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            throw AssertionError("HTTP request should not be made during sign out")
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@AuthRepositoryTest.json)
            }
            defaultRequest {
                url("http://localhost:8080")
                contentType(ContentType.Application.Json)
            }
        }

        val tokenStorage = createTestTokenStorage()
        tokenStorage.saveAuthToken("auth-token-123")
        tokenStorage.saveRefreshToken("refresh-token-456")

        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined, NoOpLogger())

        // Act
        repository.signOut()

        // Assert
        assertNull(tokenStorage.getAuthToken())
        assertNull(tokenStorage.getRefreshToken())
    }

    /**
     * Test isAuthenticated returns true when auth token exists
     */
    @Test
    fun `isAuthenticated returns true when auth token exists`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            throw AssertionError("HTTP request should not be made")
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@AuthRepositoryTest.json)
            }
            defaultRequest {
                url("http://localhost:8080")
                contentType(ContentType.Application.Json)
            }
        }

        val tokenStorage = createTestTokenStorage()
        tokenStorage.saveAuthToken("auth-token-123")

        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined, NoOpLogger())

        // Act
        val isAuthenticated = repository.isAuthenticated()

        // Assert
        assertTrue(isAuthenticated)
    }

    /**
     * Test isAuthenticated returns false when no auth token exists
     */
    @Test
    fun `isAuthenticated returns false when no auth token exists`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            throw AssertionError("HTTP request should not be made")
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(this@AuthRepositoryTest.json)
            }
            defaultRequest {
                url("http://localhost:8080")
                contentType(ContentType.Application.Json)
            }
        }

        val tokenStorage = createTestTokenStorage()
        // No auth token stored

        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined, NoOpLogger())

        // Act
        val isAuthenticated = repository.isAuthenticated()

        // Assert
        assertFalse(isAuthenticated)
    }

    // ========== Google sign in ==========

    private fun simpleClient(mockEngine: MockEngine) = HttpClient(mockEngine) {
        install(ContentNegotiation) {
            json(this@AuthRepositoryTest.json)
        }
        defaultRequest {
            url("http://localhost:8080")
            contentType(ContentType.Application.Json)
        }
    }

    @Test
    fun `signInWithGoogle posts the id token and stores the session`() = runTest {
        // Arrange
        val tokenStorage = createTestTokenStorage()
        val requestedPaths = mutableListOf<String>()
        var sentBody: String? = null

        val mockEngine = MockEngine { request ->
            requestedPaths += request.url.encodedPath
            sentBody = (request.body as io.ktor.http.content.TextContent).text
            respond(
                content = """
                {
                  "authToken": "google-auth-token",
                  "refreshToken": "google-refresh-token",
                  "email": "google@example.com",
                  "name": "Google User"
                }
                """,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val repository = AuthRepository(
            simpleClient(mockEngine),
            tokenStorage,
            Dispatchers.Unconfined,
            NoOpLogger()
        )

        // Act
        val result = repository.signInWithGoogle("an-id-token")

        // Assert
        assertTrue(result.isSuccess)
        assertEquals("google@example.com", result.getOrNull()?.email)
        assertEquals(ApiEndpoints.SIGN_IN_WITH_GOOGLE, requestedPaths.single())
        assertTrue(sentBody!!.contains("an-id-token"))
        // Both halves of the session have to land, or the next 401 has nothing to refresh with.
        assertEquals("google-auth-token", tokenStorage.getAuthToken())
        assertEquals("google-refresh-token", tokenStorage.getRefreshToken())
    }

    @Test
    fun `signInWithGoogle returns failure and stores nothing when the server rejects the token`() = runTest {
        // Arrange
        val tokenStorage = createTestTokenStorage()
        val mockEngine = MockEngine {
            respond(
                content = """{"status":401,"message":"Invalid Google ID token"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val repository = AuthRepository(
            simpleClient(mockEngine),
            tokenStorage,
            Dispatchers.Unconfined,
            NoOpLogger()
        )

        // Act
        val result = repository.signInWithGoogle("a-rejected-token")

        // Assert - a rejected sign in must not leave a half-written session behind
        assertTrue(result.isFailure)
        assertNull(tokenStorage.getAuthToken())
        assertNull(tokenStorage.getRefreshToken())
    }

    @Test
    fun `setPassword omits the current password when the account has none`() = runTest {
        // Arrange
        var sentBody: String? = null
        val mockEngine = MockEngine { request ->
            sentBody = (request.body as io.ktor.http.content.TextContent).text
            respond(content = "", status = HttpStatusCode.NoContent)
        }

        val repository = AuthRepository(
            simpleClient(mockEngine),
            createTestTokenStorage(),
            Dispatchers.Unconfined,
            NoOpLogger()
        )

        // Act
        val result = repository.setPassword(currentPassword = null, newPassword = "newPassword")

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(sentBody!!.contains("newPassword"))
    }

    // region deleteAccount

    private var refreshAttempts = 0

    /** A client like the app's (createHttpClient): bearer tokens from storage and a refresh on every 401. */
    private fun deleteAccountRepository(
        status: HttpStatusCode = HttpStatusCode.NoContent,
        failWith: Exception? = null,
        sentRequests: MutableList<io.ktor.client.request.HttpRequestData> = mutableListOf()
    ): AuthRepository {
        val tokenStorage = createTestTokenStorage()
        kotlinx.coroutines.runBlocking {
            tokenStorage.saveAuthToken("access")
            tokenStorage.saveRefreshToken("refresh")
        }
        val engine = MockEngine { request ->
            sentRequests += request
            failWith?.let { throw it }
            respond(content = "", status = status)
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(this@AuthRepositoryTest.json) }
            install(Auth) {
                bearer {
                    loadTokens { BearerTokens(tokenStorage.getAuthToken()!!, tokenStorage.getRefreshToken()!!) }
                    refreshTokens {
                        refreshAttempts++
                        BearerTokens("new-access", "new-refresh")
                    }
                    // The app's client skips requests that opt out of the auth handling
                    sendWithoutRequest { !it.attributes.contains(io.ktor.client.plugins.auth.AuthCircuitBreaker) }
                }
            }
            defaultRequest {
                url("http://localhost:8080")
                contentType(ContentType.Application.Json)
            }
        }
        return AuthRepository(client, tokenStorage, Dispatchers.Unconfined, NoOpLogger())
    }

    @Test
    fun `deleteAccount sends an authenticated DELETE to the account and reads 204 as deleted`() = runTest {
        val sent = mutableListOf<io.ktor.client.request.HttpRequestData>()

        val result = deleteAccountRepository(sentRequests = sent).deleteAccount()

        assertEquals(DeleteAccountResult.DELETED, result)
        val request = sent.single()
        assertEquals(io.ktor.http.HttpMethod.Delete, request.method)
        assertEquals(ApiEndpoints.ME, request.url.encodedPath)
        assertEquals("Bearer access", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `deleteAccount reads 401 as unauthorized without refreshing the token or retrying`() = runTest {
        val sent = mutableListOf<io.ktor.client.request.HttpRequestData>()

        val result = deleteAccountRepository(HttpStatusCode.Unauthorized, sentRequests = sent).deleteAccount()

        assertEquals(DeleteAccountResult.UNAUTHORIZED, result)
        assertEquals(0, refreshAttempts)
        assertEquals(1, sent.size)
    }

    @Test
    fun `deleteAccount reads a server error as failed`() = runTest {
        val result = deleteAccountRepository(HttpStatusCode.InternalServerError).deleteAccount()

        assertEquals(DeleteAccountResult.FAILED, result)
    }

    @Test
    fun `deleteAccount reads a network error as failed`() = runTest {
        val result = deleteAccountRepository(failWith = java.io.IOException("offline")).deleteAccount()

        assertEquals(DeleteAccountResult.FAILED, result)
    }

    @Test
    fun `deleteAccount without a stored session is unauthorized and sends nothing`() = runTest {
        val sent = mutableListOf<io.ktor.client.request.HttpRequestData>()
        val repository = AuthRepository(
            simpleClient(MockEngine { request -> sent += request; respond("", HttpStatusCode.NoContent) }),
            createTestTokenStorage(),
            Dispatchers.Unconfined,
            NoOpLogger()
        )

        assertEquals(DeleteAccountResult.UNAUTHORIZED, repository.deleteAccount())
        assertTrue(sent.isEmpty())
    }

    // endregion
}
