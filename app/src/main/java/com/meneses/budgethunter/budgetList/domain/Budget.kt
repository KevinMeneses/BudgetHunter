package com.meneses.budgethunter.budgetList.domain

import android.os.Parcelable
import com.meneses.budgethunter.commons.EMPTY
import kotlinx.parcelize.Parcelize

@Parcelize
data class Budget(
    val id: Int = -1,
    val amount: Double = 0.0,
    val name: String = EMPTY,
    val frequency: Frequency? = null
) : Parcelable {
    enum class Frequency(val value: String) {
        UNIQUE("Unica"),
        DAILY("Diario"),
        WEEKLY("Semanal"),
        MONTHLY("Mensual"),
        ANNUAL("Anual");

        companion object {
            fun getFrequencies(): List<Frequency> =
                listOf(DAILY, WEEKLY, MONTHLY, ANNUAL, UNIQUE)

            fun getByValue(value: String) =
                when(value) {
                    UNIQUE.value -> UNIQUE
                    DAILY.value -> DAILY
                    WEEKLY.value -> WEEKLY
                    MONTHLY.value -> MONTHLY
                    ANNUAL.value -> ANNUAL
                    else -> UNIQUE
                }
        }
    }
}
