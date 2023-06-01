package com.meneses.budgethunter.budgetEntry.domain

import android.os.Parcelable
import com.meneses.budgethunter.commons.EMPTY
import java.time.LocalDate
import kotlinx.parcelize.Parcelize

@Parcelize
data class BudgetEntry(
    val id: Int = -1,
    val budgetId: Int = -1,
    val amount: String = EMPTY,
    val description: String = EMPTY,
    val type: Type = Type.OUTCOME,
    val date: String = LocalDate.now().toString(),
    val isSelected: Boolean = false
) : Parcelable {
    enum class Type {
        OUTCOME,
        INCOME
    }

    companion object {
        fun getItemTypes() = listOf(Type.OUTCOME, Type.INCOME)
    }
}
