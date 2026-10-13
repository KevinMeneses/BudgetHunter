package com.meneses.budgethunter.budgetEntry.domain

import com.meneses.budgethunter.commons.EMPTY
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class BudgetEntry(
    val id: Int = -1,
    val budgetId: Int = -1,
    val amount: String = EMPTY,
    val description: String = EMPTY,
    val type: Type = Type.OUTCOME,
    val category: Category = Category.UNCATEGORIZED,
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
        /**
         * Not a category but the absence of one ("Sin categoría"): what an entry holds until someone
         * chooses. It is what the automatic categorization looks for, and it is different from [OTHER],
         * which is a choice (the user's, or the AI's after looking at the entry and finding nothing
         * that fits). The server stores it as is.
         */
        UNCATEGORIZED,
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

        /** Every category a person can pick in the selector or see in the metrics, "Sin categoría" first. */
        fun getCategories() = listOf(Category.UNCATEGORIZED) + getAssignableCategories()

        /**
         * The categories that mean something: what the AI may answer, and what the receipt prompt and its
         * response schema list. Never includes [Category.UNCATEGORIZED].
         */
        fun getAssignableCategories() = listOf(
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
