package com.meneses.budgethunter.fakes.manager

import com.meneses.budgethunter.commons.platform.IAppUpdateManager
import com.meneses.budgethunter.commons.platform.AppUpdateResult

class FakeAppUpdateManager(
    private val updateResult: AppUpdateResult
) : IAppUpdateManager {
    var checkForUpdatesCalled = false
    var startUpdateCalled = false

    override fun checkForUpdates(callback: (AppUpdateResult) -> Unit) {
        checkForUpdatesCalled = true
        callback(updateResult)
    }
}

fun FakeAppUpdateResult(
    onStart: () -> Unit = {}
): AppUpdateResult.UpdateAvailable {
    return AppUpdateResult.UpdateAvailable(startUpdate = onStart)
}
