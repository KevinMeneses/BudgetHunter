package com.meneses.budgethunter.commons.data

import kotlinx.cinterop.*
import platform.Foundation.*

/**
 * iOS implementation of FileManager for budget entry invoice handling.
 */
@OptIn(ExperimentalForeignApi::class)
actual class FileManager {

    actual fun saveFile(fileData: FileData): String {
        val targetDirectory = if (fileData.directory.isNotEmpty()) {
            fileData.directory
        } else {
            getInternalFilesDir()
        }

        // Ensure directory exists
        createDirectoryIfNeeded(targetDirectory)

        val filePath = "$targetDirectory/${fileData.filename}"
        val data = fileData.data.toNSData()
        val success = data.writeToFile(filePath, true)

        return if (success) filePath else throw Exception("Failed to save file")
    }

    actual fun deleteFile(filePath: String): Boolean {
        return try {
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val success = NSFileManager.defaultManager.removeItemAtPath(filePath, error.ptr)
                success
            }
        } catch (_: Exception) {
            false
        }
    }

    actual fun createUri(filePath: String): String {
        return "file://$filePath"
    }

    private fun getInternalFilesDir(): String {
        return NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).first() as String
    }

    private fun createDirectoryIfNeeded(directoryPath: String) {
        if (!NSFileManager.defaultManager.fileExistsAtPath(directoryPath)) {
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                NSFileManager.defaultManager.createDirectoryAtPath(
                    directoryPath,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = error.ptr
                )
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(
        bytes = allocArrayOf(this@toNSData),
        length = this@toNSData.size.toULong()
    )
}
