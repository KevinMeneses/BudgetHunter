package com.meneses.budgethunter.budgetEntry.domain

import com.meneses.budgethunter.budgetEntry.data.ImageProcessor
import com.meneses.budgethunter.budgetEntry.data.remote.GeminiApiClient
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.base64EncodedStringWithOptions
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

/**
 * iOS-specific implementation of AIImageProcessor.
 * Converts UIImage to base64 and delegates to shared GeminiApiClient.
 */
class IosAIImageProcessor(
    private val geminiApiClient: GeminiApiClient,
    private val imageProcessor: ImageProcessor,
    private val ioDispatcher: CoroutineDispatcher
) : AIImageProcessor {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun processImage(imageData: ImageData, prompt: String): BudgetEntry? = withContext(ioDispatcher) {
        try {
            // Get UIImage from the image URI
            val uiImage = imageProcessor.getImageFromUri(imageData) as? UIImage
                ?: return@withContext null

            // Convert UIImage to base64-encoded JPEG
            val imageData = UIImageJPEGRepresentation(uiImage, 0.8) as NSData
            val base64Image = imageData.base64EncodedStringWithOptions(0u)

            // Use shared Gemini API client
            geminiApiClient.extractBudgetEntryFromImage(base64Image, prompt)
        } catch (e: Exception) {
            println("iOS AI Image Processing Error: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
