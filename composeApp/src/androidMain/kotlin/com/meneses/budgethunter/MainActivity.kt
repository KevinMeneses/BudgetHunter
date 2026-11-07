package com.meneses.budgethunter

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.meneses.budgethunter.commons.platform.AndroidCameraManager
import com.meneses.budgethunter.commons.platform.AndroidFilePickerManager
import com.meneses.budgethunter.commons.platform.AndroidGoogleSignInManager
import com.meneses.budgethunter.commons.platform.AndroidNotificationManager
import com.meneses.budgethunter.commons.platform.CameraLauncherDelegate
import com.meneses.budgethunter.commons.platform.FilePickerLauncherDelegate
import com.meneses.budgethunter.commons.platform.GoogleSignInActivityDelegate
import com.meneses.budgethunter.commons.platform.PermissionsLauncherDelegate
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.navigation.DefaultBudgetLaunchRequest
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity :
    ComponentActivity(),
    KoinComponent,
    CameraLauncherDelegate,
    FilePickerLauncherDelegate,
    PermissionsLauncherDelegate,
    GoogleSignInActivityDelegate {

    // Activity result launchers
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri>
    private lateinit var selectFileLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>

    // Platform managers from Koin - need concrete types for delegate setup
    private val cameraManager: AndroidCameraManager by inject()
    private val filePickerManager: AndroidFilePickerManager by inject()
    private val permissionsManager: PermissionsManager by inject()
    private val googleSignInManager: AndroidGoogleSignInManager by inject()
    private val defaultBudgetLaunchRequest: DefaultBudgetLaunchRequest by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup activity result launchers
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            cameraManager.handlePhotoResult(success)
        }

        selectFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            filePickerManager.handleFileResult(uri)
        }

        permissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val smsGranted = permissions["android.permission.RECEIVE_SMS"] ?: false
            permissionsManager.handlePermissionResult(smsGranted)
        }

        // Set this activity as the launcher delegate
        cameraManager.setLauncherDelegate(this)
        filePickerManager.setLauncherDelegate(this)
        permissionsManager.setLauncherDelegate(this)
        googleSignInManager.setActivityDelegate(this)

        // Not gated on savedInstanceState: when the process was killed in the background, the
        // system recreates the Activity with the tapped notification's intent AND a saved state.
        // handleLaunchIntent consumes the extras, so a recreation does not handle them twice.
        handleLaunchIntent(intent)

        setContent {
            BudgetHunterApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(AndroidNotificationManager.EXTRA_OPEN_DEFAULT_BUDGET, false)) {
            defaultBudgetLaunchRequest.request()
        }
        if (intent.getBooleanExtra(AndroidNotificationManager.EXTRA_OPEN_SETTINGS, false)) {
            defaultBudgetLaunchRequest.requestSettings()
        }
        intent.removeExtra(AndroidNotificationManager.EXTRA_OPEN_DEFAULT_BUDGET)
        intent.removeExtra(AndroidNotificationManager.EXTRA_OPEN_SETTINGS)
    }

    override fun onDestroy() {
        // The manager is a singleton, so a delegate left pointing here would leak this Activity
        // across every configuration change.
        googleSignInManager.setActivityDelegate(null)
        super.onDestroy()
    }

    // Implement GoogleSignInActivityDelegate
    override fun currentActivity(): Activity = this

    // Implement CameraLauncherDelegate
    override fun launchCamera(uri: Uri) {
        takePhotoLauncher.launch(uri)
    }

    // Implement FilePickerLauncherDelegate
    override fun launchFilePicker(mimeTypes: Array<String>) {
        selectFileLauncher.launch(mimeTypes)
    }

    // Implement PermissionsLauncherDelegate
    override fun launchPermissionsRequest(permissions: Array<String>) {
        permissionsLauncher.launch(permissions)
    }

    override fun shouldShowSMSPermissionRationale(): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.RECEIVE_SMS
        )
    }
}
