package com.meneses.budgethunter.commons.application

import com.meneses.budgethunter.commons.data.FileManager
import com.meneses.budgethunter.commons.data.sync.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Use case to validate file paths and handle missing files gracefully.
 * Essential for iOS development where files are lost during clean builds.
 */
class ValidateFilePathUseCase(
    private val fileManager: FileManager,
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger
) {
    private val tag = "ValidateFilePathUseCase"

    suspend fun execute(filePath: String?): String? = withContext(ioDispatcher) {
        if (filePath.isNullOrEmpty()) return@withContext null

        return@withContext try {
            if (fileManager.fileExists(filePath)) {
                filePath
            } else {
                logger.warn(tag, "File not found: $filePath")
                null
            }
        } catch (e: Exception) {
            logger.warn(tag, "Error validating file path", e)
            null
        }
    }
}
