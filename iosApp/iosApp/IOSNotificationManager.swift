import UIKit
import UserNotifications
import Foundation
import ComposeApp

/**
 * iOS Notification Manager implementation that conforms to the KMP NotificationManager interface
 */
class IOSNotificationManager: NSObject, NotificationManager {

    override init() {
        super.init()
        requestNotificationPermission()
    }

    func showToast(message: String) {
        // iOS doesn't have native toast messages like Android
        // We'll use a UIAlertController with auto-dismiss as a toast alternative
        DispatchQueue.main.async {
            self.presentToastAlert(message: message)
        }
    }

    func showNotification(title: String, message: String) {
        // Check if notifications are authorized
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            switch settings.authorizationStatus {
            case .authorized, .provisional:
                self.scheduleLocalNotification(title: title, message: message)
            case .denied, .notDetermined:
                // Fall back to toast if notifications not authorized
                DispatchQueue.main.async {
                    self.showToast(message: "\(title): \(message)")
                }
            case .ephemeral:
                // For App Clips, fall back to toast
                DispatchQueue.main.async {
                    self.showToast(message: "\(title): \(message)")
                }
            @unknown default:
                DispatchQueue.main.async {
                    self.showToast(message: "\(title): \(message)")
                }
            }
        }
    }

    private func requestNotificationPermission() {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if let error = error {
                print("Notification permission error: \(error.localizedDescription)")
            }
            print("Notification permission granted: \(granted)")
        }
    }

    private func scheduleLocalNotification(title: String, message: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = message
        content.sound = .default
        content.badge = 1

        // Create a trigger that fires immediately
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 0.1, repeats: false)

        // Create the request
        let request = UNNotificationRequest(
            identifier: "budget-hunter-\(Date().timeIntervalSince1970)",
            content: content,
            trigger: trigger
        )

        // Schedule the notification
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("Failed to schedule notification: \(error.localizedDescription)")
                // Fall back to toast if notification scheduling fails
                DispatchQueue.main.async {
                    self.showToast(message: "\(title): \(message)")
                }
            }
        }
    }

    private func presentToastAlert(message: String) {
        guard let rootViewController = getRootViewController() else {
            print("No root view controller available for toast")
            return
        }

        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)

        // Style the alert to look more like a toast
        alert.view.backgroundColor = UIColor.black.withAlphaComponent(0.8)
        alert.view.layer.cornerRadius = 10

        rootViewController.present(alert, animated: true)

        // Auto-dismiss after 2 seconds (toast-like behavior)
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            alert.dismiss(animated: true)
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

// MARK: - UNUserNotificationCenterDelegate
extension IOSNotificationManager: UNUserNotificationCenterDelegate {

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                               willPresent notification: UNNotification,
                               withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        // Show notification even when app is in foreground
        completionHandler([.sound, .badge])
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                               didReceive response: UNNotificationResponse,
                               withCompletionHandler completionHandler: @escaping () -> Void) {
        // Handle notification tap
        print("Notification tapped: \(response.notification.request.content.title)")
        completionHandler()
    }
}
