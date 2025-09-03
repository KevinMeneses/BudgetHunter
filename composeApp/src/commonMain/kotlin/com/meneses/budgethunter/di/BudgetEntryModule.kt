package com.meneses.budgethunter.di

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
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
}