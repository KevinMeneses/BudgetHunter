package com.meneses.budgethunter

import androidx.compose.ui.window.ComposeUIViewController
import com.meneses.budgethunter.di.startKoinIfNeeded
import platform.UIKit.UIViewController

/**
 * Creates the main iOS view controller that hosts the Compose Multiplatform content.
 * This function is called from the iOS Swift code to bridge into the shared Compose UI.
 */
fun MainViewController(): UIViewController {
    // The SMS shortcut may have started Koin already, with the app in the background
    startKoinIfNeeded()

    return ComposeUIViewController {
        BudgetHunterApp()
    }
}
