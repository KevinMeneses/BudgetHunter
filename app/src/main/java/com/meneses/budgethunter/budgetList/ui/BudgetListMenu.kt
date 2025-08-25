package com.meneses.budgethunter.budgetList.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetDetail.ui.MenuButton
import com.meneses.budgethunter.theme.AppColors

@Composable
fun BudgetListMenu(
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
                item { SettingsButton(onSettingsClick) }
            }
        }
    }
}

@Composable
fun SettingsButton(
    onClick: () -> Unit
) {
    MenuButton(
        text = stringResource(id = R.string.settings),
        icon = Icons.Filled.Settings,
        onClick = onClick,
        withDivider = false
    )
}
