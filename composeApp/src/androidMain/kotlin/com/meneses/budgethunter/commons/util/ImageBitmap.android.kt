package com.meneses.budgethunter.commons.util

import android.graphics.BitmapFactory
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/**
 * Android implementation for loading ImageBitmap from PDF file.
 * Falls back to regular image loading if PDF rendering fails.
 */
actual fun getImageBitmapFromPDFFile(filePath: String): ImageBitmap? {
    return try {
        val descriptor = ParcelFileDescriptor.open(
            File(filePath),
            ParcelFileDescriptor.MODE_READ_ONLY
        )

        getBitmapFromPDFFileDescriptor(descriptor).asImageBitmap()
    } catch (_: Exception) {
        getImageBitmapFromFile(filePath)
    }
}

/**
 * Android implementation for loading ImageBitmap from file path.
 * Used for displaying attached invoices in budget entry modal.
 */
actual fun getImageBitmapFromFile(filePath: String): ImageBitmap? {
    return try {
        val bitmap = BitmapFactory.decodeFile(filePath)
        bitmap?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}