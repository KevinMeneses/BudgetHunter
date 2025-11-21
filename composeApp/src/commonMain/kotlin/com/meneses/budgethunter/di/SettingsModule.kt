package com.meneses.budgethunter.di

import com.meneses.budgethunter.budgetList.data.IBudgetRepository
import com.meneses.budgethunter.commons.data.IPreferencesManager
import com.meneses.budgethunter.commons.platform.IPermissionsManager
import com.meneses.budgethunter.settings.SettingsViewModel
import org.koin.dsl.module

val settingsModule = module {

    factory<SettingsViewModel> {
        SettingsViewModel(
            preferencesManager = get<IPreferencesManager>(),
            budgetRepository = get<IBudgetRepository>(),
            permissionsManager = get<IPermissionsManager>()
        )
    }
}
