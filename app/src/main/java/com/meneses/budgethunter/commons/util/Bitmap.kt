package com.meneses.budgethunter.commons.util

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page
import android.os.ParcelFileDescriptor
import java.io.File

fun getBitmapFromPDFFile(filePath: String): Bitmap {
    val descriptor = ParcelFileDescriptor.open(
        /* file = */ File(filePath),
        /* mode = */ ParcelFileDescriptor.MODE_READ_ONLY
    )
    return getBitmapFromPDFFileDescriptor(descriptor)
}

fun getBitmapFromPDFFileDescriptor(descriptor: ParcelFileDescriptor): Bitmap {
    val page = PdfRenderer(descriptor).openPage(0)
    val bitmap = Bitmap.createBitmap(
        /* width = */ page.width,
        /* height = */ page.height,
        /* config = */ Bitmap.Config.ARGB_8888
    )
    val rect = Rect(0, page.height, page.width, 0)
    page.render(bitmap, rect, Matrix(), Page.RENDER_MODE_FOR_DISPLAY)
    return bitmap
}
