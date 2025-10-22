package com.meneses.budgethunter.commons.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

interface PermissionsLauncherDelegate {
    fun launchPermissionsRequest(permissions: Array<String>)
    fun shouldShowSMSPermissionRationale(): Boolean
}

class AndroidPermissionsManager(private val context: Context) : PermissionsManager {

    private var launcherDelegate: PermissionsLauncherDelegate? = null
    private var permissionResultCallback: ((Boolean) -> Unit)? = null

    fun setLauncherDelegate(delegate: PermissionsLauncherDelegate) {
        this.launcherDelegate = delegate
    }

    override fun shouldShowSMSPermissionRationale(): Boolean {
        return launcherDelegate?.shouldShowSMSPermissionRationale() ?: false
    }

    override fun hasSmsPermission(): Boolean {
        return ContextCompat
            .checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
    }

    override fun requestSmsPermissions(callback: (granted: Boolean) -> Unit) {
        permissionResultCallback = callback
        val permissions = mutableListOf(Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        launcherDelegate?.launchPermissionsRequest(permissions.toTypedArray())
    }

    override fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun handlePermissionResult(granted: Boolean) {
        permissionResultCallback?.invoke(granted)
        permissionResultCallback = null
    }
}
