import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    init() {
        IOSBridge.Companion.shared.cameraManager = IOSCameraManager()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}