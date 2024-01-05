package com.meneses.budgethunter.splash

import com.meneses.budgethunter.splash.application.SplashEvent
import com.meneses.budgethunter.splash.application.SplashState
import com.meneses.budgethunter.splash.application.UpdateManager
import com.meneses.budgethunter.splash.application.getUpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import moe.tlaster.precompose.stateholder.SavedStateHolder
import moe.tlaster.precompose.viewmodel.ViewModel


fun splashScreenViewModel(): (SavedStateHolder) -> SplashScreenViewModel = {
    SplashScreenViewModel(getUpdateManager())
}

class SplashScreenViewModel(
    private val updateManager: UpdateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashState())
    val uiState = _uiState.asStateFlow()

    fun sendEvent(event: SplashEvent) {
        when (event) {
            is SplashEvent.VerifyUpdate ->
                updateManager.verifyUpdate(event.context) { newState ->
                    _uiState.update { newState }
                }
        }
    }
}
