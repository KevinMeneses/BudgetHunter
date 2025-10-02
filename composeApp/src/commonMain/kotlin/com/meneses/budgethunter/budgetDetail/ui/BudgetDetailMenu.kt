package com.meneses.budgethunter.budgetDetail.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.delete_budget
import budgethunter.composeapp.generated.resources.filter
import budgethunter.composeapp.generated.resources.metrics
import budgethunter.composeapp.generated.resources.settings
import com.meneses.budgethunter.theme.AppColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun BudgetDetailMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onFilterClick: () -> Unit,
    onMetricsClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = AbsoluteRoundedCornerShape(10.dp),
        containerColor = AppColors.surface
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(Res.string.filter),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            onClick = {
                onDismiss()
                onFilterClick()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(Res.string.filter),
                    modifier = Modifier.size(18.dp),
                    tint = AppColors.primary
                )
            }
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(Res.string.metrics),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            onClick = {
                onDismiss()
                onMetricsClick()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = stringResource(Res.string.metrics),
                    modifier = Modifier.size(18.dp),
                    tint = AppColors.secondary
                )
            }
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(Res.string.delete_budget),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.error
                )
            },
            onClick = {
                onDismiss()
                onDeleteClick()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.delete_budget),
                    modifier = Modifier.size(18.dp),
                    tint = AppColors.error
                )
            }
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(Res.string.settings),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            onClick = {
                onDismiss()
                onSettingsClick()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(Res.string.settings),
                    modifier = Modifier.size(18.dp),
                    tint = AppColors.primary
                )
            }
        )
    }
}
