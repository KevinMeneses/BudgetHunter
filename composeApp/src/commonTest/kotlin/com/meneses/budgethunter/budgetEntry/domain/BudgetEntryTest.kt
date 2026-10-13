package com.meneses.budgethunter.budgetEntry.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BudgetEntryTest {

    @Test
    fun `a new entry has no category`() {
        assertEquals(BudgetEntry.Category.UNCATEGORIZED, BudgetEntry().category)
    }

    @Test
    fun `the assignable categories are the original eleven, in order, without the absence of one`() {
        assertEquals(
            listOf(
                BudgetEntry.Category.FOOD,
                BudgetEntry.Category.GROCERIES,
                BudgetEntry.Category.SELF_CARE,
                BudgetEntry.Category.TRANSPORTATION,
                BudgetEntry.Category.HOUSEHOLD_ITEMS,
                BudgetEntry.Category.SERVICES,
                BudgetEntry.Category.EDUCATION,
                BudgetEntry.Category.HEALTH,
                BudgetEntry.Category.LEISURE,
                BudgetEntry.Category.TAXES,
                BudgetEntry.Category.OTHER
            ),
            BudgetEntry.getAssignableCategories()
        )
        assertFalse(BudgetEntry.getAssignableCategories().contains(BudgetEntry.Category.UNCATEGORIZED))
    }

    @Test
    fun `what a person can pick or see in the metrics starts with Sin categoria and has every category once`() {
        val categories = BudgetEntry.getCategories()

        assertEquals(BudgetEntry.Category.UNCATEGORIZED, categories.first())
        assertEquals(BudgetEntry.Category.entries.toSet(), categories.toSet())
        assertEquals(categories.size, categories.toSet().size)
    }

    @Test
    fun `OTHER is a different thing from having no category`() {
        assertTrue(BudgetEntry.Category.OTHER != BudgetEntry.Category.UNCATEGORIZED)
        assertTrue(BudgetEntry.getAssignableCategories().contains(BudgetEntry.Category.OTHER))
    }
}
