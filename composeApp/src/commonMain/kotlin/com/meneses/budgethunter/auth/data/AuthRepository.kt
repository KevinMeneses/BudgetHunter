package com.meneses.budgethunter.auth.data

import com.meneses.budgethunter.commons.data.network.models.AuthResponse
import com.meneses.budgethunter.commons.data.network.models.RefreshTokenRequest
import com.meneses.budgethunter.commons.data.network.models.SignInRequest
import com.meneses.budgethunter.commons.data.network.models.SignUpRequest
import com.meneses.budgethunter.commons.data.network.models.SignUpResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class AuthRepository(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage,
    private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun signUp(
        email: String,
        name: String,
        password: String
    ): Result<SignUpResponse> = withContext(ioDispatcher) {
        try {
            val response = httpClient.post("/api/users/sign_up") {
                setBody(SignUpRequest(email, name, password))
            }
            val signUpResponse = response.body<SignUpResponse>()

            // Sign up only returns user info, no tokens
            // User must sign in separately to get tokens
            Result.success(signUpResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(
        email: String,
        password: String
    ): Result<AuthResponse> = withContext(ioDispatcher) {
        try {
            val response = httpClient.post("/api/users/sign_in") {
                setBody(SignInRequest(email, password))
            }
            val authResponse = response.body<AuthResponse>()

            // Store tokens on successful sign in
            tokenStorage.saveAuthToken(authResponse.authToken)
            tokenStorage.saveRefreshToken(authResponse.refreshToken)

            Result.success(authResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshToken(): Result<AuthResponse> = withContext(ioDispatcher) {
        try {
            val currentRefreshToken = tokenStorage.getRefreshToken()
                ?: return@withContext Result.failure(Exception("No refresh token available"))

            val response = httpClient.post("/api/users/refresh_token") {
                setBody(RefreshTokenRequest(currentRefreshToken))
            }
            val authResponse = response.body<AuthResponse>()

            // Store new tokens (token rotation)
            tokenStorage.saveAuthToken(authResponse.authToken)
            tokenStorage.saveRefreshToken(authResponse.refreshToken)

            Result.success(authResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() = withContext(ioDispatcher) {
        tokenStorage.clearTokens()
    }

    suspend fun isAuthenticated(): Boolean = withContext(ioDispatcher) {
        tokenStorage.getAuthToken() != null
    }
}
