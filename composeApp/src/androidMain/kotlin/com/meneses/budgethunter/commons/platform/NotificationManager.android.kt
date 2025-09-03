package com.meneses.budgethunter.commons.platform

import android.content.Context
import android.widget.Toast

actual class NotificationManager(
    private val context: Context
) {
    
    actual fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    actual fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}