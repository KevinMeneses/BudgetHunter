package com.meneses.budgethunter.budgetEntry.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BudgetEntryFilterTest {

    private val today = LocalDate(2026, 3, 15)

    private fun entry(
        description: String = "Entry",
        type: BudgetEntry.Type = BudgetEntry.Type.OUTCOME,
        category: BudgetEntry.Category = BudgetEntry.Category.OTHER,
        date: String = "2026-03-10"
    ) = BudgetEntry(description = description, type = type, category = category, date = date)

    private fun List<BudgetEntry>.descriptions(filter: BudgetEntryFilter) =
        filteredBy(filter, today).map { it.description }

    // ── isActive ─────────────────────────────────────────────────────────────

    @Test
    fun `an empty filter is not active`() {
        assertFalse(BudgetEntryFilter().isActive)
    }

    @Test
    fun `a blank description alone does not make the filter active`() {
        assertFalse(BudgetEntryFilter(description = "   ").isActive)
    }

    @Test
    fun `any criterion makes the filter active`() {
        assertTrue(BudgetEntryFilter(description = "a").isActive)
        assertTrue(BudgetEntryFilter(type = BudgetEntry.Type.INCOME).isActive)
        assertTrue(BudgetEntryFilter(category = BudgetEntry.Category.FOOD).isActive)
        assertTrue(BudgetEntryFilter(dateRange = EntryDateRange.ThisMonth).isActive)
    }

    // ── matching ─────────────────────────────────────────────────────────────

    @Test
    fun `an empty filter lets every entry through`() {
        val entries = listOf(entry("A"), entry("B"))

        assertEquals(entries, entries.filteredBy(BudgetEntryFilter(), today))
    }

    @Test
    fun `description matches anywhere - ignoring case and surrounding spaces`() {
        val entries = listOf(entry("Groceries Shopping"), entry("Restaurant Meal"), entry("Grocery Store"))

        assertEquals(
            listOf("Groceries Shopping", "Grocery Store"),
            entries.descriptions(BudgetEntryFilter(description = " GROCER "))
        )
    }

    @Test
    fun `type keeps only that type`() {
        val entries = listOf(
            entry("Salary", type = BudgetEntry.Type.INCOME),
            entry("Groceries", type = BudgetEntry.Type.OUTCOME)
        )

        assertEquals(listOf("Salary"), entries.descriptions(BudgetEntryFilter(type = BudgetEntry.Type.INCOME)))
    }

    @Test
    fun `category keeps only that category - uncategorized included`() {
        val entries = listOf(
            entry("Supermarket", category = BudgetEntry.Category.GROCERIES),
            entry("SMS", category = BudgetEntry.Category.UNCATEGORIZED)
        )

        assertEquals(
            listOf("SMS"),
            entries.descriptions(BudgetEntryFilter(category = BudgetEntry.Category.UNCATEGORIZED))
        )
    }

    @Test
    fun `all criteria must match together`() {
        val entries = listOf(
            entry("Groceries", BudgetEntry.Type.OUTCOME, BudgetEntry.Category.GROCERIES, "2026-03-10"),
            entry("Grocery refund", BudgetEntry.Type.INCOME, BudgetEntry.Category.GROCERIES, "2026-03-10"),
            entry("Groceries", BudgetEntry.Type.OUTCOME, BudgetEntry.Category.FOOD, "2026-03-10"),
            entry("Groceries old", BudgetEntry.Type.OUTCOME, BudgetEntry.Category.GROCERIES, "2026-01-10")
        )

        val filter = BudgetEntryFilter(
            description = "grocer",
            type = BudgetEntry.Type.OUTCOME,
            category = BudgetEntry.Category.GROCERIES,
            dateRange = EntryDateRange.ThisMonth
        )

        assertEquals(listOf("Groceries"), entries.descriptions(filter))
    }

    // ── date ranges ──────────────────────────────────────────────────────────

    @Test
    fun `this month goes from the first to the last day of the current month`() {
        assertEquals(
            LocalDate(2026, 3, 1) to LocalDate(2026, 3, 31),
            EntryDateRange.ThisMonth.resolve(today)
        )
    }

    @Test
    fun `last month is the whole previous month`() {
        assertEquals(
            LocalDate(2026, 2, 1) to LocalDate(2026, 2, 28),
            EntryDateRange.LastMonth.resolve(today)
        )
    }

    @Test
    fun `last month in January is December of the year before`() {
        assertEquals(
            LocalDate(2025, 12, 1) to LocalDate(2025, 12, 31),
            EntryDateRange.LastMonth.resolve(LocalDate(2026, 1, 1))
        )
    }

    @Test
    fun `last 7 days are today and the six days before`() {
        assertEquals(
            LocalDate(2026, 3, 9) to LocalDate(2026, 3, 15),
            EntryDateRange.Last7Days.resolve(today)
        )
    }

    @Test
    fun `a date range includes both ends`() {
        val entries = listOf(
            entry("Before", date = "2026-03-09"),
            entry("Start", date = "2026-03-10"),
            entry("End", date = "2026-03-20"),
            entry("After", date = "2026-03-21")
        )
        val range = EntryDateRange.Custom(LocalDate(2026, 3, 10), LocalDate(2026, 3, 20))

        assertEquals(listOf("Start", "End"), entries.descriptions(BudgetEntryFilter(dateRange = range)))
    }

    @Test
    fun `this month leaves out the end of the previous month`() {
        val entries = listOf(entry("February", date = "2026-02-28"), entry("March", date = "2026-03-01"))

        assertEquals(listOf("March"), entries.descriptions(BudgetEntryFilter(dateRange = EntryDateRange.ThisMonth)))
    }

    @Test
    fun `this month in February of a leap year ends on the 29th`() {
        assertEquals(
            LocalDate(2028, 2, 1) to LocalDate(2028, 2, 29),
            EntryDateRange.ThisMonth.resolve(LocalDate(2028, 2, 10))
        )
    }

    @Test
    fun `this month in December ends on the 31st`() {
        assertEquals(
            LocalDate(2026, 12, 1) to LocalDate(2026, 12, 31),
            EntryDateRange.ThisMonth.resolve(LocalDate(2026, 12, 31))
        )
    }

    @Test
    fun `last 7 days can start in the previous month`() {
        assertEquals(
            LocalDate(2026, 2, 26) to LocalDate(2026, 3, 4),
            EntryDateRange.Last7Days.resolve(LocalDate(2026, 3, 4))
        )
    }

    @Test
    fun `a one-day custom range keeps only that day - across years too`() {
        val entries = listOf(entry("New year", date = "2026-01-01"), entry("Eve", date = "2025-12-31"))

        val oneDay = EntryDateRange.Custom(LocalDate(2026, 1, 1), LocalDate(2026, 1, 1))
        val acrossYears = EntryDateRange.Custom(LocalDate(2025, 12, 31), LocalDate(2026, 1, 1))

        assertEquals(listOf("New year"), entries.descriptions(BudgetEntryFilter(dateRange = oneDay)))
        assertEquals(listOf("New year", "Eve"), entries.descriptions(BudgetEntryFilter(dateRange = acrossYears)))
    }

    @Test
    fun `a blank description is ignored next to another criterion`() {
        val entries = listOf(entry("Salary", type = BudgetEntry.Type.INCOME), entry("Rent"))

        val filter = BudgetEntryFilter(description = "  ", type = BudgetEntry.Type.INCOME)

        assertEquals(listOf("Salary"), entries.descriptions(filter))
    }
}
