package com.meneses.budgethunter.budgetEntry.domain

import android.graphics.Bitmap
import android.util.Base64
import com.meneses.budgethunter.budgetEntry.data.ImageProcessor
import com.meneses.budgethunter.budgetEntry.data.remote.GeminiApiClient
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.util.flattenOnWhite
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.cancellation.CancellationException

/**
 * Android-specific implementation of AIImageProcessor.
 * Converts Android Bitmap to base64 and delegates to shared GeminiApiClient.
 */
class AndroidAIImageProcessor(
    private val geminiApiClient: GeminiApiClient,
    private val imageProcessor: ImageProcessor,
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger
) : AIImageProcessor {
    private val tag = "AndroidAIImageProcessor"

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

                val bitmap = imageProcessor.getImageFromUri(imageData) as? Bitmap
                    ?: return@withContext AiExtractionResult.Failure(AiFailureReason.FILE)

                // Flatten on white first: transparent pixels would turn black in JPEG
                val outputStream = ByteArrayOutputStream()
                bitmap.flattenOnWhite().compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

                geminiApiClient.extract(base64Image, "image/jpeg", prompt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(tag, "AI image processing error", e)
                AiExtractionResult.Failure(AiFailureReason.UNKNOWN)
            }
        }
}
