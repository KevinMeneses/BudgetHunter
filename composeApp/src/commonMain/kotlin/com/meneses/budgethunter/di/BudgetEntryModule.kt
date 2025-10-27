package com.meneses.budgethunter.di

import com.meneses.budgethunter.budgetEntry.BudgetEntryViewModel
import com.meneses.budgethunter.budgetEntry.application.CreateBudgetEntryFromImageUseCase
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.BudgetEntrySyncManager
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.network.BudgetEntryApiService
import com.meneses.budgethunter.budgetEntry.domain.AIImageProcessor
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.db.BudgetEntryQueries
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module

val budgetEntryModule = module {

    single<BudgetEntryLocalDataSource> {
        BudgetEntryLocalDataSource(get<BudgetEntryQueries>(), get<CoroutineDispatcher>(named("IO")))
    }

    single<BudgetEntryApiService> {
        BudgetEntryApiService(
            httpClient = get<HttpClient>(named("AuthHttpClient")),
            ioDispatcher = get<CoroutineDispatcher>(named("IO"))
        )
    }

    single<BudgetEntrySyncManager> {
        BudgetEntrySyncManager(
            localDataSource = get<BudgetEntryLocalDataSource>(),
            budgetEntryApiService = get<BudgetEntryApiService>(),
            authRepository = get(),
            budgetEntryQueries = get<BudgetEntryQueries>(),
            budgetLocalDataSource = get<BudgetLocalDataSource>(),
            ioDispatcher = get<CoroutineDispatcher>(named("IO"))
        )
    }

    single<BudgetEntryRepository> {
        BudgetEntryRepository(
            localDataSource = get<BudgetEntryLocalDataSource>(),
            syncManager = get(),
            authRepository = get(),
            budgetEntryApiService = get(),
            budgetLocalDataSource = get<BudgetLocalDataSource>(),
            ioDispatcher = get<CoroutineDispatcher>(named("IO"))
        )
    }

    // AI image processing use case
    single<CreateBudgetEntryFromImageUseCase> {
        CreateBudgetEntryFromImageUseCase(
            aiImageProcessor = get<AIImageProcessor>(),
            ioDispatcher = get<CoroutineDispatcher>(named("IO"))
        )
    }

    // BudgetEntryViewModel with all required dependencies
    factory {
        BudgetEntryViewModel(
            budgetEntryRepository = get(),
            createBudgetEntryFromImageUseCase = get(),
            validateFilePathUseCase = get(),
            preferencesManager = get(),
            fileManager = get(),
            cameraManager = get(),
            filePickerManager = get(),
            shareManager = get(),
            notificationManager = get()
        )
    }
}
