package com.meneses.budgethunter.auth.application

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.auth.data.AuthSession
import com.meneses.budgethunter.auth.data.DeleteAccountResult
import com.meneses.budgethunter.billing.data.PremiumRepository
import com.meneses.budgethunter.commons.data.sync.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Deletes the signed-in account on the server and, once it is gone, clears the device exactly
 * like signing out does ([SignOutUseCase]).
 *
 * The server hands the user's shared budgets to another member, deletes the ones nobody else is
 * in and keeps their entries in other people's budgets. It cannot cancel a store subscription:
 * only Google Play or the App Store can, which is why the confirmation asks about the plan first.
 * Signing out also logs the store out (RevenueCat), so the next account starts clean.
 */
class DeleteAccountUseCase(
    private val authRepository: AuthRepository,
    private val premiumRepository: PremiumRepository,
    private val signOutUseCase: SignOutUseCase,
    private val authSession: AuthSession,
    private val logger: Logger,
    private val ioDispatcher: CoroutineDispatcher,
    private val planTimeoutMillis: Long = PLAN_TIMEOUT_MILLIS
) {
    enum class Result {
        /** The account is gone and the device cleared; navigation follows [AuthSession]. */
        DELETED,

        /** The server refused the session: the user goes to sign in, as with any expired one. */
        SESSION_EXPIRED,

        /** Nothing changed: the user stays signed in and can try again. */
        FAILED
    }

    /**
     * Whether the account pays for premium: true or false, or null when the server could not
     * be asked in time.
     */
    suspend fun isPremium(): Boolean? = withContext(ioDispatcher) {
        try {
            withTimeoutOrNull(planTimeoutMillis) { premiumRepository.status().isPremium }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(TAG, "Could not read the plan before deleting the account", e)
            null
        }
    }

    suspend fun execute(): Result = withContext(ioDispatcher) {
        // The delete never refreshes its token, so an access token that merely expired would end
        // a session whose refresh token still works. Any call through the usual client renews it
        // first; if the server refuses the session there, the client already ended it and the
        // delete finds no token. Its answer does not matter: a network failure fails the delete too.
        authRepository.getCurrentUser()
        when (authRepository.deleteAccount()) {
            DeleteAccountResult.DELETED -> {
                // The account no longer exists: whatever happens now, the cleanup must finish
                withContext(NonCancellable) { clearDevice() }
                Result.DELETED
            }
            DeleteAccountResult.UNAUTHORIZED -> {
                withContext(NonCancellable) { endExpiredSession() }
                Result.SESSION_EXPIRED
            }
            DeleteAccountResult.FAILED -> Result.FAILED
        }
    }

    private suspend fun clearDevice() {
        try {
            signOutUseCase.execute()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Only dropping the tokens can fail here, and they are already refused by the server.
            // Leave for sign in anyway: the next sign in drops what this account left behind
            // (PrepareDataForAccountUseCase).
            logger.warn(TAG, "Account deleted, but signing out failed", e)
            authSession.onSignedOut()
        }
    }

    /** Same as the refresh refusal in the HTTP client: drop the tokens, keep the data, go to sign in. */
    private suspend fun endExpiredSession() {
        try {
            authRepository.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(TAG, "Could not drop the refused session", e)
        }
        authSession.onSessionExpired()
    }

    private companion object {
        const val TAG = "DeleteAccountUseCase"

        // The confirmation waits on this; past it the dialog warns about the subscription anyway
        const val PLAN_TIMEOUT_MILLIS = 5_000L
    }
}
