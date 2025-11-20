package com.meneses.budgethunter.commons.platform

sealed class AppUpdateResult {
    data object NoUpdateAvailable : AppUpdateResult()
    data object UpdateInProgress : AppUpdateResult()
    data class UpdateAvailable(val startUpdate: () -> Unit) : AppUpdateResult()
    data object UpdateFailed : AppUpdateResult()
}

/**
 * Cross-platform app update manager interface.
 * Platform-specific implementations are provided in androidMain and iosMain.
 */
expect class AppUpdateManager() : IAppUpdateManager
