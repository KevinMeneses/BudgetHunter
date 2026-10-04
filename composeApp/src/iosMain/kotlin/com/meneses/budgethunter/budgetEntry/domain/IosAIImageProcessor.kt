package com.meneses.budgethunter.budgetEntry.domain

import com.meneses.budgethunter.budgetEntry.data.ImageProcessor
import com.meneses.budgethunter.budgetEntry.data.remote.GeminiApiClient
import com.meneses.budgethunter.commons.data.sync.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
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
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger
) : AIImageProcessor {
    private val tag = "IosAIImageProcessor"

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun processImage(imageData: ImageData, prompt: String): AiExtractionResult =
        withContext(ioDispatcher) {
            try {
                // PDFs go as-is: Gemini reads every page natively, which beats rendering only page 1
                if (imageData.isPdfFile()) {
                    imageProcessor.readFileAsBase64(imageData, MAX_INLINE_PDF_BYTES)?.let { base64Pdf ->
                        return@withContext geminiApiClient.extract(base64Pdf, "application/pdf", prompt)
                    }
                    logger.info(tag, "PDF unreadable or too large to send as-is, rendering first page instead")
                }

                val uiImage = imageProcessor.getImageFromUri(imageData) as? UIImage
                    ?: return@withContext AiExtractionResult.Failure(AiFailureReason.FILE)

                val jpegData = UIImageJPEGRepresentation(uiImage, 0.9) as NSData
                val base64Image = jpegData.base64EncodedStringWithOptions(0u)

                geminiApiClient.extract(base64Image, "image/jpeg", prompt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(tag, "AI image processing error", e)
                AiExtractionResult.Failure(AiFailureReason.UNKNOWN)
            }
        }
}
