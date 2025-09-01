package com.meneses.budgethunter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Main entry point for the BudgetHunter Compose Multiplatform app.
 * This composable represents the entire app and will be expanded as we migrate screens.
 */
@Composable
fun BudgetHunterApp() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // TODO: Add navigation and screens here during migration
            // For now, this is a placeholder that will be expanded as we migrate from the Android app
            PlaceholderScreen()
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