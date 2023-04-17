package com.meneses.budgethunter

import androidx.navigation.NavOptionsBuilder
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

val budgetEntryList = mutableListOf(
    BudgetEntry(
        id = 0,
        budgetId = 0,
        amount = 20000.0,
        description = "Papitas",
        type = BudgetEntry.Type.OUTCOME
    ),
    BudgetEntry(
        id = 1,
        budgetId = 0,
        amount = 50000.0,
        description = "Pollo",
        type = BudgetEntry.Type.OUTCOME
    ),
    BudgetEntry(
        id = 2,
        budgetId = 1,
        amount = 10000.0,
        description = "Gaseosa",
        type = BudgetEntry.Type.OUTCOME
    ),
    BudgetEntry(
        id = 3,
        budgetId = 1,
        amount = 8000.0,
        description = null,
        type = BudgetEntry.Type.OUTCOME
    ),
    BudgetEntry(
        id = 4,
        budgetId = 2,
        amount = 20000.0,
        description = "Pago salida",
        type = BudgetEntry.Type.INCOME
    ),
    BudgetEntry(
        id = 5,
        budgetId = 2,
        amount = 17000.0,
        description = "Carne",
        type = BudgetEntry.Type.OUTCOME
    ),
    BudgetEntry(
        id = 6,
        budgetId = 2,
        amount = 12000.0,
        description = "Frijoles",
        type = BudgetEntry.Type.OUTCOME
    )
)

val budgetListMock = mutableListOf(
    Budget(
        id = 0,
        amount = 200000.0,
        name = "Mensual",
        frequency = Budget.Frequency.MONTHLY
    ),
    Budget(
        id = 1,
        amount = 120000.0,
        name = "Paseo",
        frequency = Budget.Frequency.UNIQUE
    ),
    Budget(
        id = 2,
        amount = 900000.0,
        name = "Anual",
        frequency = Budget.Frequency.ANNUAL
    )
)

val fakeNavigation = object : DestinationsNavigator {
    override fun clearBackStack(route: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun navigate(
        route: String,
        onlyIfResumed: Boolean,
        builder: NavOptionsBuilder.() -> Unit
    ) {
        TODO("Not yet implemented")
    }

    override fun navigateUp(): Boolean {
        TODO("Not yet implemented")
    }

    override fun popBackStack(): Boolean {
        TODO("Not yet implemented")
    }

    override fun popBackStack(route: String, inclusive: Boolean, saveState: Boolean): Boolean {
        TODO("Not yet implemented")
    }

}