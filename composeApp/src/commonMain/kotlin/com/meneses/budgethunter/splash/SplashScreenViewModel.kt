package com.meneses.budgethunter.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.AppUpdateManager
import com.meneses.budgethunter.commons.platform.AppUpdateResult
import com.meneses.budgethunter.splash.application.SplashEvent
import com.meneses.budgethunter.splash.application.SplashIntent
import com.meneses.budgethunter.splash.application.SplashState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SplashScreenViewModel(
    private val appUpdateManager: AppUpdateManager,
    private val authRepository: AuthRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<SplashEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun sendIntent(intent: SplashIntent) {
        when (intent) {
            is SplashIntent.VerifyUpdate -> verifyUpdate()
        }
    }

    private fun verifyUpdate() {
        viewModelScope.launch {
            val isOfflineModeEnabled = preferencesManager.isOfflineModeEnabled()
            val isAuthenticated = authRepository.isAuthenticated()

            // Signing in from offline mode used to leave the flag on. With a session it is not
            // offline mode: clear it before navigating, or the privacy policy check sent with the
            // navigation would skip this account on every start.
            if (isOfflineModeEnabled && isAuthenticated) {
                preferencesManager.setOfflineModeEnabled(false)
            }

            // If offline mode is enabled, skip authentication check
            _uiState.update {
                it.copy(isAuthenticated = isOfflineModeEnabled || isAuthenticated)
            }

            appUpdateManager.checkForUpdates { result ->
                when (result) {
                    is AppUpdateResult.NoUpdateAvailable -> emitNavigationEvent()
                    is AppUpdateResult.UpdateInProgress -> setUpdateInProgressState()
                    is AppUpdateResult.UpdateAvailable -> result.startUpdate()
                    is AppUpdateResult.UpdateFailed -> emitNavigationEvent()
                }
            }
        }
    }

    private fun emitNavigationEvent() {
        viewModelScope.launch {
            delay(200)
            val event = if (_uiState.value.isAuthenticated) {
                SplashEvent.NavigateToBudgetList
            } else {
                SplashEvent.NavigateToSignIn
            }
            _events.trySend(event)
        }
    }

    private fun setUpdateInProgressState() =
        _uiState.update { it.copy(updatingApp = true) }
}
