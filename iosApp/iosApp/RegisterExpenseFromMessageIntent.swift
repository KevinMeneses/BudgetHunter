import AppIntents
import ComposeApp

/**
 * "Register expense from message" ("Registrar gasto desde mensaje"): receives the text of a bank SMS from a Shortcuts automation
 * (trigger "Mensaje") and registers it in the default budget, the same way Android does when it
 * reads the SMS itself. It runs in the background, the app does not need to be open.
 *
 * All the reading, the entry creation and the sync live in the shared Kotlin code
 * (SmsShortcutEntryPoint); this only passes the text and shows the answer.
 *
 * The texts here are the English ones; the Spanish ones are in es.lproj/Localizable.strings.
 */
@available(iOS 16.0, *)
struct RegisterExpenseFromMessageIntent: AppIntent {

    static var title: LocalizedStringResource = "Register expense from message"

    static var description = IntentDescription(
        "Reads your bank SMS and registers the transaction in your default BudgetHunter budget."
    )

    static var openAppWhenRun: Bool = false

    @Parameter(
        title: "Message",
        description: "The text of the bank SMS.",
        inputOptions: String.IntentInputOptions(multiline: true),
        requestValueDialog: IntentDialog("Which bank message do you want to register?")
    )
    var message: String

    static var parameterSummary: some ParameterSummary {
        Summary("Register expense from \(\.$message)")
    }

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        let reply = try await SmsShortcutEntryPoint(createNative: { IOSNativeDependencies() }).register(messageBody: message)
        return .result(dialog: "\(reply.message)")
    }
}
