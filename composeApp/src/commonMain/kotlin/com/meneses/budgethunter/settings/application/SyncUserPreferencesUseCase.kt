package com.meneses.budgethunter.settings.application

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.data.network.models.UpdateUserPreferencesRequest
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.settings.data.UserPreferencesRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the settings-screen preferences on the account as well as on the device, so they
 * survive signing out, reinstalling and switching phones.
 *
 * The device stays the source of truth while the app runs — the SMS receiver and the entry
 * screen read the local copy — and the account is how it gets restored.
 */
class SyncUserPreferencesUseCase(
    private val preferencesManager: PreferencesManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val budgetRepository: BudgetRepository,
    private val authRepository: AuthRepository,
    private val logger: Logger
) {

    // A pull that finishes between a push's read of the local values and its request would
    // otherwise let the push send values the pull is about to replace.
    private val mutex = Mutex()

    /**
     * Brings the account's preferences onto the device. An account that never saved any
     * (a fresh one) takes the device's current values instead, so signing up after configuring
     * the app offline does not throw that configuration away.
     *
     * Call it once the budgets have been synced: the default budget is matched by server id and
     * cannot be resolved before its budget exists locally.
     */
    suspend fun pull() = mutex.withLock {
        if (!authRepository.isAuthenticated()) return@withLock

        val remote = userPreferencesRepository.get().getOrElse {
            logger.warn(TAG, "Could not load the preferences from the account", it)
            return@withLock
        }

        if (remote.smsReadingEnabled == null && remote.aiProcessingEnabled == null) {
            pushLocked()
            return@withLock
        }

        remote.smsReadingEnabled?.let { preferencesManager.setSmsReadingEnabled(it) }
        remote.aiProcessingEnabled?.let { preferencesManager.setAiProcessingEnabled(it) }
        remote.selectedBankIds?.let { preferencesManager.setSelectedBankIds(it.toSet()) }

        val defaultBudget = remote.defaultBudgetId?.let { serverId ->
            budgetRepository.getAllCached().firstOrNull { it.serverId == serverId }
        }
        // No match is "none": the account's default is a budget this device does not have (yet),
        // and keeping an old local id would point at an unrelated budget.
        preferencesManager.setDefaultBudgetId(defaultBudget?.id ?: -1)
    }

    /** Saves the device's current preferences on the account. Best effort: failures are logged. */
    suspend fun push() = mutex.withLock { pushLocked() }

    private suspend fun pushLocked() {
        if (!authRepository.isAuthenticated()) return

        val localBudgetId = preferencesManager.getDefaultBudgetId()
        val request = UpdateUserPreferencesRequest(
            smsReadingEnabled = preferencesManager.isSmsReadingEnabled(),
            aiProcessingEnabled = preferencesManager.isAiProcessingEnabled(),
            // A budget that never reached the server has no id to share, so it stays local-only.
            defaultBudgetId = budgetRepository.getById(localBudgetId)?.serverId,
            selectedBankIds = preferencesManager.getSelectedBankIds().toList()
        )

        userPreferencesRepository.save(request).onFailure {
            logger.warn(TAG, "Could not save the preferences on the account", it)
        }
    }

    private companion object {
        const val TAG = "SyncUserPreferences"
    }
}
