package com.meneses.budgethunter.commons.platform

actual class AppUpdateManager {
    
    actual fun checkForUpdates(onResult: (AppUpdateResult) -> Unit) {
        // iOS implementation placeholder
        // iOS updates are handled through the App Store automatically
        // This could integrate with StoreKit for app review prompts
        println("AppUpdateManager.checkForUpdates() called - iOS implementation needed")
        onResult(AppUpdateResult.NoUpdateAvailable)
    }
}