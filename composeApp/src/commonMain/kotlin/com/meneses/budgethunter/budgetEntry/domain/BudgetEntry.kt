package com.meneses.budgethunter.budgetEntry.domain

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
import com.meneses.budgethunter.commons.EMPTY
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

@Serializable
data class BudgetEntry(
    val id: Int = -1,
    val budgetId: Int = -1,
    val amount: String = EMPTY,
    val description: String = EMPTY,
    val type: Type = Type.OUTCOME,
    val category: Category = Category.OTHER,
    val date: String = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString(),
    val invoice: String? = null,
    val isSelected: Boolean = false,
    val serverId: Long? = null,
    val isSynced: Boolean = false,
    val createdByEmail: String? = null,
    val updatedByEmail: String? = null,
    val creationDate: String? = null,
    val modificationDate: String? = null
) {
    @Serializable
    enum class Type {
        OUTCOME,
        INCOME
    }

    @Serializable
    enum class Category {
        FOOD,
        GROCERIES,
        SELF_CARE,
        TRANSPORTATION,
        HOUSEHOLD_ITEMS,
        SERVICES,
        EDUCATION,
        HEALTH,
        LEISURE,
        TAXES,
        OTHER
    }

    companion object {
        fun getItemTypes() = listOf(Type.OUTCOME, Type.INCOME)
        fun getCategories() = listOf(
            Category.FOOD,
            Category.GROCERIES,
            Category.SELF_CARE,
            Category.TRANSPORTATION,
            Category.HOUSEHOLD_ITEMS,
            Category.SERVICES,
            Category.EDUCATION,
            Category.HEALTH,
            Category.LEISURE,
            Category.TAXES,
            Category.OTHER
        )
    }
}

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
