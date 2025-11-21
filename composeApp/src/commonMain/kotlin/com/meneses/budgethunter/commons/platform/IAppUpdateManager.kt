package com.meneses.budgethunter.commons.platform

interface IAppUpdateManager {
    fun checkForUpdates(onResult: (AppUpdateResult) -> Unit)
}
