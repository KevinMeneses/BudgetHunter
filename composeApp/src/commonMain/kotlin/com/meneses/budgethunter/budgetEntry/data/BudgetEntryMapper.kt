package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.data.network.models.CreateBudgetEntryRequest
import com.meneses.budgethunter.commons.data.network.models.UpdateBudgetEntryRequest
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

fun BudgetEntry.toCreateRequest() = CreateBudgetEntryRequest(
    amount = amount.toDoubleOrNull() ?: 0.0,
    description = description,
    category = category.name,
    type = type.name
)

fun BudgetEntry.toUpdateRequest() = UpdateBudgetEntryRequest(
    amount = amount.toDoubleOrNull() ?: 0.0,
    description = description,
    category = category.name,
    type = type.name
)

fun String.toBudgetEntryType(): BudgetEntry.Type =
    enumValues<BudgetEntry.Type>().firstOrNull { it.name.equals(this, ignoreCase = true) }
        ?: BudgetEntry.Type.OUTCOME

fun String.toBudgetEntryCategory(): BudgetEntry.Category =
    enumValues<BudgetEntry.Category>().firstOrNull { it.name.equals(this, ignoreCase = true) }
        ?: BudgetEntry.Category.OTHER
