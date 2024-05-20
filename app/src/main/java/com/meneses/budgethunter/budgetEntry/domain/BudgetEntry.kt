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
    val date: String = LocalDate.now().toString(),
    val invoice: String? = null,
    val isSelected: Boolean = false
) {
    @Serializable
    enum class Type {
        OUTCOME,
        INCOME
    }

    companion object {
        fun getItemTypes() = listOf(Type.OUTCOME, Type.INCOME)
    }
}
