package com.meneses.budgethunter.commons.platform

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.appupdate.AppUpdateManager as GoogleAppUpdateManager

interface AppUpdateLauncherDelegate {
    fun startUpdateFlow(
        updateInfo: AppUpdateInfo,
        appUpdateManager: GoogleAppUpdateManager,
        requestCode: Int
    )
}

actual class AppUpdateManager(
    private val context: Context
) : IAppUpdateManager {

    private var googleAppUpdateManager: GoogleAppUpdateManager? = null
    private var launcherDelegate: AppUpdateLauncherDelegate? = null
    private var currentCallback: ((AppUpdateResult) -> Unit)? = null

    private val installStatusListener = InstallStateUpdatedListener { installState ->
        when (installState.installStatus()) {
            InstallStatus.DOWNLOADED -> googleAppUpdateManager?.completeUpdate()
            InstallStatus.INSTALLED, InstallStatus.FAILED, InstallStatus.CANCELED -> {
                finishUpdate()
            }
            else -> Unit
        }
    }

    fun setLauncherDelegate(delegate: AppUpdateLauncherDelegate) {
        this.launcherDelegate = delegate
    }

    override fun checkForUpdates(onResult: (AppUpdateResult) -> Unit) {
        currentCallback = onResult
        googleAppUpdateManager = AppUpdateManagerFactory.create(context)
        val updateInfoTask = googleAppUpdateManager!!.appUpdateInfo
        updateInfoTask.handleUpdateInfo()
    }

    private fun Task<AppUpdateInfo>.handleUpdateInfo() {
        addOnSuccessListener { updateInfo ->
            when (updateInfo.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    val updateResult = AppUpdateResult.UpdateAvailable {
                        startUpdate(updateInfo)
                    }
                    currentCallback?.invoke(updateResult)
                }
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    currentCallback?.invoke(AppUpdateResult.UpdateInProgress)
                }
                else -> {
                    currentCallback?.invoke(AppUpdateResult.NoUpdateAvailable)
                }
            }
        }

        addOnFailureListener {
            currentCallback?.invoke(AppUpdateResult.UpdateFailed)
        }
    }

    private fun startUpdate(updateInfo: AppUpdateInfo) {
        val manager = googleAppUpdateManager ?: return
        manager.registerListener(installStatusListener)
        launcherDelegate?.startUpdateFlow(
            updateInfo,
            manager,
            REQUEST_CODE_UPDATE_APP
        )
    }

    private fun finishUpdate() {
        googleAppUpdateManager?.unregisterListener(installStatusListener)
        currentCallback?.invoke(AppUpdateResult.NoUpdateAvailable)
    }

    companion object {
        const val REQUEST_CODE_UPDATE_APP = 10
    }
}
