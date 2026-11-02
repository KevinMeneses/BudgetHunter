import SwiftUI
import GoogleSignIn
import ComposeApp

@main
struct iOSApp: App {

    init() {
        IOSBridgeSetup.configureIfNeeded()
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
