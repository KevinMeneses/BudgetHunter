package com.meneses.budgethunter.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.meneses.budgethunter.auth.SignInViewModel
import com.meneses.budgethunter.auth.SignUpViewModel
import com.meneses.budgethunter.auth.application.SignOutUseCase
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.auth.data.TokenStorage
import com.meneses.budgethunter.commons.data.network.createHttpClient
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

val authModule = module {

    single<TokenStorage> {
        TokenStorage(get<DataStore<Preferences>>())
    }

    single<HttpClient>(named("AuthHttpClient")) {
        createHttpClient(
            baseUrl = "http://10.0.2.2:8080", // Android emulator host machine
            tokenStorage = get<TokenStorage>(),
            json = get<Json>()
        )
    }

    single<AuthRepository> {
        AuthRepository(
            httpClient = get<HttpClient>(named("AuthHttpClient")),
            tokenStorage = get<TokenStorage>(),
            ioDispatcher = get<CoroutineDispatcher>(named("IO"))
        )
    }

    single<SignOutUseCase> {
        SignOutUseCase(
            authRepository = get<AuthRepository>(),
            budgetRepository = get(),
            budgetEntryRepository = get(),
            preferencesManager = get(),
            ioDispatcher = get<CoroutineDispatcher>(named("IO"))
        )
    }

    factory<SignInViewModel> {
        SignInViewModel(
            authRepository = get<AuthRepository>(),
            preferencesManager = get(),
            budgetRepository = get()
        )
    }

    factory<SignUpViewModel> {
        SignUpViewModel(get<AuthRepository>())
    }
}
