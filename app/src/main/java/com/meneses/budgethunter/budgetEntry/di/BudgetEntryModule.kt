package com.meneses.budgethunter.budgetEntry.di

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.meneses.budgethunter.budgetEntry.BudgetEntryViewModel
import com.meneses.budgethunter.budgetEntry.application.CreateAndroidBudgetEntryFromImageUseCase
import com.meneses.budgethunter.budgetEntry.application.CreateBudgetEntryFromImageUseCase
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.db.BudgetEntryQueries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
class BudgetEntryModule {

    @Single
    fun provideBudgetEntryLocalDataSource(
        queries: BudgetEntryQueries,
        @Named("IO") dispatcher: CoroutineDispatcher
    ): BudgetEntryLocalDataSource = BudgetEntryLocalDataSource(queries, dispatcher)

    @Single
    fun provideBudgetEntryRepository(
        localDataSource: BudgetEntryLocalDataSource,
        @Named("IO") ioDispatcher: CoroutineDispatcher
    ): BudgetEntryRepository = BudgetEntryRepository(localDataSource, ioDispatcher)

    @Single
    fun provideGetAIBudgetEntryFromImageUseCase(
        context: Context,
        @Named("IO") ioDispatcher: CoroutineDispatcher,
        generativeModel: GenerativeModel,
        json: Json
    ): CreateBudgetEntryFromImageUseCase =
        CreateAndroidBudgetEntryFromImageUseCase(
            contentResolver = context.contentResolver,
            ioDispatcher = ioDispatcher,
            generativeModel = generativeModel,
            json = json
        )

    @Factory
    fun provideBudgetEntryViewModel(
        budgetEntryRepository: BudgetEntryRepository,
        createBudgetEntryFromImageUseCase: CreateBudgetEntryFromImageUseCase,
        preferencesManager: PreferencesManager
    ): BudgetEntryViewModel = BudgetEntryViewModel(
        budgetEntryRepository,
        createBudgetEntryFromImageUseCase,
        preferencesManager
    )
}
