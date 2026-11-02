package com.meneses.budgethunter.budgetMetrics.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.chartCategoryColors

/**
 * What the metrics screen shows when the budget has no entries of [type]: the outline of the
 * donut the entries would fill, with the type's icon in the hole.
 *
 * The ring draws itself in and then turns slowly, so the screen does not look broken or frozen.
 */
@Composable
fun MetricsEmptyState(
    type: BudgetEntry.Type,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // A bit above the middle: centered exactly, the heavy ring makes the group look low
        Spacer(modifier = Modifier.weight(2f))

        GhostDonut(type = type)

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(3f))
    }
}

@Composable
private fun GhostDonut(type: BudgetEntry.Type) {
    // Restarted when switching between expenses and income, so each tab draws its own ring
    val drawIn = remember(type) { Animatable(0f) }
    LaunchedEffect(type) {
        drawIn.animateTo(1f, tween(durationMillis = 1_100, easing = FastOutSlowInEasing))
    }

    val transition = rememberInfiniteTransition(label = "ghostDonut")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 30_000, easing = LinearEasing)),
        label = "rotation"
    )
    val glow by transition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1_800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "glow"
    )

    // The first colors of the real donut, faded: a preview of what the entries will look like
    val sliceColors = chartCategoryColors().take(GHOST_SLICES.size)

    Box(contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .rotate(rotation)
        ) {
            val strokeWidth = 26.dp.toPx()
            val drawnDegrees = drawIn.value * FULL_TURN
            var startAngle = START_ANGLE

            GHOST_SLICES.forEachIndexed { index, proportion ->
                val sweep = proportion * FULL_TURN
                val visibleSweep = (drawnDegrees - (startAngle - START_ANGLE)).coerceIn(0f, sweep) - GAP_DEGREES
                if (visibleSweep > 0f) {
                    drawArc(
                        color = sliceColors[index].copy(alpha = glow),
                        startAngle = startAngle + GAP_DEGREES / 2,
                        sweepAngle = visibleSweep,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth)
                    )
                }
                startAngle += sweep
            }
        }

        Icon(
            imageVector = when (type) {
                BudgetEntry.Type.OUTCOME -> Icons.AutoMirrored.Outlined.ReceiptLong
                BudgetEntry.Type.INCOME -> Icons.Outlined.Savings
            },
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = AppColors.onSurface
        )
    }
}

/** Made-up shares, only to suggest the shape of a filled donut. */
private val GHOST_SLICES = listOf(0.38f, 0.26f, 0.2f, 0.16f)
private const val START_ANGLE = -90f
private const val FULL_TURN = 360f
private const val GAP_DEGREES = 3f
