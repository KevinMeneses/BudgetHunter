package com.meneses.budgethunter.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Hues for the categories of a chart, in fixed order: the biggest category always takes the
 * first one, so a category never changes color because another one appeared.
 *
 * Each mode has its own steps, chosen against its surface rather than flipped from the other
 * one. Three hues is where the set stops: in a chart where every slice is compared against
 * every other one, a fourth hue stops being distinguishable for color blind readers on the
 * dark surface. Everything past the third category goes to [chartRestColor].
 */
@Composable
fun chartCategoryColors(): List<Color> =
    if (isSystemInDarkTheme()) {
        listOf(
            Color(0xFF3987E5),
            Color(0xFFD95926),
            Color(0xFF199E70)
        )
    } else {
        listOf(
            Color(0xFF2A78D6),
            Color(0xFFEB6834),
            Color(0xFF1BAF7A)
        )
    }

/**
 * Color of everything that did not make it into [chartCategoryColors]: it carries no identity
 * of its own, so it stays a muted gray that recedes behind the named categories.
 */
val chartRestColor: Color
    @Composable get() = AppColors.onSurfaceVariant.copy(alpha = 0.45f)
