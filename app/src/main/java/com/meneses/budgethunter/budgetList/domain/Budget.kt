package com.meneses.budgethunter.budgetList.domain

import com.meneses.budgethunter.commons.EMPTY
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Budget(
    val id: Int = -1,
    val amount: Double = 0.0,
    val name: String = EMPTY,
    val totalExpenses: Double = 0.0,
    val date: String = LocalDate.now().toString()
)
