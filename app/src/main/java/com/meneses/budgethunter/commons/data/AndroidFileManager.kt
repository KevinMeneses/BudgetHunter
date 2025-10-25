package com.meneses.budgethunter.commons.data

import androidx.core.net.toUri
import java.io.File
import java.io.IOException

class AndroidFileManager : FileManager {

    override fun saveFile(fileData: FileData): String {
        return saveFile(fileData.data, fileData.directory, fileData.filename)
    }

    private fun saveFile(data: ByteArray, directory: String, filename: String): String {
        val file = File(directory, filename)
        file.outputStream().use { it.write(data) }
        return file.absolutePath
    }

    override fun deleteFile(path: String): Boolean {
        return try {
            File(path).delete()
        } catch (e: IOException) {
            false
        }
    }

    override fun createUri(filePath: String): String {
        return File(filePath).toUri().toString()
    }
}
