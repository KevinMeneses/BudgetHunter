package com.meneses.budgethunter.auth.data

import com.meneses.budgethunter.commons.data.network.ApiEndpoints
import com.meneses.budgethunter.commons.data.network.models.AuthResponse
import com.meneses.budgethunter.commons.data.network.models.RefreshTokenRequest
import com.meneses.budgethunter.commons.data.network.models.SignInRequest
import com.meneses.budgethunter.commons.data.network.models.SignUpRequest
import com.meneses.budgethunter.commons.data.network.models.SignUpResponse
import com.meneses.budgethunter.commons.data.sync.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class AuthRepository(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage,
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger
) {

    suspend fun signUp(
        email: String,
        name: String,
        password: String
    ): Result<SignUpResponse> = withContext(ioDispatcher) {
        try {
            val response = httpClient.post(ApiEndpoints.SIGN_UP) {
                setBody(SignUpRequest(email, name, password))
            }
            val signUpResponse = response.body<SignUpResponse>()

            // Sign up only returns user info, no tokens
            // User must sign in separately to get tokens
            Result.success(signUpResponse)
        } catch (e: Exception) {
            logger.warn(TAG, "Sign up failed", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(
        email: String,
        password: String
    ): Result<AuthResponse> = withContext(ioDispatcher) {
        try {
            val response = httpClient.post(ApiEndpoints.SIGN_IN) {
                setBody(SignInRequest(email, password))
            }
            val authResponse = response.body<AuthResponse>()

            // Store tokens on successful sign in
            tokenStorage.saveAuthToken(authResponse.authToken)
            tokenStorage.saveRefreshToken(authResponse.refreshToken)
            invalidateCachedBearerToken()

            Result.success(authResponse)
        } catch (e: Exception) {
            // Includes the writes to TokenStorage: a keychain or keystore that refuses to store
            // the session shows up as a plain "sign in failed", so name the cause in the log.
            logger.warn(TAG, "Sign in failed", e)
            Result.failure(e)
        }
    }

    suspend fun refreshToken(): Result<AuthResponse> = withContext(ioDispatcher) {
        try {
            val currentRefreshToken = tokenStorage.getRefreshToken()
                ?: return@withContext Result.failure(Exception("No refresh token available"))

            val response = httpClient.post(ApiEndpoints.REFRESH_TOKEN) {
                setBody(RefreshTokenRequest(currentRefreshToken))
            }
            val authResponse = response.body<AuthResponse>()

            // Store new tokens (token rotation)
            tokenStorage.saveAuthToken(authResponse.authToken)
            tokenStorage.saveRefreshToken(authResponse.refreshToken)
            invalidateCachedBearerToken()

            Result.success(authResponse)
        } catch (e: Exception) {
            logger.warn(TAG, "Token refresh failed", e)
            Result.failure(e)
        }
    }

    suspend fun signOut() = withContext(ioDispatcher) {
        // TODO: Call server-side session invalidation endpoint once available in the backend.
        tokenStorage.clearTokens()
        invalidateCachedBearerToken()
    }

    suspend fun isAuthenticated(): Boolean = withContext(ioDispatcher) {
        try {
            tokenStorage.getAuthToken() != null
        } catch (e: Exception) {
            // Asked from the splash screen, where an exception would take the whole app down.
            // Treating an unreadable session as "not signed in" sends the user to sign in again.
            logger.warn(TAG, "Could not read the stored session", e)
            false
        }
    }

    /**
     * Drops the token cached in memory by Ktor's [BearerAuthProvider].
     *
     * The provider only calls `loadTokens` once and reuses the result until a 401 triggers a
     * refresh, so writing to [TokenStorage] is not enough: without this call the client keeps
     * sending the previous session's token, which makes the server answer with the *previous
     * user's* data after switching accounts.
     */
    private fun invalidateCachedBearerToken() {
        httpClient.authProvider<BearerAuthProvider>()?.clearToken()
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
