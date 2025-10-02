package com.meneses.budgethunter.commons.ui

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DefDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    thickness: Dp = 1.dp
) {
    HorizontalDivider(
        modifier = modifier,
        color = color,
        thickness = thickness
    )
}