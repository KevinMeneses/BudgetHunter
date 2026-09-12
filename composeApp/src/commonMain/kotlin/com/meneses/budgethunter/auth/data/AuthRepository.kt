package com.meneses.budgethunter.auth.data

import com.meneses.budgethunter.commons.data.network.ApiEndpoints
import com.meneses.budgethunter.commons.data.network.models.AuthResponse
import com.meneses.budgethunter.commons.data.network.models.CurrentUser
import com.meneses.budgethunter.commons.data.network.models.GoogleSignInRequest
import com.meneses.budgethunter.commons.data.network.models.RefreshTokenRequest
import com.meneses.budgethunter.commons.data.network.models.SetPasswordRequest
import com.meneses.budgethunter.commons.data.network.models.SignInRequest
import com.meneses.budgethunter.commons.data.network.models.SignUpRequest
import com.meneses.budgethunter.commons.data.network.models.SignUpResponse
import com.meneses.budgethunter.commons.data.sync.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.get
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

            persistSession(authResponse)

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

            // Token rotation: the server issues a new refresh token with every exchange.
            persistSession(authResponse)

            Result.success(authResponse)
        } catch (e: Exception) {
            logger.warn(TAG, "Token refresh failed", e)
            Result.failure(e)
        }
    }

    /**
     * Exchanges a Google ID token for a session.
     *
     * The server creates the account on first use and links it to an existing password account
     * with the same verified email, so this one call covers both signing up and signing in.
     */
    suspend fun signInWithGoogle(idToken: String): Result<AuthResponse> = withContext(ioDispatcher) {
        try {
            val response = httpClient.post(ApiEndpoints.SIGN_IN_WITH_GOOGLE) {
                setBody(GoogleSignInRequest(idToken))
            }
            val authResponse = response.body<AuthResponse>()

            persistSession(authResponse)

            Result.success(authResponse)
        } catch (e: Exception) {
            logger.warn(TAG, "Google sign in failed", e)
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<CurrentUser> = withContext(ioDispatcher) {
        try {
            Result.success(httpClient.get(ApiEndpoints.ME).body<CurrentUser>())
        } catch (e: Exception) {
            logger.warn(TAG, "Could not load the current user", e)
            Result.failure(e)
        }
    }

    /**
     * Sets or replaces the account password.
     *
     * [currentPassword] is null for an account that has none yet — the one created through Google
     * sign in. Asking for a password the user never chose would be a dead end, and the session
     * token already proves who they are.
     */
    suspend fun setPassword(
        currentPassword: String?,
        newPassword: String
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            httpClient.post(ApiEndpoints.PASSWORD) {
                setBody(SetPasswordRequest(currentPassword, newPassword))
            }.body<Unit>()
            Result.success(Unit)
        } catch (e: Exception) {
            logger.warn(TAG, "Could not set the password", e)
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
     * Stores a freshly issued session.
     *
     * Every sign-in path goes through here so none of them can forget the cache invalidation
     * below, which is what keeps a switched account from seeing the previous user's data.
     */
    private suspend fun persistSession(authResponse: AuthResponse) {
        tokenStorage.saveAuthToken(authResponse.authToken)
        tokenStorage.saveRefreshToken(authResponse.refreshToken)
        invalidateCachedBearerToken()
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
