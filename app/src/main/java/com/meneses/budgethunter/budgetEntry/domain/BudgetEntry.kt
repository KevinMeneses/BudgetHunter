package com.meneses.budgethunter.budgetEntry.domain

import android.os.Parcelable
import com.meneses.budgethunter.commons.EMPTY
import kotlinx.parcelize.Parcelize
import java.time.LocalDate

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
    enum class Type(val value: String) {
        OUTCOME("Gasto"),
        INCOME("Ingreso"),
        BOTH("Ambos")
    }

    companion object {
        fun getItemTypes(type: Type? = null) =
            if (type == Type.BOTH) listOf(Type.BOTH, Type.OUTCOME, Type.INCOME)
            else listOf(Type.OUTCOME, Type.INCOME)
    }
}
