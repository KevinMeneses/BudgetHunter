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

@Composable
fun BudgetEntry.Category.toStringResource(): String = when (this) {
    BudgetEntry.Category.FOOD -> stringResource(id = R.string.food)
    BudgetEntry.Category.GROCERIES -> stringResource(id = R.string.groceries)
    BudgetEntry.Category.SELF_CARE -> stringResource(id = R.string.self_care)
    BudgetEntry.Category.TRANSPORTATION -> stringResource(id = R.string.transportation)
    BudgetEntry.Category.HOUSEHOLD_ITEMS -> stringResource(id = R.string.household_items)
    BudgetEntry.Category.SERVICES -> stringResource(id = R.string.services)
    BudgetEntry.Category.EDUCATION -> stringResource(id = R.string.education)
    BudgetEntry.Category.HEALTH -> stringResource(id = R.string.health)
    BudgetEntry.Category.LEISURE -> stringResource(id = R.string.leisure)
    BudgetEntry.Category.TAXES -> stringResource(id = R.string.taxes)
    BudgetEntry.Category.OTHER -> stringResource(id = R.string.other)
}
