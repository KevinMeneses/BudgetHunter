import AppIntents

/**
 * Makes "Register expense from message" show up in the Shortcuts app and in Siri without the
 * user having to build anything first. The message is asked for when it is run by voice.
 * The Spanish phrases are in es.lproj/AppShortcuts.strings.
 */
@available(iOS 16.0, *)
struct BudgetHunterShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: RegisterExpenseFromMessageIntent(),
            phrases: [
                "Register expense from message in \(.applicationName)",
                "Register expense in \(.applicationName)",
                "Register bank message in \(.applicationName)"
            ]
        )
    }
}
