package com.meneses.budgethunter.di

import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.settings.SettingsViewModel
import org.koin.dsl.module

val settingsModule = module {

    factory<SettingsViewModel> {
        SettingsViewModel(
            preferencesManager = get<PreferencesManager>(),
            budgetRepository = get<BudgetRepository>(),
            permissionsManager = get<PermissionsManager>()
        )
    }
}
