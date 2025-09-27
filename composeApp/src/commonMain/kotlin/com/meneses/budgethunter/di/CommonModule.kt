package com.meneses.budgethunter.di

import com.meneses.budgethunter.budgetList.data.adapter.categoryAdapter
import com.meneses.budgethunter.budgetList.data.adapter.typeAdapter
import com.meneses.budgethunter.commons.application.ValidateFilePathUseCase
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.db.BudgetEntryQueries
import com.meneses.budgethunter.db.BudgetQueries
import com.meneses.budgethunter.db.Budget_entry
import com.meneses.budgethunter.db.Database
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

val commonModule = module {

    single<Budget_entry.Adapter> {
        Budget_entry.Adapter(typeAdapter, categoryAdapter)
    }

    single<BudgetEntryQueries> { get<Database>().budgetEntryQueries }

    single<BudgetQueries> { get<Database>().budgetQueries }

    single<CoroutineDispatcher>(named("IO")) { Dispatchers.IO }

    single<CoroutineScope>(named("IOScope")) {
        CoroutineScope(get<CoroutineDispatcher>(named("IO")))
    }

    single<CoroutineDispatcher>(named("Default")) { Dispatchers.Default }

    single<Json> { Json { coerceInputValues = true } }

    single<PreferencesManager> { PreferencesManager(get()) }

    single<ValidateFilePathUseCase> {
        ValidateFilePathUseCase(
            fileManager = get(),
            ioDispatcher = get(named("IO"))
        )
    }
}
