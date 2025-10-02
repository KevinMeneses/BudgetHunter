package com.meneses.budgethunter.budgetList.data.adapter

import app.cash.sqldelight.ColumnAdapter
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

val categoryAdapter = object : ColumnAdapter<BudgetEntry.Category, String> {
    override fun decode(databaseValue: String) = BudgetEntry.Category.valueOf(databaseValue)
    override fun encode(value: BudgetEntry.Category) = value.name
}