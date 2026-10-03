package com.meneses.budgethunter.commons.platform

interface NotificationManager {
    fun showToast(message: String)
    fun showNotification(title: String, message: String)

    /** Like [showNotification], but tapping it opens the default budget. */
    fun showEntryAddedNotification(title: String, message: String)
}
