import SwiftUI
import GoogleSignIn
import ComposeApp

@main
struct iOSApp: App {

    init() {
        IOSBridge.Companion.shared.cameraManager = IOSCameraManager()
        IOSBridge.Companion.shared.filePickerManager = IOSFilePickerManager()
        IOSBridge.Companion.shared.shareManager = IOSShareManager()
        IOSBridge.Companion.shared.notificationManager = IOSNotificationManager()
        IOSBridge.Companion.shared.keychainStore = IOSKeychainStore()
        IOSBridge.Companion.shared.googleSignInManager = IOSGoogleSignInManager()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                // Google's sign-in sheet returns control to the app through the reversed client id
                // URL scheme registered in Info.plist; the SDK has to see that URL to finish.
                .onOpenURL { url in
                    _ = GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
