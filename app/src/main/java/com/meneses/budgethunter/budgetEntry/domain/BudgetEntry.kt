package com.meneses.budgethunter.budgetEntry.domain

import com.meneses.budgethunter.commons.EMPTY
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class BudgetEntry(
    val id: Int = -1,
    val budgetId: Int = -1,
    val amount: String = EMPTY,
    val description: String = EMPTY,
    val type: Type = Type.OUTCOME,
    val category: Category = Category.OTHER,
    val date: String = LocalDate.now().toString(),
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
