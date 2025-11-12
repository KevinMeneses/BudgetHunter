package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.db.Budget_entry
import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetEntryMapperTest {

    @Test
    fun `toDomain maps all fields correctly`() {
        val dbEntry = Budget_entry(
            id = 1L,
            budget_id = 10L,
            amount = 100.50,
            description = "Test Entry",
            type = BudgetEntry.Type.OUTCOME,
            date = "2024-01-15",
            invoice = "invoice.pdf",
            category = BudgetEntry.Category.FOOD
        )

        val domainEntry = dbEntry.toDomain()

        assertEquals(1, domainEntry.id)
        assertEquals(10, domainEntry.budgetId)
        assertEquals("100.50", domainEntry.amount)
        assertEquals("Test Entry", domainEntry.description)
        assertEquals(BudgetEntry.Type.OUTCOME, domainEntry.type)
        assertEquals("2024-01-15", domainEntry.date)
        assertEquals("invoice.pdf", domainEntry.invoice)
        assertEquals(BudgetEntry.Category.FOOD, domainEntry.category)
    }

    @Test
    fun `toDomain converts Long to Int for ids`() {
        val dbEntry = Budget_entry(
            id = 999L,
            budget_id = 888L,
            amount = 0.0,
            description = "",
            type = BudgetEntry.Type.INCOME,
            date = "",
            invoice = null,
            category = BudgetEntry.Category.OTHER
        )

        val domainEntry = dbEntry.toDomain()

        assertEquals(999, domainEntry.id)
        assertEquals(888, domainEntry.budgetId)
    }

    @Test
    fun `toDomain formats amount using toPlainString`() {
        val dbEntry = Budget_entry(
            id = 1L,
            budget_id = 1L,
            amount = 100.0,
            description = "",
            type = BudgetEntry.Type.OUTCOME,
            date = "",
            invoice = null,
            category = BudgetEntry.Category.OTHER
        )

        val domainEntry = dbEntry.toDomain()

        // Whole numbers should not have decimals
        assertEquals("100", domainEntry.amount)
    }

    @Test
    fun `toDomain formats decimal amounts correctly`() {
        val dbEntry = Budget_entry(
            id = 1L,
            budget_id = 1L,
            amount = 123.45,
            description = "",
            type = BudgetEntry.Type.OUTCOME,
            date = "",
            invoice = null,
            category = BudgetEntry.Category.OTHER
        )

        val domainEntry = dbEntry.toDomain()

        assertEquals("123.45", domainEntry.amount)
    }

    @Test
    fun `toDomain handles null invoice`() {
        val dbEntry = Budget_entry(
            id = 1L,
            budget_id = 1L,
            amount = 50.0,
            description = "No invoice",
            type = BudgetEntry.Type.OUTCOME,
            date = "2024-01-01",
            invoice = null,
            category = BudgetEntry.Category.GROCERIES
        )

        val domainEntry = dbEntry.toDomain()

        assertEquals(null, domainEntry.invoice)
    }

    @Test
    fun `toDomain handles empty strings`() {
        val dbEntry = Budget_entry(
            id = 1L,
            budget_id = 1L,
            amount = 0.0,
            description = "",
            type = BudgetEntry.Type.OUTCOME,
            date = "",
            invoice = null,
            category = BudgetEntry.Category.OTHER
        )

        val domainEntry = dbEntry.toDomain()

        assertEquals("", domainEntry.description)
        assertEquals("", domainEntry.date)
        assertEquals(BudgetEntry.Category.OTHER, domainEntry.category)
    }

    @Test
    fun `toDomain list maps empty list`() {
        val emptyList = emptyList<Budget_entry>()
        val result = emptyList.toDomain()

        assertEquals(0, result.size)
    }

    @Test
    fun `toDomain list maps multiple entries`() {
        val dbEntries = listOf(
            Budget_entry(
                id = 1L,
                budget_id = 1L,
                amount = 100.0,
                description = "Entry 1",
                type = BudgetEntry.Type.OUTCOME,
                date = "2024-01-01",
                invoice = null,
                category = BudgetEntry.Category.FOOD
            ),
            Budget_entry(
                id = 2L,
                budget_id = 1L,
                amount = 50.50,
                description = "Entry 2",
                type = BudgetEntry.Type.INCOME,
                date = "2024-01-02",
                invoice = "invoice2.pdf",
                category = BudgetEntry.Category.OTHER
            ),
            Budget_entry(
                id = 3L,
                budget_id = 1L,
                amount = 75.25,
                description = "Entry 3",
                type = BudgetEntry.Type.OUTCOME,
                date = "2024-01-03",
                invoice = null,
                category = BudgetEntry.Category.TRANSPORTATION
            )
        )

        val domainEntries = dbEntries.toDomain()

        assertEquals(3, domainEntries.size)
        assertEquals(1, domainEntries[0].id)
        assertEquals("100", domainEntries[0].amount)
        assertEquals("Entry 1", domainEntries[0].description)

        assertEquals(2, domainEntries[1].id)
        assertEquals("50.50", domainEntries[1].amount)
        assertEquals("invoice2.pdf", domainEntries[1].invoice)

        assertEquals(3, domainEntries[2].id)
        assertEquals("75.25", domainEntries[2].amount)
        assertEquals(BudgetEntry.Category.TRANSPORTATION, domainEntries[2].category)
    }

    @Test
    fun `toDomain handles different entry types`() {
        val outcomeEntry = Budget_entry(
            id = 1L,
            budget_id = 1L,
            amount = 100.0,
            description = "Expense",
            type = BudgetEntry.Type.OUTCOME,
            date = "2024-01-01",
            invoice = null,
            category = BudgetEntry.Category.FOOD
        )

        val incomeEntry = Budget_entry(
            id = 2L,
            budget_id = 1L,
            amount = 200.0,
            description = "Income",
            type = BudgetEntry.Type.INCOME,
            date = "2024-01-01",
            invoice = null,
            category = BudgetEntry.Category.OTHER
        )

        assertEquals(BudgetEntry.Type.OUTCOME, outcomeEntry.toDomain().type)
        assertEquals(BudgetEntry.Type.INCOME, incomeEntry.toDomain().type)
    }

    @Test
    fun `toDomain handles all category types`() {
        val categories = BudgetEntry.getCategories()

        categories.forEach { category ->
            val dbEntry = Budget_entry(
                id = 1L,
                budget_id = 1L,
                amount = 100.0,
                description = "Test",
                type = BudgetEntry.Type.OUTCOME,
                date = "2024-01-01",
                invoice = null,
                category = category
            )

            val domainEntry = dbEntry.toDomain()
            assertEquals(category, domainEntry.category)
        }
    }

    @Test
    fun `toDomain handles large amounts`() {
        val dbEntry = Budget_entry(
            id = 1L,
            budget_id = 1L,
            amount = 999999.99,
            description = "Large amount",
            type = BudgetEntry.Type.OUTCOME,
            date = "2024-01-01",
            invoice = null,
            category = BudgetEntry.Category.OTHER
        )

        val domainEntry = dbEntry.toDomain()

        assertEquals("999999.99", domainEntry.amount)
    }

    @Test
    fun `toDomain handles zero amount`() {
        val dbEntry = Budget_entry(
            id = 1L,
            budget_id = 1L,
            amount = 0.0,
            description = "Zero",
            type = BudgetEntry.Type.OUTCOME,
            date = "2024-01-01",
            invoice = null,
            category = BudgetEntry.Category.OTHER
        )

        val domainEntry = dbEntry.toDomain()

        assertEquals("0", domainEntry.amount)
    }

    @Test
    fun `toDomain handles amounts with rounding`() {
        val dbEntry = Budget_entry(
            id = 1L,
            budget_id = 1L,
            amount = 123.456789,
            description = "Precise",
            type = BudgetEntry.Type.OUTCOME,
            date = "2024-01-01",
            invoice = null,
            category = BudgetEntry.Category.OTHER
        )

        val domainEntry = dbEntry.toDomain()

        // toPlainString should round to 2 decimal places
        assertEquals("123.46", domainEntry.amount)
    }
}
