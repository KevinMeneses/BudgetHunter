package com.meneses.budgethunter.budgetList.domain

data class BudgetFilter(
    val name: String? = null,
    val frequency: Budget.Frequency? = null
)
