package com.meneses.budgethunter.commons.platform

import com.meneses.budgethunter.commons.data.FileData

actual class CameraManager {
    
    actual fun takePhoto(onResult: (FileData?) -> Unit) {
        // iOS implementation placeholder
        // This would integrate with UIImagePickerController or Camera framework
        println("CameraManager.takePhoto() called - iOS implementation needed")
        onResult(null)
    }
}