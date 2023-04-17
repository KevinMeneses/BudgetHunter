package com.meneses.budgethunter.budgetEntry.domain

import android.os.Parcelable
import com.meneses.budgethunter.commons.EMPTY
import kotlinx.parcelize.Parcelize

@Parcelize
data class BudgetEntry(
    val id: Int = -1,
    val budgetId: Int,
    val amount: Double? = null,
    val description: String? = EMPTY,
    val type: Type = Type.OUTCOME,
    val date: String? = null
) : Parcelable {
    enum class Type(val value: String) {
        OUTCOME("Gasto"),
        INCOME("Ingreso")
    }

    companion object {
        fun getItemTypes() = listOf(Type.OUTCOME, Type.INCOME)
    }
}
