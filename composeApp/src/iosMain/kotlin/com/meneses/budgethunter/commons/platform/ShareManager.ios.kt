package com.meneses.budgethunter.commons.platform

actual class ShareManager {
    
    actual fun shareFile(filePath: String, mimeTypes: Array<String>) {
        // iOS implementation placeholder
        // This would integrate with UIActivityViewController for sharing
        println("ShareManager.shareFile() called with path: $filePath - iOS implementation needed")
    }
}