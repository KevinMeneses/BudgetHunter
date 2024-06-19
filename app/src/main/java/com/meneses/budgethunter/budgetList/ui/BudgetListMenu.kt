package com.meneses.budgethunter.budgetList.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetDetail.ui.MenuButton
import com.meneses.budgethunter.commons.ui.blinkEffect
import com.meneses.budgethunter.commons.ui.pulsateEffect
import com.meneses.budgethunter.theme.AppColors

@Composable
fun BudgetListMenu(
    isCollaborationActive: Boolean,
    onCollaborateClick: () -> Unit,
    onUserGuideClick: () -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            LazyColumn(
                modifier = Modifier.background(AppColors.primary)
            ) {
                item { CollaborateButton(isCollaborationActive, onCollaborateClick) }
                item { UserGuideButton(onUserGuideClick) }
            }
        }
    }
}

@Composable
private fun CollaborateButton(
    animate: Boolean,
    onClick: () -> Unit
) {
    MenuButton(
        modifier = Modifier
            .blinkEffect(animate)
            .pulsateEffect(animate, targetValue = 1.1f),
        text = stringResource(id = R.string.collaborate),
        icon = Icons.Default.Person,
        onClick = onClick
    )
}

@Composable
fun UserGuideButton(
    onClick: () -> Unit,
) {
    MenuButton(
        text = stringResource(id = R.string.user_guide),
        icon = Icons.Outlined.Info,
        onClick = onClick,
        withDivider = false
    )
}
