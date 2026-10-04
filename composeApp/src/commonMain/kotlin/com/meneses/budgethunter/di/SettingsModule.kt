package com.meneses.budgethunter.di

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.settings.SettingsViewModel
import com.meneses.budgethunter.settings.application.SyncUserPreferencesUseCase
import com.meneses.budgethunter.settings.data.UserPreferencesRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.koin.core.qualifier.named
import org.koin.dsl.module

val settingsModule = module {

    single<UserPreferencesRepository> {
        UserPreferencesRepository(
            httpClient = get<HttpClient>(named("AuthHttpClient")),
            ioDispatcher = get<CoroutineDispatcher>(named("IO"))
        )
    }

    single<SyncUserPreferencesUseCase> {
        SyncUserPreferencesUseCase(
            preferencesManager = get<PreferencesManager>(),
            userPreferencesRepository = get<UserPreferencesRepository>(),
            budgetRepository = get<BudgetRepository>(),
            authRepository = get<AuthRepository>(),
            logger = get()
        )
    }

    factory<SettingsViewModel> {
        SettingsViewModel(
            preferencesManager = get<PreferencesManager>(),
            budgetRepository = get<BudgetRepository>(),
            permissionsManager = get<PermissionsManager>(),
            authRepository = get<AuthRepository>(),
            syncUserPreferences = get<SyncUserPreferencesUseCase>(),
            applicationScope = get<CoroutineScope>(named("ApplicationScope"))
        )
    }
}
