package com.meneses.budgethunter.commons.platform

expect class PermissionsManager {
    fun hasSmsPermission(): Boolean
    fun requestSmsPermissions(callback: (granted: Boolean) -> Unit)
    fun openAppSettings()
    fun shouldShowSMSPermissionRationale(): Boolean

    /** Whether the user let the app read other apps' notifications (Android's notification access). */
    fun hasNotificationListenerAccess(): Boolean

    /** Opens the system screen where that access is granted; it cannot be asked for in a dialog. */
    fun openNotificationListenerSettings()
}
