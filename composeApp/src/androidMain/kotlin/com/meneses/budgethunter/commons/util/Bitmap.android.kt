package com.meneses.budgethunter.commons.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import kotlin.math.max
import kotlin.math.roundToInt

private const val DEFAULT_PDF_RENDER_WIDTH = 1600
private const val MAX_AI_IMAGE_SIDE = 2048

/**
 * Renders the first page of a PDF onto an opaque white bitmap.
 *
 * PdfRenderer only draws the page content and leaves everything else transparent, which turns black
 * when the bitmap is later compressed to JPEG (black text on black) or shown on a dark surface.
 * The page is also scaled up from its 72 dpi size so small print stays legible.
 * The descriptor and renderer are always closed.
 */
fun getBitmapFromPDFFileDescriptor(
    descriptor: ParcelFileDescriptor,
    targetWidth: Int = DEFAULT_PDF_RENDER_WIDTH
): Bitmap {
    descriptor.use {
        PdfRenderer(descriptor).use { renderer ->
            renderer.openPage(0).use { page ->
                val scale = targetWidth.toFloat() / page.width
                val width = targetWidth
                val height = (page.height * scale).roundToInt().coerceAtLeast(1)
                val bitmap = createBitmap(width, height)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, Matrix().apply { setScale(scale, scale) }, Page.RENDER_MODE_FOR_DISPLAY)
                return bitmap
            }
        }
    }
}

/**
 * Returns an opaque copy of this bitmap composited over white, downscaled so its longest side is at
 * most [maxSide]. Transparent pixels (PNG/WEBP/PDF) would otherwise become black in a JPEG.
 */
fun Bitmap.flattenOnWhite(maxSide: Int = MAX_AI_IMAGE_SIDE): Bitmap {
    val scale = (maxSide.toFloat() / max(width, height)).coerceAtMost(1f)
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    val result = createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawColor(Color.WHITE)
    canvas.drawBitmap(
        this,
        null,
        Rect(0, 0, targetWidth, targetHeight),
        Paint(Paint.FILTER_BITMAP_FLAG)
    )
    return result
}
