package com.meneses.budgethunter.fakes.manager

import com.meneses.budgethunter.commons.platform.AppUpdateManager
import com.meneses.budgethunter.commons.platform.AppUpdateResult

class FakeAppUpdateManager(
    private val updateResult: AppUpdateResult
) : AppUpdateManager {
    var checkForUpdatesCalled = false
    var startUpdateCalled = false

    override fun checkForUpdates(callback: (AppUpdateResult) -> Unit) {
        checkForUpdatesCalled = true
        callback(updateResult)
    }
}

class FakeAppUpdateResult(
    private val onStart: () -> Unit = {}
) : AppUpdateResult.UpdateAvailable {
    override fun startUpdate() {
        onStart()
    }
}
