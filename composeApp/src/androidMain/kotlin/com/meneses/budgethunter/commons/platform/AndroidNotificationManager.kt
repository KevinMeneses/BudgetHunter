package com.meneses.budgethunter.commons.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.PendingIntent
import android.app.NotificationManager as AndroidNotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.meneses.budgethunter.MainActivity

class AndroidNotificationManager(
    private val context: Context
) : NotificationManager {

    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun showNotification(title: String, message: String) {
        // Check if we have notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                // Fall back to Toast if no permission
                showToast("$title: $message")
                return
            }
        }

        try {
            createNotificationChannel()

            // Create intent to open the app when notification is tapped
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                999,
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Use app icon when available
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            with(NotificationManagerCompat.from(context)) {
                notify(INFO_NOTIFICATION_ID, notificationBuilder.build())
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
        private const val INFO_NOTIFICATION_ID = 1002
    }
}
