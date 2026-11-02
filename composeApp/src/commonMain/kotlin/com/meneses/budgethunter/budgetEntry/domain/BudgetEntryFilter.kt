package com.meneses.budgethunter.budgetEntry.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * What the budget detail list is narrowed to. Every criterion left null (or a blank
 * [description]) lets every entry through.
 */
data class BudgetEntryFilter(
    val description: String? = null,
    val type: BudgetEntry.Type? = null,
    val category: BudgetEntry.Category? = null,
    val dateRange: EntryDateRange? = null
) {
    val isActive: Boolean
        get() = !description.isNullOrBlank() || type != null || category != null || dateRange != null

    /**
     * @param today the day the relative ranges ([EntryDateRange.ThisMonth], ...) are counted from
     */
    fun matches(entry: BudgetEntry, today: LocalDate): Boolean {
        if (!description.isNullOrBlank() && !entry.description.contains(description.trim(), ignoreCase = true)) {
            return false
        }
        if (type != null && entry.type != type) return false
        if (category != null && entry.category != category) return false
        if (dateRange != null) {
            // Entries keep their date as "yyyy-MM-dd", which sorts like the date itself
            val (start, end) = dateRange.resolve(today)
            if (entry.date < start.toString() || entry.date > end.toString()) return false
        }
        return true
    }
}

fun List<BudgetEntry>.filteredBy(filter: BudgetEntryFilter, today: LocalDate): List<BudgetEntry> =
    if (filter.isActive) filter { filter.matches(it, today) } else this

/**
 * The dates an entry may have to pass the filter, both ends included.
 *
 * The presets are relative to the day the filter is applied, so "this month" keeps meaning the
 * current month without the user having to pick dates.
 */
sealed interface EntryDateRange {
    fun resolve(today: LocalDate): Pair<LocalDate, LocalDate>

    data object ThisMonth : EntryDateRange {
        override fun resolve(today: LocalDate) = today.firstDayOfMonth() to today.lastDayOfMonth()
    }

    data object LastMonth : EntryDateRange {
        override fun resolve(today: LocalDate): Pair<LocalDate, LocalDate> {
            val dayInLastMonth = today.firstDayOfMonth().minus(DatePeriod(days = 1))
            return dayInLastMonth.firstDayOfMonth() to dayInLastMonth
        }
    }

    /** Today and the six days before it. */
    data object Last7Days : EntryDateRange {
        override fun resolve(today: LocalDate) = today.minus(DatePeriod(days = 6)) to today
    }

    data class Custom(val start: LocalDate, val end: LocalDate) : EntryDateRange {
        override fun resolve(today: LocalDate) = start to end
    }
}

private fun LocalDate.firstDayOfMonth() = LocalDate(year, month, 1)

private fun LocalDate.lastDayOfMonth() = firstDayOfMonth().plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
