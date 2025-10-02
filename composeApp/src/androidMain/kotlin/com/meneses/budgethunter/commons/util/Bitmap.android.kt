package com.meneses.budgethunter.commons.util

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap

fun getBitmapFromPDFFileDescriptor(descriptor: ParcelFileDescriptor): Bitmap {
    val page = PdfRenderer(descriptor).openPage(0)
    val bitmap = createBitmap(page.width, page.height)
    val rect = Rect(0, page.height, page.width, 0)
    page.render(bitmap, rect, Matrix(), Page.RENDER_MODE_FOR_DISPLAY)
    return bitmap
}