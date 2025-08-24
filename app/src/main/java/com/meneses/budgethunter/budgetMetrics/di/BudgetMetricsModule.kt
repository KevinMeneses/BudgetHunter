package com.meneses.budgethunter.budgetMetrics.di

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetMetrics.BudgetMetricsViewModel
import com.meneses.budgethunter.budgetMetrics.application.GetTotalsPerCategoryUseCase
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.koin.core.annotation.Factory

@Module
class BudgetMetricsModule {

    @Single
    fun provideGetTotalsPerCategoryUseCase(
        budgetEntryLocalDataSource: BudgetEntryLocalDataSource,
        @Named("Default") defaultDispatcher: CoroutineDispatcher
    ): GetTotalsPerCategoryUseCase = GetTotalsPerCategoryUseCase(budgetEntryLocalDataSource, defaultDispatcher)

    @Factory
    fun provideBudgetMetricsViewModel(
        getTotalsPerCategoryUseCase: GetTotalsPerCategoryUseCase
    ): BudgetMetricsViewModel = BudgetMetricsViewModel(getTotalsPerCategoryUseCase)
}
