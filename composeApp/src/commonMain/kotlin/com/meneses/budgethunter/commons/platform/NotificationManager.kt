package com.meneses.budgethunter.commons.platform

expect class NotificationManager {
    fun showToast(message: String)
    fun showError(message: String)
}