package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.budgetList.domain.Budget

fun mapSelectAllToBudget(
    id: Long,
    amount: Double,
    name: String,
    date: String,
    serverId: Long?,
    isSynced: Long,
    lastSyncedAt: String?,
    totalExpenses: Double
) = Budget(
    id = id.toInt(), // SQLite/server IDs are Long; domain model uses Int for local IDs
    amount = amount,
    name = name,
    totalExpenses = totalExpenses,
    date = date,
    serverId = serverId,
    isSynced = isSynced == 1L,
    lastSyncedAt = lastSyncedAt
)
