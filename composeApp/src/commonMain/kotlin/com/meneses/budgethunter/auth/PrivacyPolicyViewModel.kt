package com.meneses.budgethunter.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.auth.application.CheckPrivacyPolicyUseCase
import com.meneses.budgethunter.auth.application.PrivacyPolicyIntent
import com.meneses.budgethunter.auth.application.PrivacyPolicyState
import com.meneses.budgethunter.auth.application.SignOutUseCase
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.commons.data.sync.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The dialog that asks a signed-in account to accept the privacy policy when the server says it
 * has not accepted the current version. It lives over the whole navigation, and navigation asks
 * for the [PrivacyPolicyIntent.Check] after every sign in and when the app opens with a session.
 *
 * It never blocks the app: a failed check shows nothing, and a failed acceptance or sign out
 * closes the dialog anyway. The server still lacks the acceptance, so the next check asks again.
 */
class PrivacyPolicyViewModel(
    private val checkPrivacyPolicy: CheckPrivacyPolicyUseCase,
    private val authRepository: AuthRepository,
    private val signOutUseCase: SignOutUseCase,
    private val applicationScope: CoroutineScope,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyPolicyState())
    val uiState = _uiState.asStateFlow()

    private var checkJob: Job? = null

    fun sendIntent(intent: PrivacyPolicyIntent) {
        when (intent) {
            PrivacyPolicyIntent.Check -> check()
            PrivacyPolicyIntent.Accept -> accept()
            PrivacyPolicyIntent.SignOut -> requestSignOut()
            PrivacyPolicyIntent.ConfirmSignOut -> signOut()
            PrivacyPolicyIntent.CancelSignOut ->
                _uiState.update { it.copy(isSignOutConfirmationVisible = false) }
        }
    }

    private fun check() {
        // One question at a time: a check already running, or a dialog already open, answers it
        if (checkJob?.isActive == true || _uiState.value.pendingVersion != null) return
        checkJob = viewModelScope.launch {
            val pending = checkPrivacyPolicy.pendingVersion() ?: return@launch
            _uiState.update { it.copy(pendingVersion = pending) }
        }
    }

    private fun accept() {
        val version = _uiState.value.pendingVersion ?: return
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            // Failure is logged by the repository; the next check asks again
            authRepository.acceptPrivacyPolicy(version)
            close()
        }
    }

    private fun requestSignOut() {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            // Signing out deletes the device's data, so the same warning as in Settings when some
            // of it never reached the server. Unable to tell, warn anyway.
            val losesChanges = try {
                signOutUseCase.hasUnsyncedChanges()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                true
            }
            if (losesChanges) {
                _uiState.update { it.copy(isWorking = false, isSignOutConfirmationVisible = true) }
            } else {
                signOut()
            }
        }
    }

    // Outlives this view model, like the sign out of Settings: a half-done wipe must not be cut
    // short. Navigation to sign in follows AuthSession, so nothing is sent from here.
    private fun signOut() {
        _uiState.update { it.copy(isWorking = true, isSignOutConfirmationVisible = false) }
        applicationScope.launch {
            try {
                signOutUseCase.execute()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Still signed in and nothing deleted: the next check asks again
                logger.warn(TAG, "Sign out from the privacy policy dialog failed", e)
            } finally {
                close()
            }
        }
    }

    private fun close() {
        _uiState.update { PrivacyPolicyState() }
    }

    private companion object {
        const val TAG = "PrivacyPolicyViewModel"
    }
}
