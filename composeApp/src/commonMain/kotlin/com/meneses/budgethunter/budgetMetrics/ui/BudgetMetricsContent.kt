package com.meneses.budgethunter.budgetMetrics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.metrics_empty_message
import budgethunter.composeapp.generated.resources.metrics_empty_title
import budgethunter.composeapp.generated.resources.metrics_expenses
import budgethunter.composeapp.generated.resources.metrics_incomes
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.ui.toStringResource
import com.meneses.budgethunter.budgetMetrics.application.BudgetMetricsState
import com.meneses.budgethunter.budgetMetrics.domain.CategoryMetric
import com.meneses.budgethunter.commons.ui.DonutChart
import com.meneses.budgethunter.commons.ui.EmptyStatePlaceholder
import com.meneses.budgethunter.commons.util.toCurrency
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.chartCategoryColors
import com.meneses.budgethunter.theme.chartRestColor
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun BudgetMetricsContent(
    uiState: BudgetMetricsState,
    modifier: Modifier = Modifier
) {
    if (uiState.categoryMetrics.isEmpty()) {
        EmptyStatePlaceholder(
            modifier = modifier,
            title = stringResource(Res.string.metrics_empty_title),
            subtitle = stringResource(Res.string.metrics_empty_message)
        )
        return
    }

    // Only the categories that get their own hue in the donut keep it in the list below, so
    // both halves of the screen can be read as one.
    val categoryColors = chartCategoryColors()
    val restColor = chartRestColor
    val colorOf = { index: Int ->
        if (index < categoryColors.size) categoryColors[index] else restColor
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            MetricsDonut(
                categoryMetrics = uiState.categoryMetrics,
                total = uiState.total,
                centerLabel = uiState.selectedType.toMetricsLabel(),
                categoryColors = categoryColors,
                restColor = restColor
            )
        }

        itemsIndexed(uiState.categoryMetrics) { index, metric ->
            CategoryRow(
                metric = metric,
                color = colorOf(index)
            )
        }
    }
}

/**
 * The biggest categories carry their own hue; everything below them is folded into one muted
 * slice, left unlabeled because the list underneath already names every category inside it.
 */
@Composable
private fun MetricsDonut(
    categoryMetrics: List<CategoryMetric>,
    total: Double,
    centerLabel: String,
    categoryColors: List<Color>,
    restColor: Color
) {
    val named = categoryMetrics.take(categoryColors.size)
    val rest = categoryMetrics.drop(categoryColors.size)

    val proportions = buildList {
        named.forEach { add((it.percentage / 100).toFloat()) }
        if (rest.isNotEmpty()) add((rest.sumOf { it.percentage } / 100).toFloat())
    }

    val colors = buildList {
        addAll(categoryColors.take(named.size))
        if (rest.isNotEmpty()) add(restColor)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        DonutChart(
            proportions = proportions,
            colors = colors,
            centerLabel = centerLabel,
            centerValue = total.toCurrency(),
            modifier = Modifier.padding(vertical = 30.dp)
        )
    }
}

@Composable
private fun CategoryRow(
    metric: CategoryMetric,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = metric.category.toStringResource(),
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.onSurface
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = metric.amount.toCurrency(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.onSurface
                )
                Text(
                    text = metric.percentage.asPercentage(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AppColors.onSurface.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((metric.percentage / 100).toFloat().coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun BudgetEntry.Type.toMetricsLabel() = when (this) {
    BudgetEntry.Type.OUTCOME -> stringResource(Res.string.metrics_expenses)
    BudgetEntry.Type.INCOME -> stringResource(Res.string.metrics_incomes)
}

private fun Double.asPercentage(): String {
    val rounded = (this * 10).roundToInt() / 10.0
    return "$rounded%"
}
