package com.meneses.budgethunter.auth.application

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.auth.data.AuthSession
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.platform.GoogleSignInManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
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
     * work gone. Now a failure ending the session throws with nothing touched, and a failure in
     * the cleanup does not reach the next account: [PrepareDataForAccountUseCase] drops what the
     * previous account left when somebody else signs in.
     *
     * @throws Exception when the session could not be ended; the user stays signed in.
     */
    suspend fun execute() = withContext(ioDispatcher) {
        authRepository.signOut()

        try {
            // Each step on its own: one failing must not leave the ones after it undone
            cleanUp("offline mode") { preferencesManager.setOfflineModeEnabled(false) }
            // Without this the platform keeps auto-selecting the last Google account, so the user
            // could never sign back in as somebody else.
            cleanUp("Google credential") { googleSignInManager.signOut() }
            // The budgets and entries belong to the account that just left
            cleanUp("budgets") { budgetRepository.clearAllData() }
            cleanUp("entries") { budgetEntryRepository.clearAllData() }
            // Settings belong to the account; the next one to sign in gets its own.
            cleanUp("settings") { preferencesManager.clearUserPreferences() }
        } finally {
            // Last, so the sign-in screen never shows before the cleanup is done
            authSession.onSignedOut()
        }
    }

    private suspend fun cleanUp(what: String, step: suspend () -> Unit) {
        try {
            step()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(TAG, "Signed out, but clearing the $what failed", e)
        }
    }

    private companion object {
        const val TAG = "SignOutUseCase"
    }
}
