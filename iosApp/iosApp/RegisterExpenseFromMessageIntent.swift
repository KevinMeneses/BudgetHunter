import AppIntents
import ComposeApp

/**
 * "Registrar gasto desde mensaje": receives the text of a bank SMS from a Shortcuts automation
 * (trigger "Mensaje") and registers it in the default budget, the same way Android does when it
 * reads the SMS itself. It runs in the background, the app does not need to be open.
 *
 * All the reading, the entry creation and the sync live in the shared Kotlin code
 * (SmsShortcutEntryPoint); this only passes the text and shows the answer.
 */
@available(iOS 16.0, *)
struct RegisterExpenseFromMessageIntent: AppIntent {

    static var title: LocalizedStringResource = "Registrar gasto desde mensaje"

    static var description = IntentDescription(
        "Lee el SMS de tu banco y registra el movimiento en tu presupuesto por defecto de BudgetHunter."
    )

    static var openAppWhenRun: Bool = false

    @Parameter(
        title: "Mensaje",
        description: "El contenido del SMS del banco.",
        inputOptions: String.IntentInputOptions(multiline: true),
        requestValueDialog: IntentDialog("¿Qué mensaje del banco quieres registrar?")
    )
    var message: String

    static var parameterSummary: some ParameterSummary {
        Summary("Registrar gasto desde \(\.$message)")
    }

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        IOSBridgeSetup.configureIfNeeded()
        let reply = try await SmsShortcutEntryPoint().register(messageBody: message)
        return .result(dialog: "\(reply.message)")
    }
}
