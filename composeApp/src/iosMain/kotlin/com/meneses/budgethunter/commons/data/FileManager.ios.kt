package com.meneses.budgethunter.commons.data

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
actual class FileManager {
    
    actual fun saveFile(fileData: FileData): String {
        val documentsDir = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).first() as String
        
        val filePath = "$documentsDir/${fileData.filename}"
        val data = fileData.data.toNSData()
        data.writeToFile(filePath, true)
        
        return filePath
    }
    
    actual fun deleteFile(filePath: String) {
        try {
            NSFileManager.defaultManager.removeItemAtPath(filePath, null)
        } catch (e: Exception) {
            // Handle error silently for KMP compatibility
        }
    }
    
    actual fun createUri(filePath: String): String {
        return "file://$filePath"
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(
        bytes = allocArrayOf(this@toNSData),
        length = this@toNSData.size.toULong()
    )
}
