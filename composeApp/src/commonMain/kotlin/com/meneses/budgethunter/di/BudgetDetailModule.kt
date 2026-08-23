package com.meneses.budgethunter.di

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetDetail.BudgetDetailViewModel
import com.meneses.budgethunter.budgetDetail.data.BudgetDetailRepository
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.sync.RealTimeSyncManager
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.collaborator.data.CollaboratorRepository
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module

val budgetDetailModule = module {

    single<BudgetDetailRepository> {
        BudgetDetailRepository(
            get<BudgetLocalDataSource>(),
            get<BudgetEntryLocalDataSource>(),
            get<BudgetEntryRepository>(),
            get<com.meneses.budgethunter.budgetList.data.BudgetRepository>(),
            get<CoroutineDispatcher>(named("IO")),
            get<DeleteBudgetUseCase>()
        )
    }

    factory<BudgetDetailViewModel> {
        BudgetDetailViewModel(
            budgetDetailRepository = get<BudgetDetailRepository>(),
            realTimeSyncManager = get<RealTimeSyncManager>(),
            authRepository = get<AuthRepository>(),
            collaboratorRepository = get<CollaboratorRepository>()
        )
    }
}
