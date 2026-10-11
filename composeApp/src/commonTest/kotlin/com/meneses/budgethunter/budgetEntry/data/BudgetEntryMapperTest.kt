package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetEntryMapperTest {

    @Test
    fun `the server's UNCATEGORIZED is read as Sin categoria`() {
        assertEquals(BudgetEntry.Category.UNCATEGORIZED, "UNCATEGORIZED".toBudgetEntryCategory())
        assertEquals(BudgetEntry.Category.UNCATEGORIZED, "uncategorized".toBudgetEntryCategory())
    }

    @Test
    fun `a value this build does not know is Other, not Sin categoria`() {
        // Otherwise a category added later would make an entry look like it is waiting to be categorized.
        assertEquals(BudgetEntry.Category.OTHER, "SOMETHING_NEW".toBudgetEntryCategory())
    }

    @Test
    fun `an entry with no category is sent as UNCATEGORIZED, which the server stores as is`() {
        val entry = BudgetEntry(amount = "10.0", description = "coffee")

        assertEquals("UNCATEGORIZED", entry.toCreateRequest().category)
        assertEquals("UNCATEGORIZED", entry.toUpdateRequest().category)
    }

    @Test
    fun `an entry with a category is sent with it, Other included`() {
        assertEquals("FOOD", BudgetEntry(category = BudgetEntry.Category.FOOD).toCreateRequest().category)
        assertEquals("OTHER", BudgetEntry(category = BudgetEntry.Category.OTHER).toCreateRequest().category)
    }
}
