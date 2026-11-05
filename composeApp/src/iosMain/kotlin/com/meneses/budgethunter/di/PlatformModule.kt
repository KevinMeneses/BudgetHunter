package com.meneses.budgethunter.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.meneses.budgethunter.auth.data.IosSecureTokenStorage
import com.meneses.budgethunter.auth.data.TokenStorage
import com.meneses.budgethunter.budgetEntry.data.ImageProcessor
import com.meneses.budgethunter.budgetEntry.data.network.ReceiptApiService
import com.meneses.budgethunter.budgetEntry.domain.AIImageProcessor
import com.meneses.budgethunter.budgetEntry.domain.IosAIImageProcessor
import com.meneses.budgethunter.commons.data.DatabaseFactory
import com.meneses.budgethunter.commons.data.FileManager
import com.meneses.budgethunter.commons.data.createDatabase
import com.meneses.budgethunter.commons.platform.AppUpdateManager
import com.meneses.budgethunter.commons.platform.CameraManager
import com.meneses.budgethunter.commons.platform.FilePickerManager
import com.meneses.budgethunter.commons.platform.GoogleSignInManager
import com.meneses.budgethunter.commons.platform.IosNetworkMonitor
import com.meneses.budgethunter.commons.platform.NetworkMonitor
import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.commons.platform.ShareManager
import com.meneses.budgethunter.db.Database
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import okio.Path.Companion.toPath
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

val iosPlatformModule = module {
    single<HttpClientEngineFactory<*>>(named(API_HTTP_ENGINE)) { Darwin }

    // Provide the database using iOS-specific factory
    single<Database> {
        DatabaseFactory().createDatabase()
    }

    // Secure token storage backed by the iOS Keychain (native Swift impl via IOSBridge)
    single<TokenStorage> {
        IosSecureTokenStorage(IOSBridge.keychainStore)
    }

    single<DataStore<Preferences>> {
        @OptIn(ExperimentalForeignApi::class)
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = true,
                    error = null
                )
                (
                    requireNotNull(documentDirectory) {
                        "iOS Documents directory is unavailable - check app permissions"
                    }.path + "/budget_hunter_preferences.preferences_pb"
                    ).toPath()
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
    single<NetworkMonitor> {
        IosNetworkMonitor(logger = get()).apply { startMonitoring() }
    }
    single<GoogleSignInManager> { IOSBridge.googleSignInManager }

    single<ImageProcessor> {
        ImageProcessor()
    }

    single<AIImageProcessor> {
        IosAIImageProcessor(
            receiptApiService = get<ReceiptApiService>(),
            imageProcessor = get<ImageProcessor>(),
            ioDispatcher = get<CoroutineDispatcher>(named("IO")),
            logger = get()
        )
    }
}
