package com.meneses.budgethunter.commons.platform

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.meneses.budgethunter.sms.platform.BankNotificationListenerService

interface PermissionsLauncherDelegate {
    fun launchPermissionsRequest(permissions: Array<String>)
    fun shouldShowSMSPermissionRationale(): Boolean
}

actual class PermissionsManager(private val context: Context) {

    private var launcherDelegate: PermissionsLauncherDelegate? = null
    private var permissionResultCallback: ((Boolean) -> Unit)? = null

    fun setLauncherDelegate(delegate: PermissionsLauncherDelegate) {
        this.launcherDelegate = delegate
    }

    actual fun shouldShowSMSPermissionRationale(): Boolean {
        return launcherDelegate?.shouldShowSMSPermissionRationale() ?: false
    }

    actual fun hasSmsPermission(): Boolean {
        val receiveSmsGranted = ContextCompat
            .checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED

        val readSmsGranted = ContextCompat
            .checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

        return receiveSmsGranted && readSmsGranted
    }

    actual fun requestSmsPermissions(callback: (granted: Boolean) -> Unit) {
        permissionResultCallback = callback
        val permissions = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        launcherDelegate?.launchPermissionsRequest(permissions.toTypedArray())
    }

    actual fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    actual fun hasNotificationListenerAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    actual fun openNotificationListenerSettings() {
        val component = ComponentName(context, BankNotificationListenerService::class.java)
        val detailIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Straight to this app's switch instead of the list of every app that asks for it
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
        } else {
            null
        }
        val listIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

        // Some manufacturers leave out the detail screen
        val intent = detailIntent?.takeIf { it.resolveActivity(context.packageManager) != null } ?: listIntent
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            openAppSettings()
        }
    }

    fun handlePermissionResult(granted: Boolean) {
        permissionResultCallback?.invoke(granted)
        permissionResultCallback = null
    }
}
