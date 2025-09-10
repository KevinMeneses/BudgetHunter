package com.meneses.budgethunter.budgetEntry.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.meneses.budgethunter.budgetEntry.domain.ImageData

/**
 * Android-specific implementation of ImageProcessor.
 * Handles Android Bitmap creation from URIs and PDF processing.
 */
actual class ImageProcessor(
    private val contentResolver: ContentResolver
) {
    
    actual fun getImageFromUri(imageData: ImageData): Any? {
        return try {
            if (imageData.isPdfFile()) {
                getPdfImage(imageData)
            } else {
                processRegularImageFromUri(imageData)
            }
        } catch (e: Exception) {
            println("Android Image Processing Error: ${e.message}")
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
            println("Android PDF Processing Error: ${e.message}")
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
            println("Android Regular Image Processing Error: ${e.message}")
            null
        }
    }

    /**
     * Converts PDF file descriptor to Bitmap.
     * This preserves the exact same logic from the original Android implementation.
     */
    private fun getBitmapFromPDFFileDescriptor(descriptor: ParcelFileDescriptor): Bitmap {
        val page = PdfRenderer(descriptor).openPage(0)
        val bitmap = createBitmap(page.width, page.height)
        val rect = Rect(0, page.height, page.width, 0)
        page.render(bitmap, rect, Matrix(), Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }
}
