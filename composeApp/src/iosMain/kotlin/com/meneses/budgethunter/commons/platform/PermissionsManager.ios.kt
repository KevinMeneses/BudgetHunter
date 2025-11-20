package com.meneses.budgethunter.commons.platform

actual class PermissionsManager : IPermissionsManager {

    override fun shouldShowSMSPermissionRationale(): Boolean {
        // iOS doesn't have SMS permissions like Android
        return false
    }

    override fun hasSmsPermission(): Boolean {
        // iOS doesn't have SMS reading capabilities like Android
        // This feature would need to be handled differently on iOS
        return false
    }

    override fun requestSmsPermissions(callback: (granted: Boolean) -> Unit) {
        // iOS doesn't support SMS reading like Android
        // On iOS, this would need alternative implementation
        println("PermissionsManager.requestSmsPermissions() called - iOS doesn't support SMS reading")
        callback(false)
    }

    override fun openAppSettings() {
        // iOS implementation placeholder
        // This would open iOS Settings app using UIApplication.shared.open
        println("PermissionsManager.openAppSettings() called - iOS implementation needed")
    }
}
