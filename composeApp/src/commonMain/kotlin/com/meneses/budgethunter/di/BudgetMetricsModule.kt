package com.meneses.budgethunter.di

import com.meneses.budgethunter.budgetEntry.data.BudgetEntrySyncManager
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.network.BudgetEntryApiService
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetMetrics.BudgetMetricsViewModel
import com.meneses.budgethunter.budgetMetrics.application.CategorizeEntriesUseCase
import com.meneses.budgethunter.budgetMetrics.application.GetCategorizationOfferUseCase
import com.meneses.budgethunter.budgetMetrics.application.GetTotalsPerCategoryUseCase
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.settings.application.SyncUserPreferencesUseCase
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module

val budgetMetricsModule = module {
    single {
        GetTotalsPerCategoryUseCase(
            budgetEntryLocalDataSource = get<BudgetEntryLocalDataSource>(),
            defaultDispatcher = get<CoroutineDispatcher>(named("Default"))
        )
    }

    single {
        GetCategorizationOfferUseCase(
            preferencesManager = get<PreferencesManager>(),
            budgetEntryLocalDataSource = get<BudgetEntryLocalDataSource>()
        )
    }

    single {
        CategorizeEntriesUseCase(
            entrySyncManager = get<BudgetEntrySyncManager>(),
            budgetEntryApiService = get<BudgetEntryApiService>(),
            budgetEntryLocalDataSource = get<BudgetEntryLocalDataSource>(),
            syncUserPreferences = get<SyncUserPreferencesUseCase>()
        )
    }

    factory { (budget: Budget) ->
        BudgetMetricsViewModel(
            budget = budget,
            getTotalsPerCategoryUseCase = get<GetTotalsPerCategoryUseCase>(),
            getCategorizationOfferUseCase = get<GetCategorizationOfferUseCase>(),
            categorizeEntriesUseCase = get<CategorizeEntriesUseCase>()
        )
    }
}
