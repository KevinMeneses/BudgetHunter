package com.meneses.budgethunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.BudgetHunterTheme
import com.ramcosta.composedestinations.DestinationsNavHost

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
                   DestinationsNavHost(navGraph = NavGraphs.root)
                }
            }
        }
    }
}