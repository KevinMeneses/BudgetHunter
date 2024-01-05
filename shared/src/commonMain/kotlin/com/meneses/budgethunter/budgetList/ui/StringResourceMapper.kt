package com.meneses.budgethunter.budgetList.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetList.domain.Budget

@Composable
fun Budget.Frequency.toStringResource() = when (this) {
    Budget.Frequency.UNIQUE -> stringResource(id = R.string.unique)
    Budget.Frequency.DAILY -> stringResource(id = R.string.daily)
    Budget.Frequency.WEEKLY -> stringResource(id = R.string.weekly)
    Budget.Frequency.MONTHLY -> stringResource(id = R.string.monthly)
    Budget.Frequency.ANNUAL -> stringResource(id = R.string.annual)
}
