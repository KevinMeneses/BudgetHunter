package com.meneses.budgethunter.auth.application

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.PreferencesManager

/**
 * Runs right after a sign in, before the first sync, so a different account never sees the data
 * the previous one left on the device.
 *
 * Signing out already wipes the local data, but a session can also end without it: an expired
 * refresh token just drops the tokens and sends the user to sign in again with the old budgets
 * still in the database. Comparing against the last account that signed in covers every path.
 *
 * Only what reached the server is dropped. Budgets created offline were never tied to an account,
 * and pushing them to whoever signs in is how offline work gets adopted.
 */
class PrepareDataForAccountUseCase(
    private val preferencesManager: PreferencesManager,
    private val budgetRepository: BudgetRepository,
    private val budgetEntryRepository: BudgetEntryRepository
) {
    suspend fun execute(email: String) {
        val lastEmail = preferencesManager.getLastSignedInEmail()
        val wiped = finishPendingWipe()
        if (!wiped && lastEmail != null && !lastEmail.trim().equals(email.trim(), ignoreCase = true)) {
            budgetRepository.clearSyncedData()
            // Settings belong to the account, same as on sign out.
            preferencesManager.clearUserPreferences()
        }
        preferencesManager.setLastSignedInEmail(email)
    }

    /**
     * Completes a sign out whose wipe failed ([SignOutUseCase] leaves a marker). Unlike the account
     * check above it drops everything, unsynced work included: that work belonged to the account
     * that signed out, which was warned it would be deleted. Also run before entering offline mode,
     * which otherwise would show the previous account's budgets.
     *
     * @return true when there was a pending wipe and it is now done.
     */
    suspend fun finishPendingWipe(): Boolean {
        if (!preferencesManager.isLocalWipePending()) return false
        budgetRepository.clearAllData()
        budgetEntryRepository.clearAllData()
        preferencesManager.clearUserPreferences()
        preferencesManager.setLocalWipePending(false)
        return true
    }
}
