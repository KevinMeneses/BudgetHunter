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

    /**
     * Whether the user lets this app post notifications. Android stops handing a listener the other
     * apps' notifications while its own are switched off, so the access alone is not enough.
     */
    fun areAppNotificationsEnabled(): Boolean

    /**
     * Whether the system has the notification listener bound right now. It can stay unbound with
     * the access granted, and then only a force stop of the app from its system screen brings it back.
     */
    fun isNotificationListenerConnected(): Boolean

    /** Opens the system screen with this app's notification switch. */
    fun openAppNotificationSettings()
}
