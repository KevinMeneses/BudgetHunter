package com.meneses.budgethunter.di

import com.meneses.budgethunter.commons.platform.CameraManager
import com.meneses.budgethunter.commons.platform.FilePickerManager
import com.meneses.budgethunter.commons.platform.GoogleSignInManager
import com.meneses.budgethunter.commons.platform.KeychainStore
import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.commons.platform.ShareManager

/**
 * What only the Swift side can provide, implemented in `IOSNativeDependencies.swift`.
 *
 * The Kotlin entry points ([com.meneses.budgethunter.MainViewController],
 * [com.meneses.budgethunter.sms.platform.SmsShortcutEntryPoint]) take a function that creates it,
 * called only when Koin starts, so a missing implementation is a Swift compile error instead of a
 * crash at runtime. The iOS platform module hands each property out as a `single`.
 */
interface NativeDependencies {
    val cameraManager: CameraManager
    val filePickerManager: FilePickerManager
    val shareManager: ShareManager
    val notificationManager: NotificationManager
    val keychainStore: KeychainStore
    val googleSignInManager: GoogleSignInManager
}
