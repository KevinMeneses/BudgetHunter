package com.meneses.budgethunter.di

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetMetrics.BudgetMetricsViewModel
import com.meneses.budgethunter.budgetMetrics.application.GetTotalsPerCategoryUseCase
import com.meneses.budgethunter.budgetMetrics.application.IGetTotalsPerCategoryUseCase
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module

val budgetMetricsModule = module {
    single<GetTotalsPerCategoryUseCase> {
        GetTotalsPerCategoryUseCase(
            budgetEntryLocalDataSource = get<BudgetEntryLocalDataSource>(),
            defaultDispatcher = get<CoroutineDispatcher>(named("Default"))
        )
    }

    single<IGetTotalsPerCategoryUseCase> { get<GetTotalsPerCategoryUseCase>() }

    factory {
        BudgetMetricsViewModel(
            getTotalsPerCategoryUseCase = get<GetTotalsPerCategoryUseCase>()
        )
    }
}
