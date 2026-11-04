package com.meneses.budgethunter.commons.platform

expect class PermissionsManager {
    fun hasSmsPermission(): Boolean
    fun hasNotificationPermission(): Boolean
    fun requestSmsPermissions(callback: (granted: Boolean) -> Unit)
    fun openAppSettings()
    fun shouldShowSMSPermissionRationale(): Boolean
}
