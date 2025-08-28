package com.meneses.budgethunter.commons.platform

interface PermissionsManager {
    fun hasSmsPermission(): Boolean
    fun requestSmsPermissions(callback: (granted: Boolean) -> Unit)
    fun openAppSettings()
    fun shouldShowSMSPermissionRationale(): Boolean
}
