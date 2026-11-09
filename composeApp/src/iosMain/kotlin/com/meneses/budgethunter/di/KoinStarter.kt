package com.meneses.budgethunter.di

import org.koin.mp.KoinPlatformTools
import platform.Foundation.NSLock

private val koinLock = NSLock()

/**
 * Starts Koin with the Swift [native] implementations, unless it is already running.
 *
 * The app is not only started by its window: a Shortcuts automation runs the SMS intent in the
 * background, possibly before or without any screen, and starting Koin twice throws. When Koin
 * is already running, [native] is ignored.
 */
fun startKoinIfNeeded(native: NativeDependencies) {
    koinLock.lock()
    try {
        if (KoinPlatformTools.defaultContext().getOrNull() == null) {
            initKoin(iosPlatformModule(native))
        }
    } finally {
        koinLock.unlock()
    }
}
