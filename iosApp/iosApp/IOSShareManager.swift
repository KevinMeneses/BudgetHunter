import UIKit
import Foundation
import ComposeApp

/**
 * iOS Share Manager implementation that conforms to the KMP ShareManager interface
 */
class IOSShareManager: NSObject, ShareManager {

    func shareFile(filePath: String, mimeTypes: KotlinArray<NSString>) {
        // Handle different path formats
        let cleanPath = filePath.hasPrefix("file://") ?
            String(filePath.dropFirst(7)) : filePath

        // Check if file exists
        guard FileManager.default.fileExists(atPath: cleanPath) else {
            print("Share failed: File does not exist at path: \(cleanPath)")
            return
        }

        let fileURL = URL(fileURLWithPath: cleanPath)
        presentShareSheet(for: fileURL)
    }

    private func presentShareSheet(for fileURL: URL) {
        guard let rootViewController = getRootViewController() else {
            print("No root view controller available for sharing")
            return
        }

        let activityViewController = UIActivityViewController(
            activityItems: [fileURL],
            applicationActivities: nil
        )

        // Configure for iPad
        if let popoverController = activityViewController.popoverPresentationController {
            popoverController.sourceView = rootViewController.view
            popoverController.sourceRect = CGRect(
                x: rootViewController.view.bounds.midX,
                y: rootViewController.view.bounds.midY,
                width: 0,
                height: 0
            )
            popoverController.permittedArrowDirections = []
        }

        // Present the share sheet
        rootViewController.present(activityViewController, animated: true) {
            print("Share sheet presented for file: \(fileURL.lastPathComponent)")
        }
    }

    private func getRootViewController() -> UIViewController? {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first else {
            return nil
        }
        return window.rootViewController
    }
}