package com.meneses.budgethunter

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meneses.budgethunter.navigation.BudgetHunterNavigation
import com.meneses.budgethunter.theme.BudgetHunterTheme

@Composable
fun BudgetHunterApp() {
    BudgetHunterTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BudgetHunterNavigation()
        }
    }
}
