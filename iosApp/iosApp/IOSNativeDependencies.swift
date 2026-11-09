import Foundation
import ComposeApp

/**
 * The native managers the shared Kotlin code needs. The entry points (`MainViewController`,
 * `SmsShortcutEntryPoint`) take a closure that creates this object, called only when Koin starts,
 * so it is created once per process.
 *
 * The managers are created with it, on the main thread where the entry points run, and not when
 * Koin first asks for them: some do work on creation (IOSNotificationManager asks for the
 * notification permission at launch).
 */
final class IOSNativeDependencies: NativeDependencies {
    let cameraManager: CameraManager = IOSCameraManager()
    let filePickerManager: FilePickerManager = IOSFilePickerManager()
    let shareManager: ShareManager = IOSShareManager()
    let notificationManager: NotificationManager = IOSNotificationManager()
    let keychainStore: KeychainStore = IOSKeychainStore()
    let googleSignInManager: GoogleSignInManager = IOSGoogleSignInManager()

    init() {
        // startKoinIfNeeded runs this under its lock, and the managers expect the main thread
        assert(Thread.isMainThread, "IOSNativeDependencies must be created on the main thread")
    }
}
