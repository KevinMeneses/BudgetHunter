import Foundation
import ComposeApp

/**
 * The native managers the shared Kotlin code needs, handed to its entry points
 * (`MainViewController`, `SmsShortcutEntryPoint`), which start Koin with them.
 *
 * The managers are created right away, on the main thread where the entry points run, and not
 * when Koin first asks for them: some do work on creation (IOSNotificationManager asks for the
 * notification permission at launch). When Koin is already running (e.g. the SMS intent ran
 * first), this object is ignored.
 */
final class IOSNativeDependencies: NativeDependencies {
    let cameraManager: CameraManager = IOSCameraManager()
    let filePickerManager: FilePickerManager = IOSFilePickerManager()
    let shareManager: ShareManager = IOSShareManager()
    let notificationManager: NotificationManager = IOSNotificationManager()
    let keychainStore: KeychainStore = IOSKeychainStore()
    let googleSignInManager: GoogleSignInManager = IOSGoogleSignInManager()
}
