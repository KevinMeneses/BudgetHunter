import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    init() {
        IOSBridge.Companion.shared.cameraManager = IOSCameraManager()
        IOSBridge.Companion.shared.filePickerManager = IOSFilePickerManager()
        IOSBridge.Companion.shared.shareManager = IOSShareManager()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}