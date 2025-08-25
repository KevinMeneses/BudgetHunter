package com.meneses.budgethunter.commons.platform

interface NotificationService {
    suspend fun showNotification(title: String, message: String)
}
