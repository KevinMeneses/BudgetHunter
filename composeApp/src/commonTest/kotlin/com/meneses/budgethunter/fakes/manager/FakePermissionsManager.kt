package com.meneses.budgethunter.fakes.manager

import com.meneses.budgethunter.commons.platform.PermissionsManager

class FakePermissionsManager : PermissionsManager {
    var hasSms = false
    var shouldShowRationale = false
    var appSettingsOpened = false
    var permissionRequested = false
    var grantPermission = false

    override fun hasSmsPermission(): Boolean = hasSms
    override fun shouldShowSMSPermissionRationale(): Boolean = shouldShowRationale
    override fun requestSmsPermissions(onResult: (Boolean) -> Unit) {
        permissionRequested = true
        onResult(grantPermission)
    }
    override fun openAppSettings() {
        appSettingsOpened = true
    }
}
