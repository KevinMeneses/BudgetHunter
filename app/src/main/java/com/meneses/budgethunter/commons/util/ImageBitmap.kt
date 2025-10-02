package com.meneses.budgethunter.commons.util

import android.graphics.BitmapFactory
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

// create expect/actual function for KMP and add iOS implementation on migration
fun getImageBitmapFromPDFFile(filePath: String): ImageBitmap? {
    return try {
        val descriptor = ParcelFileDescriptor.open(
            /* file = */ File(filePath),
            /* mode = */ ParcelFileDescriptor.MODE_READ_ONLY
        )

        getBitmapFromPDFFileDescriptor(descriptor).asImageBitmap()
    } catch (_: Exception) {
        getImageBitmapFromFile(filePath)
    }
}

// create expect/actual function for KMP and add iOS implementation on migration
fun getImageBitmapFromFile(filePath: String): ImageBitmap? {
    return try {
        val bitmap = BitmapFactory.decodeFile(filePath)
        bitmap?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
