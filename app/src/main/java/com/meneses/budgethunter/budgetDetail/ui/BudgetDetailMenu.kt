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
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.R
import com.meneses.budgethunter.commons.ui.DefDivider
import com.meneses.budgethunter.commons.ui.blinkEffect
import com.meneses.budgethunter.commons.ui.pulsateEffect
import com.meneses.budgethunter.theme.AppColors

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    BudgetDetailMenu(
        animateFilterButton = false,
        onFilterClick = {},
        onDeleteClick = {}
    )
}

@Composable
fun BudgetDetailMenu(
    animateFilterButton: Boolean,
    onFilterClick: () -> Unit,
    onDeleteClick: () -> Unit
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
                item { DeleteButton(onDeleteClick) }
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
        text = stringResource(id = R.string.filter),
        icon = Icons.Default.Search,
        onClick = onClick
    )
}

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    MenuButton(
        text = stringResource(id = R.string.delete_budget),
        icon = Icons.Default.Delete,
        onClick = onClick,
        withDivider = false
    )
}

@Composable
private fun MenuButton(
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
