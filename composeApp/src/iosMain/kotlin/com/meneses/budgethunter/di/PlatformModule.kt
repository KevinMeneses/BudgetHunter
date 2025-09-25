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
    
    // AI and Image Processing - iOS placeholder implementations
    single<ImageProcessor> {
        ImageProcessor()
    }
    
    single<AIImageProcessor> {
        IosAIImageProcessor(
            imageProcessor = get<ImageProcessor>(),
            ioDispatcher = get<CoroutineDispatcher>(named("IO"))
        )
    }
}
