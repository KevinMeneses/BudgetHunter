package com.meneses.budgethunter.budgetList.data.adapter

import app.cash.sqldelight.ColumnAdapter
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

val typeAdapter = object : ColumnAdapter<BudgetEntry.Type, String> {
    override fun decode(databaseValue: String) = BudgetEntry.Type.valueOf(databaseValue)
    override fun encode(value: BudgetEntry.Type) = value.name
}