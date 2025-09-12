package com.meneses.budgethunter.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.meneses.budgethunter.budgetEntry.data.ImageProcessor
import com.meneses.budgethunter.budgetEntry.domain.AIImageProcessor
import com.meneses.budgethunter.budgetEntry.domain.IosAIImageProcessor
import com.meneses.budgethunter.commons.data.DatabaseFactory
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
    
    // DataStore for preferences (iOS placeholder)
    single<DataStore<Preferences>> { 
        // TODO: Implement proper iOS DataStore when iOS functionality is added
        object : DataStore<Preferences> {
            override val data = kotlinx.coroutines.flow.flowOf(androidx.datastore.preferences.core.emptyPreferences())
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = androidx.datastore.preferences.core.emptyPreferences()
        }
    }
    
    // Platform-specific managers
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
