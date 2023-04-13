package com.meneses.budgethunter.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BudgetDetail(
    val amount: Double,
    val description: String?,
    val type: Type,
    val date: String? = null
) : Parcelable {
    enum class Type(val value: String) {
        OUTCOME("Gasto"),
        INCOME("Ingreso")
    }
}
