import Foundation
import Security
import ComposeApp

/**
 * Native iOS implementation of the KMP `KeychainStore` interface.
 *
 * Stores values as generic password items in the system Keychain under a fixed service
 * identifier. Implemented in Swift to use the Security framework directly, avoiding the
 * unreliable Kotlin/Native CoreFoundation bridging that was failing at runtime.
 */
class IOSKeychainStore: NSObject, KeychainStore {

    private let service = "com.meneses.budgethunter.tokens"

    func save(key: String, value: String) {
        guard let data = value.data(using: .utf8) else {
            NSLog("Keychain: '\(key)' is not valid UTF-8, not saved")
            return
        }

        // Replace instead of update. An item left behind by a previous install keeps its old
        // attributes, and SecItemUpdate then fails with a status that used to be ignored:
        // the sign in looked successful but no token was ever stored.
        SecItemDelete(baseQuery(for: key) as CFDictionary)

        var addQuery = baseQuery(for: key)
        addQuery[kSecValueData as String] = data
        // AfterFirstUnlock, not WhenUnlocked: background work (SMS entries, sync) has to read
        // the token while the screen is locked.
        addQuery[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly

        let status = SecItemAdd(addQuery as CFDictionary, nil)
        if status != errSecSuccess {
            NSLog("Keychain: could not save '\(key)' (OSStatus \(status))")
        }
    }

    func read(key: String) -> String? {
        var query = baseQuery(for: key)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess else {
            if status != errSecItemNotFound {
                NSLog("Keychain: could not read '\(key)' (OSStatus \(status))")
            }
            return nil
        }

        guard let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    func delete(key: String) {
        let status = SecItemDelete(baseQuery(for: key) as CFDictionary)
        if status != errSecSuccess && status != errSecItemNotFound {
            NSLog("Keychain: could not delete '\(key)' (OSStatus \(status))")
        }
    }

    private func baseQuery(for key: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]
    }
}
