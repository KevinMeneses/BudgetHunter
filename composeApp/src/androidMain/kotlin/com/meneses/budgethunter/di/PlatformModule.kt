package com.meneses.budgethunter.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.ai.client.generativeai.GenerativeModel
import com.meneses.budgethunter.BuildConfig
import com.meneses.budgethunter.budgetEntry.data.ImageProcessor
import com.meneses.budgethunter.budgetEntry.domain.AIImageProcessor
import com.meneses.budgethunter.budgetEntry.domain.AndroidAIImageProcessor
import com.meneses.budgethunter.commons.data.DatabaseFactory
import com.meneses.budgethunter.commons.data.FileManager
import com.meneses.budgethunter.commons.platform.AppUpdateManager
import com.meneses.budgethunter.commons.platform.CameraManager
import com.meneses.budgethunter.commons.platform.FilePickerManager
import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.commons.platform.ShareManager
import com.meneses.budgethunter.db.Database
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "budget_hunter_preferences")

val androidPlatformModule = module {
    // Provide the database using Android-specific factory
    single<Database> { 
        DatabaseFactory(get<Context>()).createDatabase()
    }
    
    // DataStore for preferences
    single<DataStore<Preferences>> { 
        get<Context>().dataStore 
    }
    
    // Platform-specific managers
    single<FileManager> { FileManager() }
    single<CameraManager> { CameraManager(get<Context>()) }
    single<FilePickerManager> { FilePickerManager(get<Context>()) }
    single<PermissionsManager> { PermissionsManager(get<Context>()) }
    single<AppUpdateManager> { AppUpdateManager(get<Context>()) }
    single<NotificationManager> { NotificationManager(get<Context>()) }
    single<ShareManager> { ShareManager(get<Context>()) }
    
    // AI and Image Processing - Android specific
    single<GenerativeModel> {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }
    
    single<ImageProcessor> {
        ImageProcessor(get<Context>().contentResolver)
    }
    
    single<AIImageProcessor> {
        AndroidAIImageProcessor(
            generativeModel = get<GenerativeModel>(),
            imageProcessor = get<ImageProcessor>(),
            json = get<Json>(),
            ioDispatcher = get<CoroutineDispatcher>(named("IO"))
        )
    }
}
