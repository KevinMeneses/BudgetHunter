package com.meneses.budgethunter.commons.platform

expect class NotificationManager {
    fun showToast(message: String)
    fun showNotification(title: String, message: String)
}