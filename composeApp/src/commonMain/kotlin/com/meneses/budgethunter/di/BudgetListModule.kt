package com.meneses.budgethunter.di

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetList.BudgetListViewModel
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.application.DuplicateBudgetUseCase
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.data.network.BudgetApiService
import com.meneses.budgethunter.budgetList.data.sync.BudgetSyncManager
import com.meneses.budgethunter.db.BudgetQueries
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.koin.core.qualifier.named
import org.koin.dsl.module

val budgetListModule = module {

    single<BudgetLocalDataSource> {
        BudgetLocalDataSource(get<BudgetQueries>(), get<CoroutineDispatcher>(named("IO")))
    }

    single<BudgetApiService> {
        BudgetApiService(
            httpClient = get<HttpClient>(named("AuthHttpClient")),
            ioDispatcher = get<CoroutineDispatcher>(named("IO"))
        )
    }

    single<BudgetSyncManager> {
        BudgetSyncManager(
            localDataSource = get<BudgetLocalDataSource>(),
            budgetApiService = get<BudgetApiService>(),
            authRepository = get(),
            budgetQueries = get<BudgetQueries>(),
            ioDispatcher = get<CoroutineDispatcher>(named("IO"))
        )
    }

    single<BudgetRepository> {
        BudgetRepository(
            localDataSource = get<BudgetLocalDataSource>(),
            budgetSyncManager = get<BudgetSyncManager>(),
            authRepository = get(),
            ioDispatcher = get<CoroutineDispatcher>(named("IO")),
            scope = get<CoroutineScope>(named("ApplicationScope"))
        )
    }

    single<DuplicateBudgetUseCase> {
        DuplicateBudgetUseCase(
            get<BudgetRepository>(), 
            get<BudgetEntryRepository>(), 
            get<CoroutineDispatcher>(named("Default"))
        )
    }

    single<DeleteBudgetUseCase> {
        DeleteBudgetUseCase(
            get<BudgetLocalDataSource>(), 
            get<BudgetEntryLocalDataSource>(), 
            get<CoroutineDispatcher>(named("IO"))
        )
    }

    factory<BudgetListViewModel> {
        BudgetListViewModel(
            budgetRepository = get<BudgetRepository>(),
            duplicateBudgetUseCase = get<DuplicateBudgetUseCase>(),
            deleteBudgetUseCase = get<DeleteBudgetUseCase>(),
            authRepository = get(),
            preferencesManager = get()
        )
    }
}