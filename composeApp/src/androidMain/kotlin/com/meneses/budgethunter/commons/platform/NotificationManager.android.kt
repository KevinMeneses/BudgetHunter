package com.meneses.budgethunter.commons.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

actual class NotificationManager(
    private val context: Context
) {
    
    actual fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    actual fun showNotification(title: String, message: String) {
        // Check if we have notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                // Fall back to Toast if no permission
                showToast("$title: $message")
                return
            }
        }
        
        try {
            createNotificationChannel()
            
            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
            
            with(NotificationManagerCompat.from(context)) {
                notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
            }
        } catch (_: SecurityException) {
            // Fall back to Toast if notification fails
            showToast("$title: $message")
        }
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SMS Transactions",
            AndroidNotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for SMS transaction processing"
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    companion object {
        private const val CHANNEL_ID = "sms_transactions"
    }
}
