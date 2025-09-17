package com.meneses.budgethunter

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.meneses.budgethunter.commons.platform.CameraLauncherDelegate
import com.meneses.budgethunter.commons.platform.CameraManager
import com.meneses.budgethunter.commons.platform.FilePickerLauncherDelegate
import com.meneses.budgethunter.commons.platform.FilePickerManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent, CameraLauncherDelegate, FilePickerLauncherDelegate {

    // Activity result launchers
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri>
    private lateinit var selectFileLauncher: ActivityResultLauncher<Array<String>>

    // Platform managers from Koin
    private val cameraManager: CameraManager by inject()
    private val filePickerManager: FilePickerManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup activity result launchers
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            cameraManager.handlePhotoResult(success)
        }

        selectFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            filePickerManager.handleFileResult(uri)
        }

        // Set this activity as the launcher delegate
        cameraManager.setLauncherDelegate(this)
        filePickerManager.setLauncherDelegate(this)

        setContent {
            BudgetHunterApp()
        }
    }

    // Implement CameraLauncherDelegate
    override fun launchCamera(uri: Uri) {
        takePhotoLauncher.launch(uri)
    }

    // Implement FilePickerLauncherDelegate
    override fun launchFilePicker(mimeTypes: Array<String>) {
        selectFileLauncher.launch(mimeTypes)
    }
}
