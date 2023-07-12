package com.meneses.budgethunter.splash

import android.content.Context
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.meneses.budgethunter.MainActivity
import com.meneses.budgethunter.splash.application.SplashEvent
import com.meneses.budgethunter.splash.application.SplashState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SplashScreenViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SplashState())
    val uiState = _uiState.asStateFlow()

    private lateinit var appUpdateManager: AppUpdateManager

    private val installStatusListener = InstallStateUpdatedListener {
        when (it.installStatus()) {
            InstallStatus.DOWNLOADED -> appUpdateManager.completeUpdate()
            else -> Unit
        }
    }

    fun sendEvent(event: SplashEvent) {
        when (event) {
            is SplashEvent.VerifyUpdate -> verifyUpdate(event.context)
        }
    }

    private fun verifyUpdate(context: Context) {
        appUpdateManager = AppUpdateManagerFactory.create(context)
        val updateInfoTask = appUpdateManager.appUpdateInfo
        updateInfoTask.handleUpdateInfo(context)
    }

    private fun Task<AppUpdateInfo>.handleUpdateInfo(context: Context) {
        addOnSuccessListener { updateInfo ->
            when (updateInfo.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE ->
                    startUpdate(updateInfo, context)

                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                    setUpdateInProgressState()

                else -> {
                    appUpdateManager.unregisterListener(installStatusListener)
                    setNavigateState()
                }
            }
        }

        addOnFailureListener { setNavigateState() }
    }

    private fun startUpdate(
        updateInfo: AppUpdateInfo,
        context: Context
    ) {
        appUpdateManager.registerListener(installStatusListener)
        appUpdateManager.startUpdateFlowForResult(
            updateInfo,
            context as MainActivity,
            AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
            REQUEST_CODE_UPDATE_APP
        )
    }

    private fun setNavigateState() =
        _uiState.update { it.copy(navigate = true) }

    private fun setUpdateInProgressState() =
        _uiState.update { it.copy(updatingApp = true) }

    companion object {
        private const val REQUEST_CODE_UPDATE_APP = 10
    }
}
