package com.meneses.budgethunter.di

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetList.BudgetListViewModel
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.application.IDeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.application.DuplicateBudgetUseCase
import com.meneses.budgethunter.budgetList.application.IDuplicateBudgetUseCase
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.data.IBudgetRepository
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.db.BudgetQueries
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.module.dsl.bind
import org.koin.core.qualifier.named
import org.koin.dsl.module

val budgetListModule = module {

    single<BudgetLocalDataSource> {
        BudgetLocalDataSource(get<BudgetQueries>(), get<CoroutineDispatcher>(named("IO")))
    }

    single<BudgetRepository> {
        BudgetRepository(get<BudgetLocalDataSource>(), get<CoroutineDispatcher>(named("IO")))
    } bind IBudgetRepository::class

    single<DuplicateBudgetUseCase> {
        DuplicateBudgetUseCase(
            get<BudgetRepository>(),
            get<BudgetEntryRepository>(),
            get<CoroutineDispatcher>(named("Default"))
        )
    } bind IDuplicateBudgetUseCase::class

    single<DeleteBudgetUseCase> {
        DeleteBudgetUseCase(
            get<BudgetLocalDataSource>(),
            get<BudgetEntryLocalDataSource>(),
            get<CoroutineDispatcher>(named("IO"))
        )
    } bind IDeleteBudgetUseCase::class

    factory<BudgetListViewModel> {
        BudgetListViewModel(
            get<BudgetRepository>(),
            get<DuplicateBudgetUseCase>(),
            get<DeleteBudgetUseCase>()
        )
    }
}
