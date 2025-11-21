package com.meneses.budgethunter.di

import com.meneses.budgethunter.budgetEntry.BudgetEntryViewModel
import com.meneses.budgethunter.budgetEntry.application.CreateBudgetEntryFromImageUseCase
import com.meneses.budgethunter.budgetEntry.application.ICreateBudgetEntryFromImageUseCase
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.IBudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.AIImageProcessor
import com.meneses.budgethunter.db.BudgetEntryQueries
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module

val budgetEntryModule = module {

    single<BudgetEntryLocalDataSource> {
        BudgetEntryLocalDataSource(get<BudgetEntryQueries>(), get<CoroutineDispatcher>(named("IO")))
    }

    single<BudgetEntryRepository> {
        BudgetEntryRepository(get<BudgetEntryLocalDataSource>(), get<CoroutineDispatcher>(named("IO")))
    }

    single<IBudgetEntryRepository> { get<BudgetEntryRepository>() }

    // AI image processing use case
    single<CreateBudgetEntryFromImageUseCase> {
        CreateBudgetEntryFromImageUseCase(
            aiImageProcessor = get<AIImageProcessor>(),
            ioDispatcher = get<CoroutineDispatcher>(named("IO"))
        )
    }

    single<ICreateBudgetEntryFromImageUseCase> { get<CreateBudgetEntryFromImageUseCase>() }

    // BudgetEntryViewModel with all required dependencies
    factory {
        BudgetEntryViewModel(
            budgetEntryRepository = get(),
            createBudgetEntryFromImageUseCase = get(),
            validateFilePathUseCase = get(),
            preferencesManager = get(),
            fileManager = get(),
            cameraManager = get(),
            filePickerManager = get(),
            shareManager = get(),
            notificationManager = get()
        )
    }
}
