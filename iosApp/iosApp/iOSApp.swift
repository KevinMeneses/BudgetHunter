import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    init() {
        IOSBridge.Companion.shared.cameraManager = IOSCameraManager()
        IOSBridge.Companion.shared.filePickerManager = IOSFilePickerManager()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}