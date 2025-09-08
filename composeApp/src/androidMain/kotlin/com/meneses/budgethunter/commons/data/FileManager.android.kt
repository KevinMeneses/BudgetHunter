package com.meneses.budgethunter.commons.data

import androidx.core.net.toUri
import java.io.File
import java.io.IOException

actual class FileManager {
    
    actual fun saveFile(fileData: FileData): String {
        return saveFile(fileData.data, fileData.directory, fileData.filename)
    }

    private fun saveFile(data: ByteArray, directory: String, filename: String): String {
        val file = File(directory, filename)
        file.outputStream().use { it.write(data) }
        return file.absolutePath
    }
    
    actual fun deleteFile(filePath: String) {
        try {
            File(filePath).delete()
        } catch (_: IOException) {
            // Handle error silently for KMP compatibility
        }
    }
    
    actual fun createUri(filePath: String): String {
        return File(filePath).toUri().toString()
    }
}
