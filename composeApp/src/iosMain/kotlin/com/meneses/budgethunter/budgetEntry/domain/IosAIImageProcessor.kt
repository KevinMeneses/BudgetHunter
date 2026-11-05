package com.meneses.budgethunter.budgetEntry.domain

import com.meneses.budgethunter.budgetEntry.data.ImageProcessor
import com.meneses.budgethunter.budgetEntry.data.network.ReceiptApiService
import com.meneses.budgethunter.commons.data.sync.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlin.coroutines.cancellation.CancellationException
import platform.Foundation.NSData
import platform.Foundation.base64EncodedStringWithOptions
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

/**
 * iOS-specific implementation of AIImageProcessor.
 * Converts UIImage to base64 and sends it to the server through the shared ReceiptApiService.
 */
class IosAIImageProcessor(
    private val receiptApiService: ReceiptApiService,
    private val imageProcessor: ImageProcessor,
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger
) : AIImageProcessor {
    private val tag = "IosAIImageProcessor"

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun processImage(imageData: ImageData, today: LocalDate): AiExtractionResult =
        withContext(ioDispatcher) {
            try {
                // PDFs go as-is: the AI reads every page natively, which beats rendering only page 1
                if (imageData.isPdfFile()) {
                    imageProcessor.readFileAsBase64(imageData, MAX_INLINE_PDF_BYTES)?.let { base64Pdf ->
                        return@withContext receiptApiService.extract(base64Pdf, "application/pdf", today)
                    }
                    logger.info(tag, "PDF unreadable or too large to send as-is, rendering first page instead")
                }

                val uiImage = imageProcessor.getImageFromUri(imageData) as? UIImage
                    ?: return@withContext AiExtractionResult.Failure(AiFailureReason.FILE)

                val jpegData = UIImageJPEGRepresentation(uiImage, 0.9) as NSData
                val base64Image = jpegData.base64EncodedStringWithOptions(0u)

                receiptApiService.extract(base64Image, "image/jpeg", today)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(tag, "AI image processing error", e)
                AiExtractionResult.Failure(AiFailureReason.UNKNOWN)
            }
        }
}
