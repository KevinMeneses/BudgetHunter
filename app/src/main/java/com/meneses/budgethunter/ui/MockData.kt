package com.meneses.budgethunter.ui

import androidx.navigation.NavOptionsBuilder
import com.meneses.budgethunter.model.Budget
import com.meneses.budgethunter.model.BudgetDetail
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

val budgetDetailLists = listOf(
    BudgetDetail(
        amount = 20000.0,
        description = "Papitas",
        type = BudgetDetail.Type.OUTCOME
    ),
    BudgetDetail(
        amount = 50000.0,
        description = "Pollo",
        type = BudgetDetail.Type.OUTCOME
    ),
    BudgetDetail(
        amount = 10000.0,
        description = "Gaseosa",
        type = BudgetDetail.Type.OUTCOME
    ),
    BudgetDetail(
        amount = 8000.0,
        description = null,
        type = BudgetDetail.Type.OUTCOME
    ),
    BudgetDetail(
        amount = 20000.0,
        description = "Pago salida",
        type = BudgetDetail.Type.INCOME
    ),
    BudgetDetail(
        amount = 17000.0,
        description = "Carne",
        type = BudgetDetail.Type.OUTCOME
    ),
    BudgetDetail(
        amount = 12000.0,
        description = "Frijoles",
        type = BudgetDetail.Type.OUTCOME
    )
)

val budgetList = listOf(
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

val budgetDetailTypeList = listOf(
    BudgetDetail.Type.OUTCOME.value,
    BudgetDetail.Type.INCOME.value
)

val budgetListFilterOptions = budgetDetailTypeList
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