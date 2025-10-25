package com.meneses.budgethunter

import android.Manifest
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.meneses.budgethunter.commons.platform.AndroidAppUpdateManager
import com.meneses.budgethunter.commons.platform.AndroidCameraManager
import com.meneses.budgethunter.commons.platform.AndroidFilePickerManager
import com.meneses.budgethunter.commons.platform.AndroidPermissionsManager
import com.meneses.budgethunter.commons.platform.AppUpdateLauncherDelegate
import com.meneses.budgethunter.commons.platform.AppUpdateManager
import com.meneses.budgethunter.commons.platform.CameraLauncherDelegate
import com.meneses.budgethunter.commons.platform.CameraManager
import com.meneses.budgethunter.commons.platform.FilePickerLauncherDelegate
import com.meneses.budgethunter.commons.platform.FilePickerManager
import com.meneses.budgethunter.commons.platform.LifecycleManager
import com.meneses.budgethunter.commons.platform.PermissionsLauncherDelegate
import com.meneses.budgethunter.commons.platform.PermissionsManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.google.android.play.core.appupdate.AppUpdateManager as GoogleAppUpdateManager

class MainActivity : ComponentActivity(), KoinComponent, CameraLauncherDelegate, FilePickerLauncherDelegate, PermissionsLauncherDelegate, AppUpdateLauncherDelegate {

    // Activity result launchers
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri>
    private lateinit var selectFileLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>

    // Platform managers from Koin
    private val cameraManager: CameraManager by inject()
    private val filePickerManager: FilePickerManager by inject()
    private val permissionsManager: PermissionsManager by inject()
    private val appUpdateManager: AppUpdateManager by inject()
    private val lifecycleManager: LifecycleManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup activity result launchers
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            (cameraManager as AndroidCameraManager).handlePhotoResult(success)
        }

        selectFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            (filePickerManager as AndroidFilePickerManager).handleFileResult(uri)
        }

        permissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val smsGranted = permissions["android.permission.RECEIVE_SMS"] ?: false
            (permissionsManager as AndroidPermissionsManager).handlePermissionResult(smsGranted)
        }

        // Set this activity as the launcher delegate
        (cameraManager as AndroidCameraManager).setLauncherDelegate(this)
        (filePickerManager as AndroidFilePickerManager).setLauncherDelegate(this)
        (permissionsManager as AndroidPermissionsManager).setLauncherDelegate(this)
        (appUpdateManager as AndroidAppUpdateManager).setLauncherDelegate(this)

        setContent { App() }
    }

    override fun onStart() {
        lifecycleManager.onStart()
        super.onStart()
    }

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
            /* activity = */ this,
            /* permission = */ Manifest.permission.RECEIVE_SMS
        )
    }

    // Implement AppUpdateLauncherDelegate
    override fun startUpdateFlow(
        updateInfo: AppUpdateInfo,
        appUpdateManager: GoogleAppUpdateManager,
        requestCode: Int
    ) {
        appUpdateManager.startUpdateFlowForResult(
            updateInfo,
            this,
            AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
            requestCode
        )
    }
}
