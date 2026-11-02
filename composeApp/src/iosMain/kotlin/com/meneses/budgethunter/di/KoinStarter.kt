package com.meneses.budgethunter.di

import org.koin.mp.KoinPlatformTools
import platform.Foundation.NSLock

private val koinLock = NSLock()

/**
 * Starts Koin unless it is already running.
 *
 * The app is not only started by its window: a Shortcuts automation runs the SMS intent in the
 * background, possibly before or without any screen, and starting Koin twice throws.
 */
fun startKoinIfNeeded() {
    koinLock.lock()
    try {
        if (KoinPlatformTools.defaultContext().getOrNull() == null) {
            initKoin(iosPlatformModule)
        }
    } finally {
        koinLock.unlock()
    }
}
