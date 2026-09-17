import UIKit
import GoogleSignIn
import ComposeApp

/**
 * Native iOS implementation of the KMP `GoogleSignInManager` interface.
 *
 * Presents Google's account sheet through the GoogleSignIn SDK and hands the resulting ID token
 * back to shared code, which trades it for a BudgetHunter session. Written in Swift for the same
 * reason as `IOSKeychainStore`: Kotlin/Native cannot import the SDK directly, and the interface
 * takes a callback rather than being `suspend` precisely so this class can implement it.
 */
class IOSGoogleSignInManager: NSObject, GoogleSignInManager {

    /**
     * The SDK reads its client id from `GIDClientID` in Info.plist, fed from `GOOGLE_IOS_CLIENT_ID`
     * in Config.xcconfig. Unset, that key expands to an empty string — and calling the SDK without
     * a client id raises an Objective-C exception instead of returning an error, taking the app
     * down. So the button only appears when a real-looking id is present, as on Android.
     */
    var isAvailable: Bool {
        guard let clientId = Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String else {
            return false
        }
        return clientId.hasSuffix(".apps.googleusercontent.com")
    }

    func signIn(onResult: @escaping (GoogleSignInResult) -> Void) {
        guard isAvailable else {
            onResult(GoogleSignInResultFailure(message: "GIDClientID is not configured"))
            return
        }

        DispatchQueue.main.async {
            guard let presenter = Self.topViewController() else {
                NSLog("GoogleSignIn: no view controller available to present the account sheet")
                onResult(GoogleSignInResultFailure(message: "No view controller available"))
                return
            }

            GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { signInResult, error in
                if let error = error as NSError? {
                    if error.domain == kGIDSignInErrorDomain,
                       error.code == GIDSignInError.Code.canceled.rawValue {
                        // Closing the sheet is the most common way out of this flow. Shared code
                        // treats it as a non-event, so it must not arrive dressed as a failure.
                        onResult(GoogleSignInResultCancelled.shared)
                    } else {
                        NSLog("GoogleSignIn: sign in failed (\(error.domain) \(error.code)): \(error.localizedDescription)")
                        onResult(GoogleSignInResultFailure(message: error.localizedDescription))
                    }
                    return
                }

                guard let idToken = signInResult?.user.idToken?.tokenString else {
                    NSLog("GoogleSignIn: sign in completed without an ID token")
                    onResult(GoogleSignInResultFailure(message: "Missing ID token"))
                    return
                }

                onResult(GoogleSignInResultSuccess(idToken: idToken))
            }
        }
    }

    func signOut() {
        // Local only: forgets the cached Google session so the next sign in shows the account
        // sheet again instead of silently reusing the previous identity. BudgetHunter's own tokens
        // are cleared separately by SignOutUseCase.
        GIDSignIn.sharedInstance.signOut()
    }

    /// The topmost presented controller. The window's root is SwiftUI's hosting controller with
    /// Compose inside, and presenting from a controller that is already presenting something
    /// fails without an error.
    private static func topViewController() -> UIViewController? {
        let windows = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
        var top = (windows.first(where: \.isKeyWindow) ?? windows.first)?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}
