package com.meneses.budgethunter.di

import com.meneses.budgethunter.budgetDetail.BudgetDetailViewModel
import com.meneses.budgethunter.budgetDetail.data.BudgetDetailRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module

val budgetDetailModule = module {

    single<BudgetDetailRepository> {
        BudgetDetailRepository(
            get<BudgetLocalDataSource>(),
            get<BudgetEntryLocalDataSource>(),
            get<CoroutineDispatcher>(named("IO")),
            get<DeleteBudgetUseCase>()
        )
    }

    single<BudgetDetailViewModel> {
        BudgetDetailViewModel(
            get<BudgetDetailRepository>()
        )
    }
}