package com.meneses.budgethunter

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Creates the main iOS view controller that hosts the Compose Multiplatform content.
 * This function is called from the iOS Swift code to bridge into the shared Compose UI.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    BudgetHunterApp()
}