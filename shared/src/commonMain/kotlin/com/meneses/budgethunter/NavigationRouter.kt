package com.meneses.budgethunter

import androidx.compose.runtime.Composable
import com.meneses.budgethunter.budgetDetail.ui.BudgetDetailScreen
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.ui.BudgetEntryScreen
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.ui.BudgetListScreen
import com.meneses.budgethunter.splash.SplashScreen
import com.meneses.budgethunter.userGuide.UserGuideScreen
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.NavOptions
import moe.tlaster.precompose.navigation.PopUpTo
import moe.tlaster.precompose.navigation.path
import moe.tlaster.precompose.navigation.rememberNavigator
import moe.tlaster.precompose.navigation.transition.NavTransition

@Composable
fun NavigationRouter() {
    val navigator = rememberNavigator()
    NavHost(
        navigator = navigator,
        navTransition = NavTransition(),
        initialRoute = "/splash",
    ) {
        scene(route = "/splash") {
            SplashScreen (
                onFinished = {
                    navigator.navigate(
                        route = "/budget_list",
                        options = NavOptions(
                            popUpTo = PopUpTo(
                                route = "/splash",
                                inclusive = true
                            )
                        )
                    )
                }
            )
        }

        scene(route = "/user_guide") {
            UserGuideScreen(
                onBackPressed = { navigator.popBackStack() }
            )
        }

        scene(route = "/budget_list") {
            BudgetListScreen(
                onHelpClick = { navigator.navigate("/user_guide") },
                onBudgetClick = { budget ->
                    val budgetJson = Json.encodeToString(budget)
                    navigator.navigate("/budget_detail/$budgetJson")
                }
            )
        }

        scene(route = "/budget_detail/{budget}") {
            val budgetJson = it.path<String>("budget").orEmpty()
            val budget: Budget = Json.decodeFromString(budgetJson)
            BudgetDetailScreen(
                budget = budget,
                onGoBack = { navigator.popBackStack() },
                onShowEntry = { entry ->
                    val entryJson = Json.encodeToString(entry)
                    navigator.navigate("/budget_entry/$entryJson")
                }
            )
        }

        scene(route = "/budget_entry/{entry}") {
            val entryJson = it.path<String>("entry").orEmpty()
            val entry: BudgetEntry = Json.decodeFromString(entryJson)
            BudgetEntryScreen(
                budgetEntry = entry,
                onGoBack = { navigator.popBackStack() }
            )
        }
    }
}
