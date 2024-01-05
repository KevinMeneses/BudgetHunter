package com.meneses.budgethunter

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.meneses.budgethunter.commons.data.initDatabaseFactory
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.BudgetHunterTheme
import moe.tlaster.precompose.PreComposeApp

@Composable
fun App() {
    PreComposeApp {
        BudgetHunterTheme {
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = AppColors.background
            ) {
                NavigationRouter()
            }
        }
    }

    val context = LocalContext.current

    LaunchedEffect(context) {
        initDatabaseFactory(context)
    }
}
