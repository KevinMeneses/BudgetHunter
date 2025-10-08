package com.meneses.budgethunter.di

import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Initialize Koin dependency injection for the multiplatform app.
 * This function should be called from the platform-specific application classes.
 */
fun initKoin(platformModule: org.koin.core.module.Module = module { }) = startKoin {
    modules(
        // Common modules
        commonModule,
        authModule,
        budgetListModule,
        budgetDetailModule,
        budgetEntryModule,
        budgetMetricsModule,
        splashModule,
        smsModule,
        settingsModule,

        // Platform-specific module
        platformModule
    )
}
