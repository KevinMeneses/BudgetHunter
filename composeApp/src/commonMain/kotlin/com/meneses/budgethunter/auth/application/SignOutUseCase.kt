package com.meneses.budgethunter.auth.application

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Use case that handles the complete sign-out flow.
 *
 * This centralizes the sign-out logic and eliminates circular dependencies
 * between AuthRepository and data repositories.
 *
 * Responsibilities:
 * 1. Clear all local budget data
 * 2. Clear all local budget entry data
 * 3. Clear authentication tokens
 * 4. Disable offline mode preference
 */
class SignOutUseCase(
    private val authRepository: AuthRepository,
    private val budgetRepository: BudgetRepository,
    private val budgetEntryRepository: BudgetEntryRepository,
    private val preferencesManager: PreferencesManager,
    private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Execute sign-out flow:
     * - Clear all local data (budgets and entries)
     * - Clear authentication tokens
     * - Disable offline mode
     */
    suspend fun execute() = withContext(ioDispatcher) {
        // Clear all local data first to prevent data leaking between users
        budgetRepository.clearAllData()
        budgetEntryRepository.clearAllData()

        // Clear tokens
        authRepository.signOut()

        // Disable offline mode when signing out
        preferencesManager.setOfflineModeEnabled(false)
    }
}
