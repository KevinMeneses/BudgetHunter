package com.meneses.budgethunter.di

import android.content.Context
import com.meneses.budgethunter.commons.data.DatabaseFactory
import com.meneses.budgethunter.commons.platform.AppUpdateManager
import com.meneses.budgethunter.commons.platform.CameraManager
import com.meneses.budgethunter.commons.platform.FilePickerManager
import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.commons.platform.ShareManager
import com.meneses.budgethunter.db.Database
import org.koin.dsl.module

val androidPlatformModule = module {
    // Provide the database using Android-specific factory
    single<Database> { 
        DatabaseFactory(get<Context>()).createDatabase()
    }
    
    // Platform-specific managers
    single<CameraManager> { CameraManager(get<Context>()) }
    single<FilePickerManager> { FilePickerManager(get<Context>()) }
    single<PermissionsManager> { PermissionsManager(get<Context>()) }
    single<AppUpdateManager> { AppUpdateManager(get<Context>()) }
    single<NotificationManager> { NotificationManager(get<Context>()) }
    single<ShareManager> { ShareManager(get<Context>()) }
}