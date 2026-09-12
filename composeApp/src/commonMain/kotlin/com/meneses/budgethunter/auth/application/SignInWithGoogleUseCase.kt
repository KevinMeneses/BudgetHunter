package com.meneses.budgethunter.auth.application

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetEntry.data.BudgetEntrySyncManager
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.platform.GoogleSignInManager
import com.meneses.budgethunter.commons.platform.GoogleSignInResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Signs in with Google: asks the platform for an ID token, trades it for a session, and kicks
 * off the initial sync.
 *
 * Shared by the sign-in and sign-up screens. The same call covers both because the server
 * creates the account on first use, so there is nothing for the two screens to do differently —
 * and keeping it in one place stops them from drifting apart on what happens after a sign in.
 */
class SignInWithGoogleUseCase(
    private val googleSignInManager: GoogleSignInManager,
    private val authRepository: AuthRepository,
    private val budgetRepository: BudgetRepository,
    private val budgetEntrySyncManager: BudgetEntrySyncManager,
    private val applicationScope: CoroutineScope
) {

    val isAvailable get() = googleSignInManager.isAvailable

    suspend fun execute(): GoogleAuthOutcome =
        when (val result = requestIdToken()) {
            is GoogleSignInResult.Success -> exchangeForSession(result.idToken)
            GoogleSignInResult.Cancelled -> GoogleAuthOutcome.Cancelled
            GoogleSignInResult.NoCredentialAvailable -> GoogleAuthOutcome.NoGoogleAccount
            is GoogleSignInResult.Failure -> GoogleAuthOutcome.Failed
        }

    /**
     * [GoogleSignInManager] is callback-based so Swift can implement it; bridge that back to a
     * suspending call for the view models.
     */
    private suspend fun requestIdToken(): GoogleSignInResult =
        suspendCancellableCoroutine { continuation ->
            googleSignInManager.signIn { result ->
                if (continuation.isActive) continuation.resume(result)
            }
        }

    private suspend fun exchangeForSession(idToken: String): GoogleAuthOutcome =
        authRepository.signInWithGoogle(idToken).fold(
            onSuccess = { authResponse ->
                // Deliberately the application scope, not the caller's: the screen is popped the
                // moment we return, and a sync tied to its lifetime would be cancelled mid-flight.
                applicationScope.launch {
                    budgetRepository.sync()
                    budgetEntrySyncManager.syncAllBudgetsEntries()
                }
                // The email comes from the server, since the user never typed one.
                GoogleAuthOutcome.Success(authResponse.email)
            },
            onFailure = { GoogleAuthOutcome.Failed }
        )
}

sealed interface GoogleAuthOutcome {
    data class Success(val email: String) : GoogleAuthOutcome

    /** The user dismissed the account picker. Expected, and not something to show an error for. */
    data object Cancelled : GoogleAuthOutcome

    data object NoGoogleAccount : GoogleAuthOutcome

    data object Failed : GoogleAuthOutcome
}
