package com.meneses.budgethunter.commons.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class AndroidPermissionsManager(
    private val context: Context
) : PermissionsManager {

    override fun hasSmsPermission(): Boolean {
        return ContextCompat
            .checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED
    }
}
