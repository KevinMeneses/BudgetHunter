package com.meneses.budgethunter.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.meneses.budgethunter.auth.data.IosSecureTokenStorage
import com.meneses.budgethunter.auth.data.TokenStorage
import com.meneses.budgethunter.billing.UnavailableStoreBilling
import com.meneses.budgethunter.billing.domain.StoreBilling
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

fun iosPlatformModule(native: NativeDependencies) = module {
    single<HttpClientEngineFactory<*>>(named(API_HTTP_ENGINE)) { Darwin }

    // Android launches first; see UnavailableStoreBilling
    single<StoreBilling> { UnavailableStoreBilling() }

    // Provide the database using iOS-specific factory
    single<Database> {
        DatabaseFactory().createDatabase()
    }

    // Secure token storage backed by the iOS Keychain (native Swift impl, see IOSKeychainStore.swift)
    single<TokenStorage> {
        IosSecureTokenStorage(native.keychainStore)
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
    single<CameraManager> { native.cameraManager }
    single<FilePickerManager> { native.filePickerManager }
    single<PermissionsManager> { PermissionsManager() }
    single<AppUpdateManager> { AppUpdateManager() }
    single<NotificationManager> { native.notificationManager }
    single<ShareManager> { native.shareManager }
    single<NetworkMonitor> {
        IosNetworkMonitor(logger = get()).apply { startMonitoring() }
    }
    single<GoogleSignInManager> { native.googleSignInManager }

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
