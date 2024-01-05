package com.meneses.budgethunter.splash.application

import android.app.Activity
import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.meneses.budgethunter.splash.SplashScreenViewModel
import kotlinx.coroutines.flow.update


private const val REQUEST_CODE_UPDATE_APP = 10

actual fun getUpdateManager() = object : UpdateManager {

    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var onStateUpdated: (SplashState) -> Unit
    private val state = SplashState()

    private val installStatusListener = InstallStateUpdatedListener {
        when (it.installStatus()) {
            InstallStatus.DOWNLOADED -> appUpdateManager.completeUpdate()
            InstallStatus.INSTALLED, InstallStatus.FAILED, InstallStatus.CANCELED -> finishUpdate()
            else -> Unit
        }
    }

    private fun Task<AppUpdateInfo>.handleUpdateInfo(context: Context) {
        addOnSuccessListener { updateInfo ->
            when (updateInfo.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> startUpdate(updateInfo, context)
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> setUpdateInProgressState()
                else -> finishUpdate()
            }
        }

        addOnFailureListener { setNavigateState() }
    }

    private fun finishUpdate() {
        appUpdateManager.unregisterListener(installStatusListener)
        setNavigateState()
    }

    private fun startUpdate(
        updateInfo: AppUpdateInfo,
        context: Context
    ) {
        appUpdateManager.registerListener(installStatusListener)
        appUpdateManager.startUpdateFlowForResult(
            updateInfo,
            context as Activity,
            AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
            REQUEST_CODE_UPDATE_APP
        )
    }

    override fun verifyUpdate(context: Any, callback: (SplashState) -> Unit) {
        appUpdateManager = AppUpdateManagerFactory.create(context as Context)
        onStateUpdated = callback
        val updateInfoTask = appUpdateManager.appUpdateInfo
        updateInfoTask.handleUpdateInfo(context)
    }

    private fun setNavigateState() =
        onStateUpdated(state.copy(navigate = true))

    private fun setUpdateInProgressState() =
        onStateUpdated(state.copy(updatingApp = true))
}
