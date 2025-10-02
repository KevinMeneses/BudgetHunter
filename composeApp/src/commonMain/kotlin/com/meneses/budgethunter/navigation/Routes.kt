package com.meneses.budgethunter.navigation

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import kotlinx.serialization.Serializable

@Serializable
object SplashScreen

@Serializable
object BudgetListScreen

@Serializable
data class BudgetDetailScreen(val budget: Budget)

@Serializable
data class BudgetEntryScreen(val budgetEntry: BudgetEntry)

@Serializable
data class BudgetMetricsScreen(val budget: Budget)

@Serializable
object SettingsScreen