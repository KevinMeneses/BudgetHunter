package com.meneses.budgethunter.budgetList.domain

import android.os.Parcelable
import com.meneses.budgethunter.commons.EMPTY
import kotlinx.parcelize.Parcelize

@Parcelize
data class Budget(
    val id: Int = -1,
    val amount: Double = 0.0,
    val name: String = EMPTY,
    val frequency: Frequency = Frequency.UNIQUE
) : Parcelable {
    enum class Frequency {
        UNIQUE,
        DAILY,
        WEEKLY,
        MONTHLY,
        ANNUAL;

        companion object {
            fun getFrequencies() = listOf(DAILY, WEEKLY, MONTHLY, ANNUAL, UNIQUE)
        }
    }
}
