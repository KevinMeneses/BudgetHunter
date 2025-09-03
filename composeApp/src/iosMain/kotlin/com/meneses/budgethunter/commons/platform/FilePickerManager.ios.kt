package com.meneses.budgethunter.commons.platform

import com.meneses.budgethunter.commons.data.FileData

actual class FilePickerManager {
    
    actual fun pickFile(mimeTypes: Array<String>, onResult: (FileData?) -> Unit) {
        // iOS implementation placeholder
        // This would integrate with UIDocumentPickerViewController
        println("FilePickerManager.pickFile() called - iOS implementation needed")
        onResult(null)
    }
}