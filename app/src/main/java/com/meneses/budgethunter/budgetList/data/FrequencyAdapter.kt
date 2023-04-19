package com.meneses.budgethunter.budgetList.data

import app.cash.sqldelight.ColumnAdapter
import com.meneses.budgethunter.budgetList.domain.Budget

val frequencyAdapter = object : ColumnAdapter<Budget.Frequency, String> {
    override fun decode(databaseValue: String) = Budget.Frequency.getByValue(databaseValue)
    override fun encode(value: Budget.Frequency) = value.value
}