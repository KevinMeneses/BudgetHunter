package com.meneses.budgethunter.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Hues for the categories of a chart, in fixed order: the biggest category always takes the
 * first one.
 *
 * Each mode has its own steps, chosen against its surface rather than flipped from the other
 * one. There is one hue per category, so every slice keeps its own color. Past the first few,
 * some pairs get hard to tell apart for color blind readers; that is accepted because the list
 * under the chart names every category in the same order, and the order alternates hue and
 * lightness so that neighbouring slices still contrast. A category added later without a hue
 * here falls back to [chartRestColor].
 */
@Composable
fun chartCategoryColors(): List<Color> =
    if (isSystemInDarkTheme()) {
        listOf(
            Color(0xFF3987E5),
            Color(0xFFD95926),
            Color(0xFF199E70),
            Color(0xFF9B72E0),
            Color(0xFFD9A030),
            Color(0xFFE0608F),
            Color(0xFF35B3C7),
            Color(0xFFB57048),
            Color(0xFF8DAA2B),
            Color(0xFFE86464),
            Color(0xFF6375D9),
            Color(0xFFCF8ED1)
        )
    } else {
        listOf(
            Color(0xFF2A78D6),
            Color(0xFFEB6834),
            Color(0xFF1BAF7A),
            Color(0xFF8A5CD1),
            Color(0xFFE0A526),
            Color(0xFFD6457A),
            Color(0xFF2BA3B8),
            Color(0xFF9C5A33),
            Color(0xFF7A9A1E),
            Color(0xFFE05252),
            Color(0xFF3D4FA8),
            Color(0xFFC77DC9)
        )
    }

/**
 * Color of any category that has no hue in [chartCategoryColors]: it carries no identity of
 * its own, so it stays a muted gray that recedes behind the named categories.
 */
val chartRestColor: Color
    @Composable get() = AppColors.onSurfaceVariant.copy(alpha = 0.45f)
