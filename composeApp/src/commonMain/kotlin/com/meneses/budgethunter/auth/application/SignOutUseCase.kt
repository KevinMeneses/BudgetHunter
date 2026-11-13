package com.meneses.budgethunter.auth.application

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.auth.data.AuthSession
import com.meneses.budgethunter.billing.domain.StoreBilling
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.platform.GoogleSignInManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Use case that handles the complete sign-out flow.
 *
 * This centralizes the sign-out logic and eliminates circular dependencies
 * between AuthRepository and data repositories.
 */
class SignOutUseCase(
    private val authRepository: AuthRepository,
    private val authSession: AuthSession,
    private val budgetRepository: BudgetRepository,
    private val budgetEntryRepository: BudgetEntryRepository,
    private val preferencesManager: PreferencesManager,
    private val googleSignInManager: GoogleSignInManager,
    private val storeBilling: StoreBilling,
    private val logger: Logger,
    private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Whether signing out now would delete something the server does not have. The wipe cannot
     * wait for a sync, so the confirmation is where the user hears about it.
     */
    suspend fun hasUnsyncedChanges(): Boolean = withContext(ioDispatcher) {
        budgetRepository.hasUnsyncedChanges() || budgetEntryRepository.hasUnsyncedChanges()
    }

    /**
     * Ends the session first and clears the device afterwards.
     *
     * The other way round, a failure after the wipe left the user signed in with their unsynced
     * work gone. Now a failure ending the session throws with nothing touched. A failure deleting
     * the budgets or entries leaves a marker, and [PrepareDataForAccountUseCase.finishPendingWipe]
     * completes the wipe at the next sign in or before offline mode, so nothing reaches the next
     * account.
     *
     * @throws Exception when the session could not be ended; the user stays signed in.
     */
    suspend fun execute() = withContext(ioDispatcher) {
        authRepository.signOut()

        // The session is already gone, so a cancellation here would only leave the device half
        // cleared: the cleanup finishes regardless.
        withContext(NonCancellable) { clearAccountData() }
    }

    private suspend fun clearAccountData() {
        try {
            // Set before deleting and cleared only once both deletes worked
            cleanUp("wipe marker") { preferencesManager.setLocalWipePending(true) }
            // Each step on its own: one failing must not leave the ones after it undone
            cleanUp("offline mode") { preferencesManager.setOfflineModeEnabled(false) }
            // Without this the platform keeps auto-selecting the last Google account, so the user
            // could never sign back in as somebody else.
            cleanUp("Google credential") { googleSignInManager.signOut() }
            // The next account to buy on this device logs in with its own id anyway; this drops the cache
            cleanUp("store account") { storeBilling.logOut() }
            // The budgets and entries belong to the account that just left
            val budgetsCleared = cleanUp("budgets") { budgetRepository.clearAllData() }
            val entriesCleared = cleanUp("entries") { budgetEntryRepository.clearAllData() }
            // Settings belong to the account; the next one to sign in gets its own.
            cleanUp("settings") { preferencesManager.clearUserPreferences() }
            if (budgetsCleared && entriesCleared) {
                cleanUp("wipe marker") { preferencesManager.setLocalWipePending(false) }
            }
        } finally {
            // Last, so the sign-in screen never shows before the cleanup is done
            authSession.onSignedOut()
        }
    }

    /** @return false when the step failed; the failure is logged, never thrown. */
    private suspend fun cleanUp(what: String, step: suspend () -> Unit): Boolean =
        try {
            step()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(TAG, "Signed out, but clearing the $what failed", e)
            false
        }

    private companion object {
        const val TAG = "SignOutUseCase"
    }
}
