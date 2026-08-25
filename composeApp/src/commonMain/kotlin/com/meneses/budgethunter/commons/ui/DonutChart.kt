package com.meneses.budgethunter.commons.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.theme.AppColors

/**
 * Ring of slices sized by [proportions], with the headline of the whole thing in the hole.
 *
 * Slices are separated by a small gap so two neighbours never blend into a single shape, and
 * the ring starts at twelve o'clock so the biggest slice is where the eye lands first.
 */
@Composable
fun DonutChart(
    proportions: List<Float>,
    colors: List<Color>,
    centerLabel: String,
    centerValue: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 200.dp,
    thickness: Dp = 26.dp
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val strokeWidth = thickness.toPx()
            val gapDegrees = if (proportions.size > 1) 2f else 0f
            var startAngle = START_ANGLE

            proportions.forEachIndexed { index, proportion ->
                val sweepAngle = proportion * FULL_TURN
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle + gapDegrees / 2,
                    sweepAngle = (sweepAngle - gapDegrees).coerceAtLeast(0f),
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = Stroke(width = strokeWidth)
                )
                startAngle += sweepAngle
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = centerValue,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

private const val START_ANGLE = -90f
private const val FULL_TURN = 360f
