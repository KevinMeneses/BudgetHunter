package com.meneses.budgethunter.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
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
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module

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
                    create = false,
                    error = null,
                )
                (requireNotNull(documentDirectory).path + "/budget_hunter_preferences.preferences_pb").toPath()
            }
        )
    }
    
    // Platform-specific managers
    single<FileManager> { FileManager() }
    single<CameraManager> { CameraManager() }
    single<FilePickerManager> { FilePickerManager() }
    single<PermissionsManager> { PermissionsManager() }
    single<AppUpdateManager> { AppUpdateManager() }
    single<NotificationManager> { NotificationManager() }
    single<ShareManager> { ShareManager() }
    
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
