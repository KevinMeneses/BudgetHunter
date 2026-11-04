package com.meneses.budgethunter.budgetDetail.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.delete_budget
import budgethunter.composeapp.generated.resources.settings
import com.meneses.budgethunter.theme.AppColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun BudgetDetailMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onDeleteClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.medium,
        containerColor = AppColors.surfaceContainer
    ) {
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
                    tint = AppColors.onSurfaceVariant
                )
            }
        )
        // Last and apart from the rest: the only action here that cannot be undone
        HorizontalDivider()
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
    }
}
