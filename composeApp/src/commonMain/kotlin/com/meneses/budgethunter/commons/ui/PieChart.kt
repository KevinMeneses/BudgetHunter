package com.meneses.budgethunter.commons.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun PieChart(
    data: Map<String, Double>,
    colors: List<Color>,
    percentages: List<Double>,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(fontSize = 14.sp, color = Color.Black)
) {
    val total = data.values.sum()
    val proportions = data.mapValues { it.value / total }
    val angles = proportions.mapValues { it.value * 360f }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.52f)
        ) {
            var startAngle = 180f
            angles.onEachIndexed { index, entry ->
                val sweepAngle = entry.value.toFloat()
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )
                startAngle += sweepAngle
            }
        }

        // Adding labels
        Column(
            modifier = Modifier.padding(top = 26.dp)
        ) {
            data.keys.forEachIndexed { index, label ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(12.dp)) {
                            drawRect(
                                color = colors[index % colors.size],
                                size = Size(size.minDimension, size.minDimension)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$label:",
                            style = textStyle,
                            fontSize = 18.sp
                        )
                    }

                    val percentage = percentages[index]
                    val formattedPercentage = (percentage * 10).roundToInt() / 10.0
                    
                    Text(
                        text = "$formattedPercentage%",
                        style = textStyle,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
