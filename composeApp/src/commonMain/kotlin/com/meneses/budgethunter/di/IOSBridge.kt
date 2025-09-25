package com.meneses.budgethunter.di

import com.meneses.budgethunter.commons.platform.CameraManager
import com.meneses.budgethunter.commons.platform.FilePickerManager

class IOSBridge {
    companion object {
        lateinit var cameraManager: CameraManager
        lateinit var filePickerManager: FilePickerManager
    }
}
