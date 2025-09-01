package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.db.Budget_entry

fun Budget_entry.toDomain() =
    BudgetEntry(
        id = id.toInt(),
        budgetId = budget_id.toInt(),
        amount = amount.toString(),
        description = description,
        type = type,
        date = date,
        invoice = invoice,
        category = category
    )

fun List<Budget_entry>.toDomain() = map { it.toDomain() }