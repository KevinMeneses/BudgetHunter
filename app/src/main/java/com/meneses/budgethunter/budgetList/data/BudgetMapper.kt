package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.db.Budget as DbBudget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun DbBudget.toDomain() = Budget(
    id = id.toInt(),
    amount = amount,
    name = name,
    frequency = frequency
)

fun Budget.toDb() = DbBudget(
    id = id.toLong(),
    amount = amount,
    name = name,
    frequency = frequency
)

fun Flow<List<DbBudget>>.toDomain() =
    map { list -> list.map { budget -> budget.toDomain() } }
