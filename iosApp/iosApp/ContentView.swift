import ComposeApp
import SwiftUI
import UIKit

struct ComposeView: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> UIViewController {
        let viewController = MainViewController_iosKt.MainViewController()
        return viewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // No updates needed for now
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard)  // Compose handles keyboard
            .onAppear {
                print("ContentView appeared")
            }
    }
}
