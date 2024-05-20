package com.meneses.budgethunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import com.meneses.budgethunter.budgetDetail.ui.BudgetDetailScreen
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.ui.BudgetEntryScreen
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.ui.BudgetListScreen
import com.meneses.budgethunter.commons.util.serializableType
import com.meneses.budgethunter.splash.SplashScreen
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.BudgetHunterTheme
import com.meneses.budgethunter.userGuide.UserGuideScreen
import kotlin.reflect.typeOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BudgetHunterTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = SplashScreen
                    ) {
                        composable<SplashScreen> {
                            SplashScreen.Show(
                                onNavigate = {
                                    navController.navigate(
                                        route = BudgetListScreen,
                                        navOptions = navOptions {
                                            popUpTo<SplashScreen> {
                                                inclusive = true
                                            }
                                        }
                                    )
                                }
                            )
                        }

                        composable<BudgetListScreen> {
                            BudgetListScreen.Show(
                                showUserGuide = {
                                    navController.navigate(UserGuideScreen)
                                },
                                showBudgetDetail = { budget ->
                                    navController.navigate(BudgetDetailScreen(budget))
                                }
                            )
                        }

                        composable<UserGuideScreen> {
                            UserGuideScreen.Show(
                                goBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable<BudgetDetailScreen>(
                            typeMap = mapOf(typeOf<Budget>() to serializableType<Budget>())
                        ) {
                            it.toRoute<BudgetDetailScreen>().Show(
                                showBudgetEntry = { budgetEntry ->
                                    navController.navigate(BudgetEntryScreen(budgetEntry))
                                },
                                goBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable<BudgetEntryScreen>(
                            typeMap = mapOf(typeOf<BudgetEntry>() to serializableType<BudgetEntry>()),
                        ) {
                            it.toRoute<BudgetEntryScreen>().Show(
                                goBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
