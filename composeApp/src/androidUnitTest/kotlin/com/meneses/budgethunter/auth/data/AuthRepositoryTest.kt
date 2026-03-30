package com.meneses.budgethunter.auth.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Comprehensive unit tests for AuthRepository.
 * Tests authentication flows including sign up, sign in, token refresh, and sign out.
 */
class AuthRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun createTestTokenStorage(): TokenStorage {
        val testDataStore = PreferenceDataStoreFactory.createWithPath(
            produceFile = { "test-${Uuid.random()}.preferences_pb".toPath() }
        )
        return TokenStorage(testDataStore)
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
        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined)

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
        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined)

        // Act
        val result = repository.signUp(
            email = "existing@example.com",
            name = "Test User",
            password = "password123"
        )

        // Assert
        assertTrue(result.isFailure)
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
        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined)

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
        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined)

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

        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined)

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

        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined)

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

        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined)

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

        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined)

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

        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined)

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

        val repository = AuthRepository(httpClient, tokenStorage, Dispatchers.Unconfined)

        // Act
        val isAuthenticated = repository.isAuthenticated()

        // Assert
        assertFalse(isAuthenticated)
    }
}
