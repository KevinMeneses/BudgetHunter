package com.meneses.budgethunter.budgetList.domain

import com.meneses.budgethunter.commons.EMPTY
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Serializable
data class Budget(
    val id: Int = -1,
    val amount: Double = 0.0,
    val name: String = EMPTY,
    val totalExpenses: Double = 0.0,
    val date: String = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
)