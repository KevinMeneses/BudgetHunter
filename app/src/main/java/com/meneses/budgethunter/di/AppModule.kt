package com.meneses.budgethunter.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.ai.client.generativeai.GenerativeModel
import com.meneses.budgethunter.BuildConfig
import com.meneses.budgethunter.budgetList.data.adapter.categoryAdapter
import com.meneses.budgethunter.budgetList.data.adapter.typeAdapter
import com.meneses.budgethunter.commons.data.AndroidDatabaseFactory
import com.meneses.budgethunter.commons.data.AndroidFileManager
import com.meneses.budgethunter.commons.data.DatabaseFactory
import com.meneses.budgethunter.commons.data.FileManager
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.AndroidCameraManager
import com.meneses.budgethunter.commons.platform.AndroidFilePickerManager
import com.meneses.budgethunter.commons.platform.AndroidNotificationManager
import com.meneses.budgethunter.commons.platform.AndroidPermissionsManager
import com.meneses.budgethunter.commons.platform.AndroidShareManager
import com.meneses.budgethunter.commons.platform.CameraManager
import com.meneses.budgethunter.commons.platform.FilePickerManager
import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.commons.platform.ShareManager
import com.meneses.budgethunter.db.BudgetEntryQueries
import com.meneses.budgethunter.db.BudgetQueries
import com.meneses.budgethunter.db.Budget_entry
import com.meneses.budgethunter.db.Database
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "budget_hunter_preferences")

@Module
class AppModule {

    @Single
    fun provideBudgetEntryAdapter(): Budget_entry.Adapter {
        return Budget_entry.Adapter(typeAdapter, categoryAdapter)
    }

    @Single
    fun provideDatabase(
        databaseFactory: DatabaseFactory
    ): Database {
        return databaseFactory.createDatabase()
    }

    @Single
    fun provideDatabaseDriverFactory(context: Context): DatabaseFactory {
        return AndroidDatabaseFactory(context)
    }

    @Single
    fun provideDataStore(context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Single
    fun providePreferencesManager(preferences: DataStore<Preferences>): PreferencesManager {
        return PreferencesManager(preferences)
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
    @Named("IOScope")
    fun provideIOScope(@Named("IO") ioDispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(ioDispatcher)

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
    fun provideFileManager(): FileManager = AndroidFileManager()

    @Single
    fun provideNotificationManager(context: Context): NotificationManager = 
        AndroidNotificationManager(context)

    @Single
    fun provideShareManager(context: Context): ShareManager = 
        AndroidShareManager(context)

    @Single
    fun provideCameraManager(context: Context): CameraManager = 
        AndroidCameraManager(context)

    @Single
    fun provideFilePickerManager(context: Context): FilePickerManager = 
        AndroidFilePickerManager(context)

    @Single
    fun providePermissionsManager(context: Context): PermissionsManager = 
        AndroidPermissionsManager(context)
}
