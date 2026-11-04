package com.meneses.budgethunter.budgetDetail.domain

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

/**
 * The entries of one day as the detail list shows them, under a header with the day's net.
 *
 * @property entries each entry with its position in the whole list, which the selection intents
 * expect
 * @property net income minus expenses of the day
 */
data class EntryDay(
    val date: String,
    val entries: List<IndexedValue<BudgetEntry>>,
    val net: Double
)

/**
 * Splits the list into days, one group per run of entries with the same date, keeping the list's
 * order. Meant for a list sorted by date: the same date further down would start a second group.
 */
fun List<BudgetEntry>.groupedByDay(): List<EntryDay> {
    val days = mutableListOf<EntryDay>()
    var current = mutableListOf<IndexedValue<BudgetEntry>>()

    fun closeDay() {
        val first = current.firstOrNull() ?: return
        days += EntryDay(date = first.value.date, entries = current, net = current.sumOf { it.value.signedAmount() })
        current = mutableListOf()
    }

    withIndex().forEach { indexed ->
        if (current.isNotEmpty() && current.last().value.date != indexed.value.date) closeDay()
        current += indexed
    }
    closeDay()
    return days
}

/** The amount as it moves the balance: positive for income, negative for expenses. */
private fun BudgetEntry.signedAmount(): Double {
    val value = amount.toDoubleOrNull() ?: 0.0
    return if (type == BudgetEntry.Type.INCOME) value else -value
}
