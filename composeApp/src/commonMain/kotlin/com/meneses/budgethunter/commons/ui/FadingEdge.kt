package com.meneses.budgethunter.commons.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Fades the bottom [height] of the content out instead of cutting it off at the edge, so a
 * scrolling list ends softly. With [visible] false the fade goes away (animated): pass whether
 * the list can still scroll forward, so its last item is shown whole once it is reached.
 */
fun Modifier.fadingBottomEdge(visible: Boolean, height: Dp = 12.dp): Modifier = composed {
    val strength by animateFloatAsState(if (visible) 1f else 0f)
    graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            if (strength == 0f) return@drawWithContent
            val fade = height.toPx().coerceAtMost(size.height)
            val top = size.height - fade
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Black.copy(alpha = 1f - strength)),
                    startY = top,
                    endY = size.height
                ),
                topLeft = Offset(0f, top),
                size = Size(size.width, fade),
                blendMode = BlendMode.DstIn
            )
        }
}
