package com.meneses.budgethunter.settings.di

import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.LifecycleManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.settings.SettingsViewModel
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
class SettingsModule {

    @Factory
    fun provideSettingsViewModel(
        preferencesManager: PreferencesManager,
        budgetRepository: BudgetRepository,
        permissionsManager: PermissionsManager,
        lifecycleManager: LifecycleManager
    ): SettingsViewModel = SettingsViewModel(
        preferencesManager = preferencesManager,
        budgetRepository = budgetRepository,
        permissionsManager = permissionsManager,
        lifecycleManager = lifecycleManager
    )
}
