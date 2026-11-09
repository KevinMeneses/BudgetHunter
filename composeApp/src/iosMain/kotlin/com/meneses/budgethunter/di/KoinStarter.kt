package com.meneses.budgethunter.di

import org.koin.mp.KoinPlatformTools
import platform.Foundation.NSLock

private val koinLock = NSLock()

/**
 * Starts Koin with the Swift implementations made by [createNative], unless it is already running.
 *
 * The app is not only started by its window: a Shortcuts automation runs the SMS intent in the
 * background, possibly before or without any screen, and starting Koin twice throws. When Koin
 * is already running, [createNative] is not called: some managers do work when created (the
 * notification one asks for the permission), and each entry point would otherwise build them
 * again for nothing.
 *
 * [createNative] runs under the lock, so it must not wait on another thread that could call
 * this too. Both entry points call this on the main thread, where the Swift managers are made.
 */
fun startKoinIfNeeded(createNative: () -> NativeDependencies) {
    koinLock.lock()
    try {
        if (KoinPlatformTools.defaultContext().getOrNull() == null) {
            initKoin(iosPlatformModule(createNative()))
        }
    } finally {
        koinLock.unlock()
    }
}
