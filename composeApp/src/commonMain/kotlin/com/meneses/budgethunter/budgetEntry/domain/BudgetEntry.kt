package com.meneses.budgethunter.budgetEntry.domain

import com.meneses.budgethunter.commons.EMPTY
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
    val isSelected: Boolean = false
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
// TODO: get the real string resources not just hardcoded strings
fun BudgetEntry.Type.toStringResource(): String {
    return when (this) {
        BudgetEntry.Type.OUTCOME -> "Outcome"
        BudgetEntry.Type.INCOME -> "Income"
    }
}

// TODO: get the real string resources not just hardcoded strings
fun BudgetEntry.Category.toStringResource(): String {
    return when (this) {
        BudgetEntry.Category.FOOD -> "Food"
        BudgetEntry.Category.GROCERIES -> "Groceries"
        BudgetEntry.Category.SELF_CARE -> "Self Care"
        BudgetEntry.Category.TRANSPORTATION -> "Transportation"
        BudgetEntry.Category.HOUSEHOLD_ITEMS -> "Household Items"
        BudgetEntry.Category.SERVICES -> "Services"
        BudgetEntry.Category.EDUCATION -> "Education"
        BudgetEntry.Category.HEALTH -> "Health"
        BudgetEntry.Category.LEISURE -> "Leisure"
        BudgetEntry.Category.TAXES -> "Taxes"
        BudgetEntry.Category.OTHER -> "Other"
    }
}
