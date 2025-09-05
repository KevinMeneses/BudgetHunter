package com.meneses.budgethunter.di

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetMetrics.BudgetMetricsViewModel
import com.meneses.budgethunter.budgetMetrics.application.GetTotalsPerCategoryUseCase
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.dsl.module

val budgetMetricsModule = module {
    single {
        GetTotalsPerCategoryUseCase(
            budgetEntryLocalDataSource = get<BudgetEntryLocalDataSource>(),
            defaultDispatcher = get<CoroutineDispatcher>()
        )
    }
    
    factory {
        BudgetMetricsViewModel(
            getTotalsPerCategoryUseCase = get<GetTotalsPerCategoryUseCase>()
        )
    }
}
