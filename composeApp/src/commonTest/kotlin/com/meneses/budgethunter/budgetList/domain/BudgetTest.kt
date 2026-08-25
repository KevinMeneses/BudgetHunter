package com.meneses.budgethunter.budgetList.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetTest {

    @Test
    fun `totalAvailable adds the incomes to the budget amount`() {
        val budget = Budget(
            amount = 1000.0,
            totalIncomes = 250.0,
            totalExpenses = 400.0
        )

        assertEquals(1250.0, budget.totalAvailable)
    }

    @Test
    fun `totalAvailable is the budget amount when there are no incomes`() {
        val budget = Budget(amount = 1000.0, totalExpenses = 400.0)

        assertEquals(1000.0, budget.totalAvailable)
    }
}
