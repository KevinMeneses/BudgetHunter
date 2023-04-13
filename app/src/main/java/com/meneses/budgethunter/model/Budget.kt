package com.meneses.budgethunter.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Budget(
    val id: Int,
    val name: String,
    val frequency: Frequency? = null
) : Parcelable {
    enum class Frequency(val value: String) {
        DAILY("Diario"),
        WEEKLY("Semanal"),
        MONTHLY("Mensual"),
        ANNUAL("Anual"),
        UNIQUE("Unica");

        companion object {
            fun getFrequencies(): List<Frequency> =
                listOf(DAILY, WEEKLY, MONTHLY, ANNUAL, UNIQUE)
        }
    }
}
