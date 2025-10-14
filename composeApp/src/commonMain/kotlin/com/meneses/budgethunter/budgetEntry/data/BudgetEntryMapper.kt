package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.db.Budget_entry
import com.meneses.budgethunter.commons.util.toPlainString

fun Budget_entry.toDomain() =
    BudgetEntry(
        id = id.toInt(),
        budgetId = budget_id.toInt(),
        amount = amount.toPlainString(),
        description = description,
        type = type,
        date = date,
        invoice = invoice,
        category = category,
        serverId = server_id,
        isSynced = is_synced == 1L,
        createdByEmail = created_by_email,
        updatedByEmail = updated_by_email,
        creationDate = creation_date,
        modificationDate = modification_date
    )

fun List<Budget_entry>.toDomain() = map { it.toDomain() }