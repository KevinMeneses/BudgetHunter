package com.meneses.budgethunter.budgetEntry.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.theme.AppColors

/** The icon that stands for a category in lists, chips and metrics. */
val BudgetEntry.Category.icon: ImageVector
    get() = when (this) {
        BudgetEntry.Category.UNCATEGORIZED -> Icons.AutoMirrored.Filled.HelpOutline
        BudgetEntry.Category.FOOD -> Icons.Default.Restaurant
        BudgetEntry.Category.GROCERIES -> Icons.Default.ShoppingCart
        BudgetEntry.Category.SELF_CARE -> Icons.Default.Spa
        BudgetEntry.Category.TRANSPORTATION -> Icons.Default.DirectionsBus
        BudgetEntry.Category.HOUSEHOLD_ITEMS -> Icons.Default.Chair
        BudgetEntry.Category.SERVICES -> Icons.Default.Bolt
        BudgetEntry.Category.EDUCATION -> Icons.Default.School
        BudgetEntry.Category.HEALTH -> Icons.Default.LocalHospital
        BudgetEntry.Category.LEISURE -> Icons.Default.Celebration
        BudgetEntry.Category.TAXES -> Icons.Default.AccountBalance
        BudgetEntry.Category.OTHER -> Icons.Default.MoreHoriz
    }

/**
 * The category's icon in a round badge. Neutral on purpose: the metrics screen colors categories
 * by their rank, so a fixed color here would contradict it. "Sin categoría" takes the AI's blue,
 * since it is what the automatic categorization looks for.
 */
@Composable
fun CategoryBadge(
    category: BudgetEntry.Category,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val isWaiting = category == BudgetEntry.Category.UNCATEGORIZED
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isWaiting) AppColors.tertiaryContainer else AppColors.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = if (isWaiting) AppColors.onTertiaryContainer else AppColors.onSurfaceVariant,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}
