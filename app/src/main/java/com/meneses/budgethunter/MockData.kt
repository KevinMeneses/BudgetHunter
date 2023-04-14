package com.meneses.budgethunter

import androidx.navigation.NavOptionsBuilder
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.insAndOuts.domain.BudgetItem
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

val budgetItemLists = mutableListOf(
    BudgetItem(
        amount = 20000.0,
        description = "Papitas",
        type = BudgetItem.Type.OUTCOME
    ),
    BudgetItem(
        amount = 50000.0,
        description = "Pollo",
        type = BudgetItem.Type.OUTCOME
    ),
    BudgetItem(
        amount = 10000.0,
        description = "Gaseosa",
        type = BudgetItem.Type.OUTCOME
    ),
    BudgetItem(
        amount = 8000.0,
        description = null,
        type = BudgetItem.Type.OUTCOME
    ),
    BudgetItem(
        amount = 20000.0,
        description = "Pago salida",
        type = BudgetItem.Type.INCOME
    ),
    BudgetItem(
        amount = 17000.0,
        description = "Carne",
        type = BudgetItem.Type.OUTCOME
    ),
    BudgetItem(
        amount = 12000.0,
        description = "Frijoles",
        type = BudgetItem.Type.OUTCOME
    )
)

val budgetList = mutableListOf(
    Budget(
        id = 0,
        name = "Mensual",
        frequency = Budget.Frequency.MONTHLY
    ),
    Budget(
        id = 0,
        name = "Paseo",
        frequency = Budget.Frequency.UNIQUE
    ),
    Budget(
        id = 0,
        name = "Anual",
        frequency = Budget.Frequency.ANNUAL
    )
)

val budgetItemTypeList = listOf(
    BudgetItem.Type.OUTCOME.value,
    BudgetItem.Type.INCOME.value
)

val budgetListFilterOptions = budgetItemTypeList
    .toMutableList()
    .apply { add("Todo") }

const val totalIncome = 10000000.0

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