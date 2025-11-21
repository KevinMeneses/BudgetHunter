package com.meneses.budgethunter.commons.platform

sealed class AppUpdateResult {
    data object NoUpdateAvailable : AppUpdateResult()
    data object UpdateInProgress : AppUpdateResult()
    data class UpdateAvailable(val startUpdate: () -> Unit) : AppUpdateResult()
    data object UpdateFailed : AppUpdateResult()
}

// Platform-specific implementations are provided in androidMain and iosMain
// Use IAppUpdateManager interface for dependency injection
