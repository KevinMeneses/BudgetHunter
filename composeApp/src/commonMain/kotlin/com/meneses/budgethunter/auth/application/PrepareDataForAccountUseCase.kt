package com.meneses.budgethunter.auth.application

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
    private val budgetRepository: BudgetRepository
) {
    suspend fun execute(email: String) {
        val lastEmail = preferencesManager.getLastSignedInEmail()
        if (lastEmail != null && !lastEmail.trim().equals(email.trim(), ignoreCase = true)) {
            budgetRepository.clearSyncedData()
            // Settings belong to the account, same as on sign out.
            preferencesManager.clearUserPreferences()
        }
        preferencesManager.setLastSignedInEmail(email)
    }
}
