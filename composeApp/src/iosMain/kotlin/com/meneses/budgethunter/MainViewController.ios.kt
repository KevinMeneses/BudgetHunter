package com.meneses.budgethunter

import androidx.compose.ui.window.ComposeUIViewController
import com.meneses.budgethunter.di.initKoin
import com.meneses.budgethunter.di.iosPlatformModule
import platform.UIKit.UIViewController

/**
 * Creates the main iOS view controller that hosts the Compose Multiplatform content.
 * This function is called from the iOS Swift code to bridge into the shared Compose UI.
 */
fun MainViewController(): UIViewController {
    // Initialize Koin for iOS
    initKoin(iosPlatformModule)

    return ComposeUIViewController {
        BudgetHunterApp()
    }
}
