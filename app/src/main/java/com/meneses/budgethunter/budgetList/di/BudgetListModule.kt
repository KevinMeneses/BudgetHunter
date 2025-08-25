package com.meneses.budgethunter.budgetList.di

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetList.BudgetListViewModel
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.application.DuplicateBudgetUseCase
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
class BudgetListModule {

    @Single
    fun provideBudgetLocalDataSource(
        queries: com.meneses.budgethunter.db.BudgetQueries,
        @Named("IO") dispatcher: CoroutineDispatcher
    ): BudgetLocalDataSource = BudgetLocalDataSource(queries, dispatcher)

    @Single
    fun provideBudgetRepository(
        localDataSource: BudgetLocalDataSource,
        @Named("IO") ioDispatcher: CoroutineDispatcher
    ): BudgetRepository = BudgetRepository(localDataSource, ioDispatcher)

    @Single
    fun provideDuplicateBudgetUseCase(
        budgetRepository: BudgetRepository,
        budgetEntryRepository: BudgetEntryRepository,
        @Named("Default") defaultDispatcher: CoroutineDispatcher
    ): DuplicateBudgetUseCase = DuplicateBudgetUseCase(budgetRepository, budgetEntryRepository, defaultDispatcher)

    @Single
    fun provideDeleteBudgetUseCase(
        budgetLocalDataSource: BudgetLocalDataSource,
        entriesLocalDataSource: BudgetEntryLocalDataSource,
        @Named("IO") ioDispatcher: CoroutineDispatcher
    ): DeleteBudgetUseCase = DeleteBudgetUseCase(budgetLocalDataSource, entriesLocalDataSource, ioDispatcher)

    @Factory
    fun provideBudgetListViewModel(
        budgetRepository: BudgetRepository,
        duplicateBudgetUseCase: DuplicateBudgetUseCase,
        deleteBudgetUseCase: DeleteBudgetUseCase
    ): BudgetListViewModel = BudgetListViewModel(budgetRepository, duplicateBudgetUseCase, deleteBudgetUseCase)
}
