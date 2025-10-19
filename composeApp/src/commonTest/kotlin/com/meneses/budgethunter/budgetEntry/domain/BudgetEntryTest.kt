package com.meneses.budgethunter.budgetEntry.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BudgetEntryTest {

    @Test
    fun `getItemTypes returns both outcome and income`() {
        val types = BudgetEntry.getItemTypes()

        assertTrue(types.contains(BudgetEntry.Type.OUTCOME))
        assertTrue(types.contains(BudgetEntry.Type.INCOME))
        assertEquals(2, types.size)
    }

    @Test
    fun `getCategories returns all available categories`() {
        val categories = BudgetEntry.getCategories()

        assertEquals(11, categories.size)
        assertTrue(categories.contains(BudgetEntry.Category.FOOD))
        assertTrue(categories.contains(BudgetEntry.Category.OTHER))
    }

    @Test
    fun `type toStringResource maps enum to readable text`() {
        assertEquals("Outcome", BudgetEntry.Type.OUTCOME.toStringResource())
        assertEquals("Income", BudgetEntry.Type.INCOME.toStringResource())
    }

    @Test
    fun `category toStringResource maps enum to readable text`() {
        assertEquals("Food", BudgetEntry.Category.FOOD.toStringResource())
        assertEquals("Other", BudgetEntry.Category.OTHER.toStringResource())
        assertFalse(BudgetEntry.Category.HEALTH.toStringResource().isBlank())
    }
}
