import AppIntents

/**
 * Makes "Registrar gasto desde mensaje" show up in the Shortcuts app and in Siri without the
 * user having to build anything first. The message is asked for when it is run by voice.
 */
@available(iOS 16.0, *)
struct BudgetHunterShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: RegisterExpenseFromMessageIntent(),
            phrases: [
                "Registrar gasto desde mensaje en \(.applicationName)",
                "Registrar gasto en \(.applicationName)",
                "Registrar mensaje del banco en \(.applicationName)"
            ]
        )
    }
}
