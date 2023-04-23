package com.meneses.budgethunter.budgetEntry.domain

import android.os.Parcelable
import com.meneses.budgethunter.commons.EMPTY
import kotlinx.parcelize.Parcelize
import java.time.LocalDate

@Parcelize
data class BudgetEntry(
    val id: Int = -1,
    val budgetId: Int = -1,
    val amount: Double = 0.0,
    val description: String = EMPTY,
    val type: Type = Type.OUTCOME,
    val date: String = LocalDate.now().toString()
) : Parcelable {
    enum class Type(val value: String) {
        OUTCOME("Gasto"),
        INCOME("Ingreso")
    }

    companion object {
        fun getItemTypes() = listOf(Type.OUTCOME, Type.INCOME)
    }
}
