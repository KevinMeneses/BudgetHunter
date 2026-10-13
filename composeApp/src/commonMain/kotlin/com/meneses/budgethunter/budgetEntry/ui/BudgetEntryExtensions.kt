package com.meneses.budgethunter.budgetEntry.ui

import androidx.compose.runtime.Composable
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.education
import budgethunter.composeapp.generated.resources.food
import budgethunter.composeapp.generated.resources.groceries
import budgethunter.composeapp.generated.resources.health
import budgethunter.composeapp.generated.resources.household_items
import budgethunter.composeapp.generated.resources.income
import budgethunter.composeapp.generated.resources.leisure
import budgethunter.composeapp.generated.resources.other
import budgethunter.composeapp.generated.resources.outcome
import budgethunter.composeapp.generated.resources.self_care
import budgethunter.composeapp.generated.resources.services
import budgethunter.composeapp.generated.resources.taxes
import budgethunter.composeapp.generated.resources.transportation
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import org.jetbrains.compose.resources.stringResource

@Composable
fun BudgetEntry.Type.toStringResource(): String {
    return when (this) {
        BudgetEntry.Type.OUTCOME -> stringResource(Res.string.outcome)
        BudgetEntry.Type.INCOME -> stringResource(Res.string.income)
    }
}

@Composable
fun BudgetEntry.Category.toStringResource(): String {
    return when (this) {
        BudgetEntry.Category.FOOD -> stringResource(Res.string.food)
        BudgetEntry.Category.GROCERIES -> stringResource(Res.string.groceries)
        BudgetEntry.Category.SELF_CARE -> stringResource(Res.string.self_care)
        BudgetEntry.Category.TRANSPORTATION -> stringResource(Res.string.transportation)
        BudgetEntry.Category.HOUSEHOLD_ITEMS -> stringResource(Res.string.household_items)
        BudgetEntry.Category.SERVICES -> stringResource(Res.string.services)
        BudgetEntry.Category.EDUCATION -> stringResource(Res.string.education)
        BudgetEntry.Category.HEALTH -> stringResource(Res.string.health)
        BudgetEntry.Category.LEISURE -> stringResource(Res.string.leisure)
        BudgetEntry.Category.TAXES -> stringResource(Res.string.taxes)
        BudgetEntry.Category.OTHER -> stringResource(Res.string.other)
    }
}
