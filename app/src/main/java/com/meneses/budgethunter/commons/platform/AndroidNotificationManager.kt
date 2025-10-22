package com.meneses.budgethunter.commons.platform

import android.content.Context
import android.widget.Toast

class AndroidNotificationManager(
    private val context: Context
) : NotificationManager {

    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
