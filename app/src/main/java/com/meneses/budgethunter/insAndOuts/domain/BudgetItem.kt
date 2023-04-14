package com.meneses.budgethunter.insAndOuts.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BudgetItem(
    val amount: Double,
    val description: String?,
    val type: Type,
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
