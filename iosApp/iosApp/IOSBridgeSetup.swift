import Foundation
import ComposeApp

/**
 * Hands the native managers to the shared Kotlin code.
 *
 * Runs from the app's start and from the SMS App Intent, which can run with the app closed and
 * before any window exists, so it has to be safe to call more than once and from any thread.
 */
enum IOSBridgeSetup {

    private static let lock = NSLock()
    private static var isConfigured = false

    static func configureIfNeeded() {
        lock.lock()
        defer { lock.unlock() }
        guard !isConfigured else { return }

        IOSBridge.Companion.shared.cameraManager = IOSCameraManager()
        IOSBridge.Companion.shared.filePickerManager = IOSFilePickerManager()
        IOSBridge.Companion.shared.shareManager = IOSShareManager()
        IOSBridge.Companion.shared.notificationManager = IOSNotificationManager()
        IOSBridge.Companion.shared.keychainStore = IOSKeychainStore()
        IOSBridge.Companion.shared.googleSignInManager = IOSGoogleSignInManager()
        isConfigured = true
    }
}
