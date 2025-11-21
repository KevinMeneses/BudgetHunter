package com.meneses.budgethunter.commons.data

import androidx.core.net.toUri
import java.io.File
import java.io.IOException

/**
 * Android implementation of FileManager for budget entry invoice handling.
 */
actual class FileManager : IFileManager {

    override fun saveFile(fileData: FileData): String {
        return saveFile(fileData.data, fileData.directory, fileData.filename)
    }

    private fun saveFile(data: ByteArray, directory: String, filename: String): String {
        val dir = File(directory)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(directory, filename)
        file.outputStream().use { it.write(data) }
        return file.absolutePath
    }

    override fun deleteFile(filePath: String): Boolean {
        return try {
            File(filePath).delete()
        } catch (_: IOException) {
            false
        }
    }

    override fun createUri(filePath: String): String {
        return File(filePath).toUri().toString()
    }

    override fun fileExists(filePath: String): Boolean {
        return File(filePath).exists()
    }
}
