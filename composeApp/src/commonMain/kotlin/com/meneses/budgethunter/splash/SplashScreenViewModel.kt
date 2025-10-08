package com.meneses.budgethunter.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.commons.platform.AppUpdateManager
import com.meneses.budgethunter.commons.platform.AppUpdateResult
import com.meneses.budgethunter.splash.application.SplashEvent
import com.meneses.budgethunter.splash.application.SplashState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SplashScreenViewModel(
    private val appUpdateManager: AppUpdateManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashState())
    val uiState = _uiState.asStateFlow()

    fun sendEvent(event: SplashEvent) {
        when (event) {
            is SplashEvent.VerifyUpdate -> verifyUpdate()
        }
    }

    private fun verifyUpdate() {
        viewModelScope.launch {
            val isAuthenticated = authRepository.isAuthenticated()
            _uiState.update { it.copy(isAuthenticated = isAuthenticated) }

            appUpdateManager.checkForUpdates { result ->
                when (result) {
                    is AppUpdateResult.NoUpdateAvailable -> setNavigateState()
                    is AppUpdateResult.UpdateInProgress -> setUpdateInProgressState()
                    is AppUpdateResult.UpdateAvailable -> result.startUpdate()
                    is AppUpdateResult.UpdateFailed -> setNavigateState()
                }
            }
        }
    }

    private fun setNavigateState() =
        _uiState.update { it.copy(navigate = true) }

    private fun setUpdateInProgressState() =
        _uiState.update { it.copy(updatingApp = true) }
}
