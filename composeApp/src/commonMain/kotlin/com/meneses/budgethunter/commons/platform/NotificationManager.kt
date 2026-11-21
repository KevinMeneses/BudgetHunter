package com.meneses.budgethunter.commons.platform

interface NotificationManager {
    fun showToast(message: String)
    fun showNotification(title: String, message: String)

    /** Like [showNotification], but tapping it opens the default budget. */
    fun showEntryAddedNotification(title: String, message: String)

    /** Like [showNotification], but tapping it opens the settings screen. */
    fun showOpenSettingsNotification(title: String, message: String)

    /**
     * Like [showOpenSettingsNotification], for the bank notification reader being stopped: always
     * the same notification, so a new one replaces it and other notifications never do.
     */
    fun showReaderStoppedNotification(title: String, message: String)

    /** Removes the notification of [showReaderStoppedNotification], if it is still shown. */
    fun cancelReaderStoppedNotification()
}
