package com.meneses.budgethunter.budgetList.data

import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetMapperTest {

    @Test
    fun `mapSelectAllToBudget maps all fields correctly`() {
        val budget = mapSelectAllToBudget(
            id = 1L,
            amount = 1000.0,
            name = "Test Budget",
            date = "2024-01-15",
            totalExpenses = 250.50
        )

        assertEquals(1, budget.id)
        assertEquals(1000.0, budget.amount)
        assertEquals("Test Budget", budget.name)
        assertEquals("2024-01-15", budget.date)
        assertEquals(250.50, budget.totalExpenses)
    }

    @Test
    fun `mapSelectAllToBudget converts Long to Int for id`() {
        val budget = mapSelectAllToBudget(
            id = 999L,
            amount = 0.0,
            name = "",
            date = "",
            totalExpenses = 0.0
        )

        assertEquals(999, budget.id)
    }

    @Test
    fun `mapSelectAllToBudget handles zero values`() {
        val budget = mapSelectAllToBudget(
            id = 0L,
            amount = 0.0,
            name = "Zero Budget",
            date = "2024-01-01",
            totalExpenses = 0.0
        )

        assertEquals(0, budget.id)
        assertEquals(0.0, budget.amount)
        assertEquals(0.0, budget.totalExpenses)
    }

    @Test
    fun `mapSelectAllToBudget handles large values`() {
        val budget = mapSelectAllToBudget(
            id = Long.MAX_VALUE,
            amount = 999999.99,
            name = "Large Budget",
            date = "2024-12-31",
            totalExpenses = 500000.50
        )

        assertEquals(Long.MAX_VALUE.toInt(), budget.id)
        assertEquals(999999.99, budget.amount)
        assertEquals(500000.50, budget.totalExpenses)
    }

    @Test
    fun `mapSelectAllToBudget handles negative expenses`() {
        // This might represent income exceeding expenses
        val budget = mapSelectAllToBudget(
            id = 1L,
            amount = 1000.0,
            name = "Budget with Income",
            date = "2024-01-01",
            totalExpenses = -100.0
        )

        assertEquals(-100.0, budget.totalExpenses)
    }

    @Test
    fun `mapSelectAllToBudget handles empty strings`() {
        val budget = mapSelectAllToBudget(
            id = 1L,
            amount = 100.0,
            name = "",
            date = "",
            totalExpenses = 0.0
        )

        assertEquals("", budget.name)
        assertEquals("", budget.date)
    }

    @Test
    fun `mapSelectAllToBudget handles special characters in name`() {
        val budget = mapSelectAllToBudget(
            id = 1L,
            amount = 100.0,
            name = "Budget with $pecial Ch@racters & Symbols!",
            date = "2024-01-01",
            totalExpenses = 0.0
        )

        assertEquals("Budget with $pecial Ch@racters & Symbols!", budget.name)
    }

    @Test
    fun `mapSelectAllToBudget handles decimal precision`() {
        val budget = mapSelectAllToBudget(
            id = 1L,
            amount = 123.456789,
            name = "Precise Budget",
            date = "2024-01-01",
            totalExpenses = 67.891234
        )

        assertEquals(123.456789, budget.amount)
        assertEquals(67.891234, budget.totalExpenses)
    }
}
