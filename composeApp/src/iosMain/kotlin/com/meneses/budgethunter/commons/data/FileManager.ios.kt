package com.meneses.budgethunter.commons.data

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create

/**
 * iOS implementation of FileManager for budget entry invoice handling.
 */
@OptIn(ExperimentalForeignApi::class)
actual class FileManager : IFileManager {

    override fun saveFile(fileData: FileData): String {
        val targetDirectory = if (fileData.directory.isNotEmpty()) {
            fileData.directory
        } else {
            getDefaultInvoicesDirectory()
        }

        // Ensure directory exists
        createDirectoryIfNeeded(targetDirectory)

        val filePath = "$targetDirectory/${fileData.filename}"
        println("FileManager.saveFile: Saving to $filePath")

        val data = fileData.data.toNSData()
        val success = data.writeToFile(filePath, true)

        if (success) {
            // Verify the file was actually saved
            val fileExists = NSFileManager.defaultManager.fileExistsAtPath(filePath)
            println("FileManager.saveFile: File saved successfully, exists: $fileExists")
            return filePath
        } else {
            throw Exception("Failed to save file to $filePath")
        }
    }

    override fun deleteFile(filePath: String): Boolean {
        return try {
            println("FileManager.deleteFile: Attempting to delete $filePath")

            // Check if file exists before trying to delete
            val fileExists = NSFileManager.defaultManager.fileExistsAtPath(filePath)
            if (!fileExists) {
                println("FileManager.deleteFile: File does not exist: $filePath")
                return false
            }

            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val success = NSFileManager.defaultManager.removeItemAtPath(filePath, error.ptr)

                if (success) {
                    println("FileManager.deleteFile: Successfully deleted $filePath")
                } else {
                    val errorMsg = error.value?.localizedDescription ?: "Unknown error"
                    println("FileManager.deleteFile: Failed to delete $filePath - $errorMsg")
                }

                success
            }
        } catch (e: Exception) {
            println("FileManager.deleteFile: Exception deleting $filePath - ${e.message}")
            false
        }
    }

    override fun createUri(filePath: String): String {
        return "file://$filePath"
    }

    /**
     * Validates if a file exists and is accessible
     */
    override fun fileExists(filePath: String): Boolean {
        val cleanPath = if (filePath.startsWith("file://")) {
            filePath.removePrefix("file://")
        } else {
            filePath
        }
        return NSFileManager.defaultManager.fileExistsAtPath(cleanPath)
    }

    private fun getInternalFilesDir(): String {
        return NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).first() as String
    }

    private fun getDefaultInvoicesDirectory(): String {
        val documentsDir = getInternalFilesDir()
        return "$documentsDir/BudgetHunter/Invoices"
    }

    private fun createDirectoryIfNeeded(directoryPath: String) {
        println("FileManager.createDirectoryIfNeeded: Checking directory $directoryPath")

        if (!NSFileManager.defaultManager.fileExistsAtPath(directoryPath)) {
            println("FileManager.createDirectoryIfNeeded: Creating directory $directoryPath")
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val success = NSFileManager.defaultManager.createDirectoryAtPath(
                    directoryPath,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = error.ptr
                )

                if (success) {
                    println("FileManager.createDirectoryIfNeeded: Successfully created directory")
                } else {
                    val errorMsg = error.value?.localizedDescription ?: "Unknown error"
                    println("FileManager.createDirectoryIfNeeded: Failed to create directory - $errorMsg")
                }
            }
        } else {
            println("FileManager.createDirectoryIfNeeded: Directory already exists")
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
