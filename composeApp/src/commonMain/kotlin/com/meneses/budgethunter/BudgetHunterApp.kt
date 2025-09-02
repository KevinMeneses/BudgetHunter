package com.meneses.budgethunter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.meneses.budgethunter.splash.application.SplashState
import com.meneses.budgethunter.splash.ui.SplashScreen
import com.meneses.budgethunter.theme.BudgetHunterTheme

/**
 * Main entry point for the BudgetHunter Compose Multiplatform app.
 * This composable represents the entire app and will be expanded as we migrate screens.
 */
@Composable
fun BudgetHunterApp() {
    BudgetHunterTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            // TODO: Add navigation here - temporarily showing SplashScreen
            SplashScreen.Show(
                uiState = SplashState(),
                onEvent = { /* TODO: Handle events */ },
                showBudgetList = { /* TODO: Navigate to BudgetList */ }
            )
        }
    }
}

/**
 * Temporary placeholder screen while the migration is in progress.
 * This will be replaced with the actual app navigation and screens.
 */
@Composable
private fun PlaceholderScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Budget Hunter - KMP Migration in Progress")
    }
}
