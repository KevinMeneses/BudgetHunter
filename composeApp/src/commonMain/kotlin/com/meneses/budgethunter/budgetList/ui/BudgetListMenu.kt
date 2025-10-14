package com.meneses.budgethunter.budgetList.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.change_name
import budgethunter.composeapp.generated.resources.delete
import budgethunter.composeapp.generated.resources.delete_budget
import budgethunter.composeapp.generated.resources.duplicate
import budgethunter.composeapp.generated.resources.duplicate_budget
import budgethunter.composeapp.generated.resources.edit_budget
import budgethunter.composeapp.generated.resources.settings
import budgethunter.composeapp.generated.resources.sign_in
import budgethunter.composeapp.generated.resources.sign_out
import com.meneses.budgethunter.theme.AppColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun BudgetListMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    isAuthenticated: Boolean = false,
    onSignOutClick: () -> Unit = {},
    onSignInClick: () -> Unit = {}
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = AbsoluteRoundedCornerShape(12.dp),
        containerColor = AppColors.surface
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    modifier = Modifier.padding(end = 10.dp),
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

        // Sign In/Out option
        if (isAuthenticated) {
            DropdownMenuItem(
                text = {
                    Text(
                        modifier = Modifier.padding(end = 10.dp),
                        text = stringResource(Res.string.sign_out),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                onClick = {
                    onDismiss()
                    onSignOutClick()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = stringResource(Res.string.sign_out),
                        modifier = Modifier.size(18.dp),
                        tint = AppColors.error
                    )
                }
            )
        } else {
            DropdownMenuItem(
                text = {
                    Text(
                        modifier = Modifier.padding(end = 10.dp),
                        text = stringResource(Res.string.sign_in),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                onClick = {
                    onDismiss()
                    onSignInClick()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = stringResource(Res.string.sign_in),
                        modifier = Modifier.size(18.dp),
                        tint = AppColors.secondary
                    )
                }
            )
        }
    }
}

@Composable
fun BudgetListItemMenu(
    dropdownExpanded: Boolean,
    onDismiss: () -> Unit,
    onUpdateClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    DropdownMenu(
        expanded = dropdownExpanded,
        onDismissRequest = onDismiss,
        shape = AbsoluteRoundedCornerShape(10.dp),
        containerColor = AppColors.surface
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(Res.string.change_name),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(Res.string.edit_budget),
                    modifier = Modifier.size(18.dp),
                    tint = AppColors.primary
                )
            },
            onClick = {
                onDismiss()
                onUpdateClick()
            }
        )

        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(Res.string.duplicate),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.duplicate_budget),
                    modifier = Modifier.size(18.dp),
                    tint = AppColors.secondary
                )
            },
            onClick = {
                onDismiss()
                onDuplicateClick()
            }
        )

        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(Res.string.delete),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.error
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.delete_budget),
                    modifier = Modifier.size(18.dp),
                    tint = AppColors.error
                )
            },
            onClick = {
                onDismiss()
                onDeleteClick()
            }
        )
    }
}
