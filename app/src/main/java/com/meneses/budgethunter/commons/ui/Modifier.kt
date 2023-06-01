package com.meneses.budgethunter.commons.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale

fun Modifier.pulsateEffect(
    isActive: Boolean,
    initialValue: Float = 1f,
    targetValue: Float = 1.2f,
    duration: Int = 500
): Modifier = composed {
    if (!isActive) return@composed this
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = initialValue,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(duration),
            repeatMode = RepeatMode.Reverse
        )
    )

    this.scale(scale)
}

fun Modifier.blinkEffect(
    isActive: Boolean,
    initialValue: Float = 0.3f,
    targetValue: Float = 1f,
    duration: Int = 500
): Modifier = composed {
    if (!isActive) return@composed this
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = initialValue,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(duration),
            repeatMode = RepeatMode.Reverse
        )
    )

    this.alpha(alpha)
}
