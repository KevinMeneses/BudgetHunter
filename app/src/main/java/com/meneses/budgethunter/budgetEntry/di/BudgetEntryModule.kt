package com.meneses.budgethunter.budgetEntry.di

import com.google.ai.client.generativeai.GenerativeModel
import com.meneses.budgethunter.budgetEntry.BudgetEntryViewModel
import com.meneses.budgethunter.budgetEntry.application.GetAIBudgetEntryFromImageUseCase
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.db.BudgetEntryQueries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.koin.core.annotation.Factory

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
        @Named("IO") ioDispatcher: CoroutineDispatcher,
        json: Json,
        generativeModel: GenerativeModel
    ): GetAIBudgetEntryFromImageUseCase = GetAIBudgetEntryFromImageUseCase(ioDispatcher, json, generativeModel)

    @Factory
    fun provideBudgetEntryViewModel(
        budgetEntryRepository: BudgetEntryRepository,
        getAIBudgetEntryFromImageUseCase: GetAIBudgetEntryFromImageUseCase
    ): BudgetEntryViewModel = BudgetEntryViewModel(budgetEntryRepository, getAIBudgetEntryFromImageUseCase)
}
