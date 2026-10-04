package com.meneses.budgethunter.commons.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

/**
 * Image with pinch-to-zoom, pan while zoomed and double-tap to toggle zoom.
 * The zoom resets when [bitmap] changes.
 */
@Composable
fun ZoomableImage(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var scale by remember(bitmap) { mutableStateOf(MIN_SCALE) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(Size.Zero) }

    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { containerSize = it.toSize() }
            .pointerInput(bitmap) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        val target = if (scale > MIN_SCALE) MIN_SCALE else DOUBLE_TAP_SCALE
                        val result = zoomAround(scale, offset, tap, Offset.Zero, target / scale, containerSize)
                        scale = result.scale
                        offset = result.offset
                    }
                )
            }
            .pointerInput(bitmap) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val result = zoomAround(scale, offset, centroid, pan, zoom, containerSize)
                    scale = result.scale
                    offset = result.offset
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
    )
}

internal data class ZoomResult(val scale: Float, val offset: Offset)

/**
 * Applies a zoom/pan gesture keeping the content point under [centroid] fixed, then clamps the
 * scale to [MIN_SCALE]..[MAX_SCALE] and the offset so the image can't be dragged out of [container].
 * The layer scales around the container's center, so the offset is relative to it.
 */
internal fun zoomAround(
    scale: Float,
    offset: Offset,
    centroid: Offset,
    pan: Offset,
    zoom: Float,
    container: Size
): ZoomResult {
    val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
    val effectiveZoom = newScale / scale
    val center = Offset(container.width / 2f, container.height / 2f)

    val raw = (centroid - center) * (1f - effectiveZoom) + offset * effectiveZoom + pan

    val maxX = (container.width * (newScale - 1f) / 2f).coerceAtLeast(0f)
    val maxY = (container.height * (newScale - 1f) / 2f).coerceAtLeast(0f)
    return ZoomResult(
        scale = newScale,
        offset = Offset(raw.x.coerceIn(-maxX, maxX), raw.y.coerceIn(-maxY, maxY))
    )
}
