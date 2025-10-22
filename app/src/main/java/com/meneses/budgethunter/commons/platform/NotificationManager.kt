package com.meneses.budgethunter.commons.platform

interface NotificationManager {
    fun showToast(message: String)
    fun showError(message: String)
}
