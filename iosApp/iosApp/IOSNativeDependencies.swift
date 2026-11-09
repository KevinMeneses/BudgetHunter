import Foundation
import ComposeApp

/**
 * The native managers the shared Kotlin code needs, handed to its entry points
 * (`MainViewController`, `SmsShortcutEntryPoint`), which start Koin with them.
 *
 * Koin reads each property once (they are `single`s) and only when something asks for it, so
 * returning a new instance here is fine, and so is creating this object more than once: when
 * Koin is already running (e.g. the SMS intent ran first), it is ignored.
 */
final class IOSNativeDependencies: NativeDependencies {
    var cameraManager: CameraManager { IOSCameraManager() }
    var filePickerManager: FilePickerManager { IOSFilePickerManager() }
    var shareManager: ShareManager { IOSShareManager() }
    var notificationManager: NotificationManager { IOSNotificationManager() }
    var keychainStore: KeychainStore { IOSKeychainStore() }
    var googleSignInManager: GoogleSignInManager { IOSGoogleSignInManager() }
}
