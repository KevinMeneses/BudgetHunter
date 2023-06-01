package com.meneses.budgethunter.budgetEntry.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

@Composable
fun BudgetEntry.Type.toStringResource(): String = when (this) {
    BudgetEntry.Type.INCOME -> stringResource(id = R.string.income)
    BudgetEntry.Type.OUTCOME -> stringResource(id = R.string.outcome)
}
