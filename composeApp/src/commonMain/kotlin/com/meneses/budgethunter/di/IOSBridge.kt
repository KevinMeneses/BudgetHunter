package com.meneses.budgethunter.di

import com.meneses.budgethunter.commons.platform.CameraManager
import com.meneses.budgethunter.commons.platform.FilePickerManager
import com.meneses.budgethunter.commons.platform.ShareManager

class IOSBridge {
    companion object {
        lateinit var cameraManager: CameraManager
        lateinit var filePickerManager: FilePickerManager
        lateinit var shareManager: ShareManager
    }
}
