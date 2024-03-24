package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.db.Budget_entry

fun BudgetEntry.toDb() =
    Budget_entry(
        id = id.toLong(),
        budget_id = budgetId.toLong(),
        amount = amount.toDouble(),
        description = description,
        type = type,
        date = date,
        invoice = invoice
    )

fun Budget_entry.toDomain() =
    BudgetEntry(
        id = id.toInt(),
        budgetId = budget_id.toInt(),
        amount = amount.toBigDecimal().toPlainString(),
        description = description,
        type = type,
        date = date,
        invoice = invoice
    )

fun List<Budget_entry>.toDomain() = map { it.toDomain() }
