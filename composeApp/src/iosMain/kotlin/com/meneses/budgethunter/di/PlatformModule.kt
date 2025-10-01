package com.meneses.budgethunter.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.meneses.budgethunter.budgetEntry.data.ImageProcessor
import com.meneses.budgethunter.budgetEntry.domain.AIImageProcessor
import com.meneses.budgethunter.budgetEntry.domain.IosAIImageProcessor
import com.meneses.budgethunter.commons.data.DatabaseFactory
import com.meneses.budgethunter.commons.data.FileManager
import com.meneses.budgethunter.commons.platform.AppUpdateManager
import com.meneses.budgethunter.commons.platform.CameraManager
import com.meneses.budgethunter.commons.platform.FilePickerManager
import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.commons.platform.ShareManager
import com.meneses.budgethunter.db.Database
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

val iosPlatformModule = module {
    // Provide the database using iOS-specific factory
    single<Database> { 
        DatabaseFactory().createDatabase()
    }
    
    // DataStore for preferences using KMP support
    single<DataStore<Preferences>> {
        @OptIn(ExperimentalForeignApi::class)
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = true,
                    error = null,
                )
                (requireNotNull(documentDirectory) {
                    "iOS Documents directory is unavailable - check app permissions"
                }.path + "/budget_hunter_preferences.preferences_pb").toPath()
            }
        )
    }
    
    // Platform-specific managers
    single<FileManager> { FileManager() }
    single<CameraManager> { IOSBridge.cameraManager }
    single<FilePickerManager> { IOSBridge.filePickerManager }
    single<PermissionsManager> { PermissionsManager() }
    single<AppUpdateManager> { AppUpdateManager() }
    single<NotificationManager> { IOSBridge.notificationManager }
    single<ShareManager> { IOSBridge.shareManager }
    
    // AI and Image Processing - iOS implementations
    single<ImageProcessor> {
        ImageProcessor()
    }

    // HTTP Client for AI API calls (using Darwin engine for iOS)
    single<HttpClient> {
        HttpClient(Darwin)
    }

    // Get API key - for iOS, this should be set via build configuration
    // For development: add GEMINI_API_KEY to your environment variables or Info.plist
    // For production: use Xcode build settings to inject from secure storage
    single(named("GEMINI_API_KEY")) {
        // Try to get from Info.plist (can be injected via build settings)
        @OptIn(ExperimentalForeignApi::class)
        val bundle = NSBundle.mainBundle
        val apiKey = bundle.objectForInfoDictionaryKey("GEMINI_API_KEY") as? String

        apiKey ?: "".also {
            println("WARNING: GEMINI_API_KEY not found in Info.plist")
            println("Add it to Info.plist or inject via Xcode build settings")
        }
    }

    // Shared Gemini API client (from commonMain data layer)
    single<com.meneses.budgethunter.budgetEntry.data.remote.GeminiApiClient> {
        com.meneses.budgethunter.budgetEntry.data.remote.GeminiApiClient(
            httpClient = get<HttpClient>(),
            apiKey = get(named("GEMINI_API_KEY")),
            json = get<Json>()
        )
    }

    single<AIImageProcessor> {
        IosAIImageProcessor(
            geminiApiClient = get<com.meneses.budgethunter.budgetEntry.data.remote.GeminiApiClient>(),
            imageProcessor = get<ImageProcessor>(),
            ioDispatcher = get<CoroutineDispatcher>(named("IO"))
        )
    }
}
