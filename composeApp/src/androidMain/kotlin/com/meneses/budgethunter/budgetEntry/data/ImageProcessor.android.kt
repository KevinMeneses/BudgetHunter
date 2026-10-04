package com.meneses.budgethunter.budgetEntry.data

import android.content.ContentResolver
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import com.meneses.budgethunter.budgetEntry.domain.ImageData
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.util.getBitmapFromPDFFileDescriptor

/**
 * Android-specific implementation of ImageProcessor.
 * Handles Android Bitmap creation from URIs and PDF processing.
 */
actual class ImageProcessor(
    private val contentResolver: ContentResolver,
    private val logger: Logger
) {
    private val tag = "ImageProcessor"

    actual fun getImageFromUri(imageData: ImageData): Any? {
        return try {
            if (imageData.isPdfFile()) {
                getPdfImage(imageData)
            } else {
                processRegularImageFromUri(imageData)
            }
        } catch (e: Exception) {
            logger.warn(tag, "Image processing error", e)
            null
        }
    }

    private fun getPdfImage(imageData: ImageData): Any? {
        return try {
            val uri = imageData.uri.toUri()
            val descriptor = contentResolver.openFileDescriptor(uri, "r")
                ?: return null

            getBitmapFromPDFFileDescriptor(descriptor)
        } catch (e: Exception) {
            logger.warn(tag, "PDF processing error", e)
            null
        }
    }

    private fun processRegularImageFromUri(imageData: ImageData): Any? {
        return try {
            val uri = imageData.uri.toUri()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            logger.warn(tag, "Regular image processing error", e)
            null
        }
    }
}
