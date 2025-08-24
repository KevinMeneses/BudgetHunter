package com.meneses.budgethunter.settings.di

import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.settings.SettingsViewModel
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
class SettingsModule {

    @Factory
    fun provideSettingsViewModel(
        preferencesManager: PreferencesManager,
        budgetRepository: BudgetRepository
    ): SettingsViewModel = SettingsViewModel(preferencesManager, budgetRepository)
}
