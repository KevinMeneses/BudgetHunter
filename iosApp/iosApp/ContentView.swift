import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    
    func makeUIViewController(context: Context) -> UIViewController {
        do {
            let viewController = MainViewController_iosKt.MainViewController()
            return viewController
        } catch {
            print("Error creating MainViewController: \(error)")
            // Return a fallback controller if needed
            return UIViewController()
        }
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // No updates needed for now
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard) // Compose handles keyboard
            .onAppear {
                print("ContentView appeared")
            }
    }
}