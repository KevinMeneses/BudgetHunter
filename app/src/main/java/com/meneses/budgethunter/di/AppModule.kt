package com.meneses.budgethunter.di

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.google.ai.client.generativeai.GenerativeModel
import com.meneses.budgethunter.BuildConfig
import com.meneses.budgethunter.budgetList.data.adapter.categoryAdapter
import com.meneses.budgethunter.budgetList.data.adapter.typeAdapter
import com.meneses.budgethunter.commons.data.KtorRealtimeMessagingClient
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.db.BudgetEntryQueries
import com.meneses.budgethunter.db.BudgetQueries
import com.meneses.budgethunter.db.Budget_entry
import com.meneses.budgethunter.db.Database
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
class AppModule {

    @Single
    fun provideBudgetEntryAdapter(): Budget_entry.Adapter {
        return Budget_entry.Adapter(typeAdapter, categoryAdapter)
    }

    @Single
    fun provideDatabase(
        driver: AndroidSqliteDriver,
        budgetEntryAdapter: Budget_entry.Adapter
    ): Database {
        return Database(driver, budgetEntryAdapter)
    }

    @Single
    fun provideSqliteDriver(context: Context): AndroidSqliteDriver {
        return AndroidSqliteDriver(
            schema = Database.Schema,
            context = context,
            name = "budgethunter.db"
        )
    }

    @Single
    fun providePreferencesManager(context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Single
    fun provideBudgetEntryQueries(database: Database): BudgetEntryQueries {
        return database.budgetEntryQueries
    }

    @Single
    fun provideBudgetQueries(database: Database): BudgetQueries {
        return database.budgetQueries
    }

    @Single
    @Named("IO")
    fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Single
    @Named("Default")
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Single
    fun provideJson(): Json = Json { coerceInputValues = true }

    @Single
    fun provideGenerativeModel(): GenerativeModel =
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )

    @Single
    fun provideMessagingClient(): () -> KtorRealtimeMessagingClient = {
        KtorRealtimeMessagingClient.getInstance()
    }
}
