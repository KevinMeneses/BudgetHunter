package com.meneses.budgethunter.budgetDetail.di

import com.meneses.budgethunter.budgetDetail.BudgetDetailViewModel
import com.meneses.budgethunter.budgetDetail.data.BudgetDetailRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
class BudgetDetailModule {

    @Single
    fun provideBudgetDetailRepository(
        budgetLocalDataSource: BudgetLocalDataSource,
        entriesLocalDataSource: BudgetEntryLocalDataSource,
        @Named("IO") ioDispatcher: CoroutineDispatcher,
        deleteBudgetUseCase: DeleteBudgetUseCase
    ): BudgetDetailRepository = BudgetDetailRepository(
        budgetLocalDataSource,
        entriesLocalDataSource,
        ioDispatcher,
        deleteBudgetUseCase
    )

    @Factory
    fun provideBudgetDetailViewModel(
        budgetDetailRepository: BudgetDetailRepository
    ): BudgetDetailViewModel = BudgetDetailViewModel(budgetDetailRepository)
}
