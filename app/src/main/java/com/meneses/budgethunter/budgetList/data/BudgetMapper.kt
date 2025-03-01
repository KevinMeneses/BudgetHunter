package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.budgetList.domain.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.meneses.budgethunter.db.Budget as DbBudget

fun DbBudget.toDomain() = Budget(
    id = id.toInt(),
    amount = amount,
    name = name
)

fun Flow<List<DbBudget>>.toDomain() =
    map { list -> list.map { budget -> budget.toDomain() } }
