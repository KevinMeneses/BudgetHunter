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
    let cameraManager: CameraManager
    let filePickerManager: FilePickerManager
    let shareManager: ShareManager
    let notificationManager: NotificationManager
    let keychainStore: KeychainStore
    let googleSignInManager: GoogleSignInManager

    init() {
        // Checked before any manager exists (default values would be built before this body)
        assert(Thread.isMainThread, "IOSNativeDependencies must be created on the main thread")
        cameraManager = IOSCameraManager()
        filePickerManager = IOSFilePickerManager()
        shareManager = IOSShareManager()
        notificationManager = IOSNotificationManager()
        keychainStore = IOSKeychainStore()
        googleSignInManager = IOSGoogleSignInManager()
    }
}
