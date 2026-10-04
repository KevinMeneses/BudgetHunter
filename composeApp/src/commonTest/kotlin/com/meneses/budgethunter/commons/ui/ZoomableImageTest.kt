package com.meneses.budgethunter.commons.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals

class ZoomableImageTest {

    private val container = Size(400f, 600f)
    private val center = Offset(200f, 300f)

    @Test
    fun `zooming around the center keeps the image centered`() {
        val result = zoomAround(1f, Offset.Zero, center, Offset.Zero, 2f, container)

        assertEquals(2f, result.scale)
        assertEquals(Offset.Zero, result.offset)
    }

    @Test
    fun `zooming around the top left corner keeps that corner in place`() {
        val result = zoomAround(1f, Offset.Zero, Offset.Zero, Offset.Zero, 2f, container)

        // Content point at the container's top-left must still be at the top-left after scaling
        assertEquals(2f, result.scale)
        assertEquals(Offset(200f, 300f), result.offset)
    }

    @Test
    fun `scale is clamped between 1x and 5x`() {
        assertEquals(5f, zoomAround(4f, Offset.Zero, center, Offset.Zero, 10f, container).scale)
        assertEquals(1f, zoomAround(2f, Offset.Zero, center, Offset.Zero, 0.1f, container).scale)
    }

    @Test
    fun `cannot pan when not zoomed`() {
        val result = zoomAround(1f, Offset.Zero, center, Offset(80f, -50f), 1f, container)

        assertEquals(Offset.Zero, result.offset)
    }

    @Test
    fun `pan is limited to the zoomed image edges`() {
        // At 2x the image can move at most half the container size in each direction
        val result = zoomAround(2f, Offset.Zero, center, Offset(1000f, -1000f), 1f, container)

        assertEquals(Offset(200f, -300f), result.offset)
    }

    @Test
    fun `zooming back out to 1x recenters the image`() {
        val zoomedAndPanned = zoomAround(2f, Offset(150f, -100f), center, Offset.Zero, 1f, container)
        val result = zoomAround(zoomedAndPanned.scale, zoomedAndPanned.offset, center, Offset.Zero, 0.5f, container)

        assertEquals(1f, result.scale)
        assertEquals(Offset.Zero, result.offset)
    }
}
