package com.meneses.budgethunter.commons.data

import android.content.ContentResolver
import android.net.Uri
import java.io.File

fun Uri.toFileData(
    contentResolver: ContentResolver,
    internalFilesDir: File?
): FileData {
    val data = contentResolver
        .openInputStream(this)
        ?.use { it.readBytes() }
        ?: byteArrayOf()
    
    val mimeType = contentResolver.getType(this)
    val fileFormat = when {
        mimeType?.startsWith("image/") == true -> {
            when {
                mimeType.contains("png") -> ".png"
                mimeType.contains("gif") -> ".gif"
                mimeType.contains("webp") -> ".webp"
                else -> ".jpg"
            }
        }
        mimeType == "application/pdf" -> ".pdf"
        else -> ".jpg" // Default fallback
    }
    val filename = System.currentTimeMillis().toString() + fileFormat
    
    return FileData(
        data = data,
        filename = filename,
        mimeType = mimeType,
        directory = internalFilesDir?.absolutePath.orEmpty()
    )
}