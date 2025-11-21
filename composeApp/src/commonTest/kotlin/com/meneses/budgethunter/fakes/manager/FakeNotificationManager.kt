package com.meneses.budgethunter.fakes.manager

import com.meneses.budgethunter.commons.platform.NotificationManager

class FakeNotificationManager : NotificationManager {
    val notifications = mutableListOf<Pair<String, String>>()
    val toasts = mutableListOf<String>()

    override fun showNotification(title: String, message: String) {
        notifications.add(title to message)
    }

    override fun showToast(message: String) {
        toasts.add(message)
    }
}
