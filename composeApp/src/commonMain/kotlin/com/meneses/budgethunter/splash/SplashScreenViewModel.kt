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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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

    private var updateCheckStarted = false

    private fun verifyUpdate() {
        // The screen asks again when it is recomposed (e.g. after a rotation): one check is enough
        if (updateCheckStarted) return
        updateCheckStarted = true

        viewModelScope.launch {
            val isOfflineModeEnabled = try {
                preferencesManager.isOfflineModeEnabled()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Unreadable preferences must not keep the app on the splash, nor take it down:
                // offline mode off, and the stored session decides
                false
            }
            val isAuthenticated = authRepository.isAuthenticated()

            // If offline mode is enabled, skip authentication check
            _uiState.update {
                it.copy(isAuthenticated = isOfflineModeEnabled || isAuthenticated)
            }

            // Play may answer from any thread and more than once (an update in progress answers
            // again when it ends); the answers are handled here, one at a time. Once the app
            // moves on the channel is closed, so a late answer is dropped.
            val answers = Channel<AppUpdateResult>(Channel.UNLIMITED)
            appUpdateManager.checkForUpdates { result -> answers.trySend(result) }

            // Play may never answer (seen right after a device reboot), nor answer again after
            // an update is offered or reported in progress (nothing launches the update flow on
            // Android, and iOS just opens the App Store): each wait has a limit, past which the app
            // goes on as if there were no update.
            while (true) {
                val answer = withTimeoutOrNull(UPDATE_CHECK_TIMEOUT_MS) { answers.receive() }
                when (answer) {
                    null,
                    is AppUpdateResult.NoUpdateAvailable,
                    is AppUpdateResult.UpdateFailed -> break
                    // Limited too: nothing here learns when it ends. If the update flow is ever
                    // launched (AppUpdateLauncherDelegate), its install listener answers when it
                    // finishes, and this wait should then allow a long download.
                    is AppUpdateResult.UpdateInProgress -> setUpdateInProgressState()
                    is AppUpdateResult.UpdateAvailable -> {
                        // An update that cannot even be offered never answers: go on at once
                        val offered = try {
                            answer.startUpdate()
                            true
                        } catch (e: Exception) {
                            false
                        }
                        if (!offered) break
                    }
                }
            }
            answers.close()
            emitNavigationEvent()
        }
    }

    private suspend fun emitNavigationEvent() {
        delay(NAVIGATION_DELAY_MS)
        val event = if (_uiState.value.isAuthenticated) {
            SplashEvent.NavigateToBudgetList
        } else {
            SplashEvent.NavigateToSignIn
        }
        _events.trySend(event)
    }

    private fun setUpdateInProgressState() =
        _uiState.update { it.copy(updatingApp = true) }

    companion object {
        /** How long the splash waits for each answer of the store about updates. */
        const val UPDATE_CHECK_TIMEOUT_MS = 5_000L

        /** Pause before leaving the splash. */
        const val NAVIGATION_DELAY_MS = 200L
    }
}
