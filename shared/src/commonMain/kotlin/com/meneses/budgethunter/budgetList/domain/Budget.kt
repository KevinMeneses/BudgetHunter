package com.meneses.budgethunter.budgetList.domain

import com.meneses.budgethunter.commons.EMPTY
import kotlinx.serialization.Serializable

@Serializable
data class Budget(
    val id: Int = -1,
    val amount: Double = 0.0,
    val name: String = EMPTY,
    val frequency: Frequency = Frequency.UNIQUE
) {
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
