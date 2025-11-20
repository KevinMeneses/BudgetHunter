package com.meneses.budgethunter.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.meneses.budgethunter.BuildConfig
import com.meneses.budgethunter.budgetEntry.data.ImageProcessor
import com.meneses.budgethunter.budgetEntry.data.remote.GeminiApiClient
import com.meneses.budgethunter.budgetEntry.domain.AIImageProcessor
import com.meneses.budgethunter.budgetEntry.domain.AndroidAIImageProcessor
import com.meneses.budgethunter.commons.data.DatabaseFactory
import com.meneses.budgethunter.commons.data.FileManager
import com.meneses.budgethunter.commons.data.IFileManager
import com.meneses.budgethunter.commons.data.createDatabase
import com.meneses.budgethunter.commons.platform.AndroidCameraManager
import com.meneses.budgethunter.commons.platform.AndroidFilePickerManager
import com.meneses.budgethunter.commons.platform.AndroidNotificationManager
import com.meneses.budgethunter.commons.platform.AndroidShareManager
import com.meneses.budgethunter.commons.platform.AppUpdateManager
import com.meneses.budgethunter.commons.platform.CameraManager
import com.meneses.budgethunter.commons.platform.FilePickerManager
import com.meneses.budgethunter.commons.platform.IAppUpdateManager
import com.meneses.budgethunter.commons.platform.IPermissionsManager
import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.commons.platform.ShareManager
import com.meneses.budgethunter.db.Database
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
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
    single<FileManager> { FileManager() } bind IFileManager::class
    single<CameraManager> { AndroidCameraManager(get<Context>()) }
    single<FilePickerManager> { AndroidFilePickerManager(get<Context>()) }
    single<PermissionsManager> { PermissionsManager(get<Context>()) } bind IPermissionsManager::class
    single<AppUpdateManager> { AppUpdateManager(get<Context>()) } bind IAppUpdateManager::class
    single<NotificationManager> { AndroidNotificationManager(get<Context>()) }
    single<ShareManager> { AndroidShareManager(get<Context>()) }

    // Keep concrete types available if needed elsewhere - use the same instance as the interface
    single<AndroidCameraManager> { get<CameraManager>() as AndroidCameraManager }
    single<AndroidFilePickerManager> { get<FilePickerManager>() as AndroidFilePickerManager }

    // AI and Image Processing - Android specific
    single<ImageProcessor> {
        ImageProcessor(get<Context>().contentResolver)
    }

    // HTTP Client for AI API calls (using Android engine)
    single<HttpClient> {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(get<Json>())
            }
        }
    }

    // API key from BuildConfig
    single(named("GEMINI_API_KEY")) {
        BuildConfig.GEMINI_API_KEY
    }

    // Shared Gemini API client (from commonMain data layer)
    single<GeminiApiClient> {
        GeminiApiClient(
            httpClient = get<HttpClient>(),
            apiKey = get(named("GEMINI_API_KEY")),
            json = get<Json>()
        )
    }

    single<AIImageProcessor> {
        AndroidAIImageProcessor(
            geminiApiClient = get<GeminiApiClient>(),
            imageProcessor = get<ImageProcessor>(),
            ioDispatcher = get<CoroutineDispatcher>(named("IO"))
        )
    }
}
