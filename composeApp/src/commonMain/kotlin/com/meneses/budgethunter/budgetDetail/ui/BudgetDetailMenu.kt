package com.meneses.budgethunter.budgetDetail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.delete_budget
import budgethunter.composeapp.generated.resources.filter
import budgethunter.composeapp.generated.resources.metrics
import com.meneses.budgethunter.budgetList.ui.SettingsButton
import com.meneses.budgethunter.commons.ui.DefDivider
import com.meneses.budgethunter.commons.ui.blinkEffect
import com.meneses.budgethunter.commons.ui.pulsateEffect
import com.meneses.budgethunter.theme.AppColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun BudgetDetailMenu(
    animateFilterButton: Boolean,
    onFilterClick: () -> Unit,
    onMetricsClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            LazyColumn(
                modifier = Modifier.background(AppColors.primary)
            ) {
                item { FilterButton(animateFilterButton, onFilterClick) }
                item { MetricsButton(onMetricsClick) }
                item { DeleteButton(onDeleteClick) }
                item { SettingsButton(onSettingsClick) }
            }
        }
    }
}

@Composable
private fun FilterButton(
    animate: Boolean,
    onClick: () -> Unit
) {
    MenuButton(
        modifier = Modifier
            .blinkEffect(animate)
            .pulsateEffect(animate, targetValue = 1.1f),
        text = stringResource(Res.string.filter),
        icon = Icons.Default.Search,
        onClick = onClick
    )
}

@Composable
private fun MetricsButton(onClick: () -> Unit) {
    MenuButton(
        text = stringResource(Res.string.metrics),
        icon = Icons.Default.Star,
        onClick = onClick
    )
}

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    MenuButton(
        text = stringResource(Res.string.delete_budget),
        icon = Icons.Default.Delete,
        onClick = onClick,
        withDivider = true
    )
}

@Composable
fun MenuButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    withDivider: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.padding(end = 10.dp),
            tint = AppColors.onPrimary
        )
        Text(
            text = text,
            color = AppColors.onPrimary
        )
    }

    if (withDivider) DefDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = AppColors.onPrimary
    )
}