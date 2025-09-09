package com.meneses.budgethunter.commons.platform

actual class NotificationManager {
    
    actual fun showToast(message: String) {
        // iOS implementation placeholder
        // iOS doesn't have native toast messages like Android
        // This could be implemented with custom UI or alerts
        println("NotificationManager.showToast(): $message")
    }
    
    actual fun showNotification(title: String, message: String) {
        // iOS implementation placeholder
        // This would typically use UserNotifications framework
        println("NotificationManager.showNotification(): $title - $message")
    }
}