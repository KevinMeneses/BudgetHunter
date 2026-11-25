package com.meneses.budgethunter.auth.data

import com.meneses.budgethunter.commons.data.network.ApiEndpoints
import com.meneses.budgethunter.commons.data.network.bodyOrThrow
import com.meneses.budgethunter.commons.data.network.models.AcceptPrivacyPolicyRequest
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
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
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
        password: String,
        acceptedPrivacyPolicyVersion: String
    ): Result<SignUpResponse> = withContext(ioDispatcher) {
        try {
            val response = httpClient.post(ApiEndpoints.SIGN_UP) {
                setBody(SignUpRequest(email, name, password, acceptedPrivacyPolicyVersion))
            }
            if (!response.status.isSuccess()) {
                // 409 is the answer for an existing account; servers before that change sent a
                // 400 with this message.
                // TODO: drop the 400 match once the production backend answers 409 for a taken email.
                val emailTaken = response.status == HttpStatusCode.Conflict ||
                    (
                        response.status == HttpStatusCode.BadRequest &&
                            response.bodyAsText().contains(EMAIL_EXISTS_MESSAGE, ignoreCase = true)
                        )
                val error =
                    if (emailTaken) EmailAlreadyRegisteredException()
                    else IllegalStateException("Sign up answered ${response.status}")
                logger.warn(TAG, "Sign up refused: ${response.status}")
                return@withContext Result.failure(error)
            }
            // The status was checked above: plain body() is safe here
            val signUpResponse = response.body<SignUpResponse>()

            // Sign up only returns user info, no tokens
            // User must sign in separately to get tokens
            Result.success(signUpResponse)
        } catch (e: CancellationException) {
            throw e
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
            val authResponse = response.bodyOrThrow<AuthResponse>()

            persistSession(authResponse)

            Result.success(authResponse)
        } catch (e: CancellationException) {
            throw e
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
            val authResponse = response.bodyOrThrow<AuthResponse>()

            // Token rotation: the server issues a new refresh token with every exchange.
            persistSession(authResponse)

            Result.success(authResponse)
        } catch (e: CancellationException) {
            throw e
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
     * [acceptedPrivacyPolicyVersion] is what the screen's notice says continuing accepts.
     */
    suspend fun signInWithGoogle(
        idToken: String,
        acceptedPrivacyPolicyVersion: String
    ): Result<AuthResponse> = withContext(ioDispatcher) {
        try {
            val response = httpClient.post(ApiEndpoints.SIGN_IN_WITH_GOOGLE) {
                setBody(GoogleSignInRequest(idToken, acceptedPrivacyPolicyVersion))
            }
            val authResponse = response.bodyOrThrow<AuthResponse>()

            persistSession(authResponse)

            Result.success(authResponse)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(TAG, "Google sign in failed", e)
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<CurrentUser> = withContext(ioDispatcher) {
        try {
            Result.success(httpClient.get(ApiEndpoints.ME).bodyOrThrow<CurrentUser>())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(TAG, "Could not load the current user", e)
            Result.failure(e)
        }
    }

    /**
     * Tells the server the user has the Google link notice (`passwordRemoved`), so later Google
     * sign-ins stop answering it. Until then each one answers it again: that is what recovers a
     * notice whose answer was lost. A server without the endpoint answers 404, which changes nothing.
     */
    suspend fun acknowledgeLinkNotice(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = httpClient.delete(ApiEndpoints.LINK_NOTICE)
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                logger.warn(TAG, "Link notice acknowledgement refused: ${response.status}")
                Result.failure(IllegalStateException("Link notice acknowledgement answered ${response.status}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(TAG, "Could not acknowledge the link notice", e)
            Result.failure(e)
        }
    }

    /**
     * Records that the signed-in account accepted the privacy policy [version], which must be the
     * server's current one (`privacyPolicy.currentVersion` of [getCurrentUser]); anything else is a 400.
     */
    suspend fun acceptPrivacyPolicy(version: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = httpClient.post(ApiEndpoints.PRIVACY_POLICY) {
                setBody(AcceptPrivacyPolicyRequest(version))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                logger.warn(TAG, "Privacy policy acceptance refused: ${response.status}")
                Result.failure(IllegalStateException("Privacy policy acceptance answered ${response.status}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(TAG, "Could not record the privacy policy acceptance", e)
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
            }.bodyOrThrow<Unit>()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(TAG, "Could not set the password", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes the account on the server. The local session is left alone: the caller decides
     * what to clear from the answer.
     *
     * A 401 is not retried through a token refresh: the caller treats it as an ended session,
     * like the rest of the app does with a refused one. [AuthCircuitBreaker] turns the refresh off
     * but also stops the Auth plugin from adding the token, so it is sent here from the storage,
     * which every refresh writes to.
     */
    suspend fun deleteAccount(): DeleteAccountResult = withContext(ioDispatcher) {
        try {
            val accessToken = tokenStorage.getAuthToken()
                ?: return@withContext DeleteAccountResult.UNAUTHORIZED
            val response = httpClient.delete(ApiEndpoints.ME) {
                attributes.put(AuthCircuitBreaker, Unit)
                bearerAuth(accessToken)
            }
            when {
                response.status.isSuccess() -> DeleteAccountResult.DELETED
                response.status == HttpStatusCode.Unauthorized -> {
                    logger.warn(TAG, "Account deletion refused: the session is no longer valid")
                    DeleteAccountResult.UNAUTHORIZED
                }
                else -> {
                    logger.warn(TAG, "Account deletion failed: ${response.status}")
                    DeleteAccountResult.FAILED
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(TAG, "Account deletion failed", e)
            DeleteAccountResult.FAILED
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
        } catch (e: CancellationException) {
            throw e
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
        const val EMAIL_EXISTS_MESSAGE = "Email already exists"
    }
}
