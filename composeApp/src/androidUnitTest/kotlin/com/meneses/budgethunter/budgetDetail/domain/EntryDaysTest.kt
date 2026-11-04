package com.meneses.budgethunter.budgetDetail.domain

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EntryDaysTest {

    private fun entry(
        date: String,
        amount: String = "10",
        type: BudgetEntry.Type = BudgetEntry.Type.OUTCOME
    ) = BudgetEntry(amount = amount, date = date, type = type)

    @Test
    fun `empty list gives no days`() {
        assertTrue(emptyList<BudgetEntry>().groupedByDay().isEmpty())
    }

    @Test
    fun `consecutive entries of the same date make one group with their original indexes`() {
        val days = listOf(entry("2026-10-03"), entry("2026-10-03"), entry("2026-10-03")).groupedByDay()

        assertEquals(1, days.size)
        assertEquals("2026-10-03", days[0].date)
        assertEquals(listOf(0, 1, 2), days[0].entries.map { it.index })
    }

    @Test
    fun `a date change starts a new group and indexes keep counting over the whole list`() {
        val days = listOf(entry("2026-10-03"), entry("2026-10-03"), entry("2026-10-02")).groupedByDay()

        assertEquals(listOf("2026-10-03", "2026-10-02"), days.map { it.date })
        assertEquals(listOf(0, 1), days[0].entries.map { it.index })
        assertEquals(listOf(2), days[1].entries.map { it.index })
    }

    @Test
    fun `net is income minus expenses of the day`() {
        val days = listOf(
            entry("2026-10-03", "100", BudgetEntry.Type.INCOME),
            entry("2026-10-03", "30.5"),
            entry("2026-10-03", "20")
        ).groupedByDay()

        assertEquals(49.5, days.single().net)
    }

    @Test
    fun `net is negative when the day only has expenses`() {
        assertEquals(-10.0, listOf(entry("2026-10-03")).groupedByDay().single().net)
    }

    @Test
    fun `a non numeric amount counts as zero`() {
        val days = listOf(
            entry("2026-10-03", "abc"),
            entry("2026-10-03", "", BudgetEntry.Type.INCOME),
            entry("2026-10-03", "5")
        ).groupedByDay()

        assertEquals(-5.0, days.single().net)
    }

    @Test
    fun `the same date appearing again later starts a second group`() {
        val days = listOf(entry("2026-10-03"), entry("2026-10-02"), entry("2026-10-03")).groupedByDay()

        assertEquals(listOf("2026-10-03", "2026-10-02", "2026-10-03"), days.map { it.date })
        assertEquals(listOf(2), days[2].entries.map { it.index })
    }
}
