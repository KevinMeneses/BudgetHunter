# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Testing
- `./gradlew build` - Build the entire project
- `./gradlew :composeApp:assembleDebug` - Build Android APK
- `./gradlew :composeApp:testDebugUnitTest` - Run unit tests (what CI runs; most live in `androidUnitTest`, shared ones in `commonTest`)
- `./gradlew connectedAndroidTest` - Run instrumented tests on connected device/emulator
- `./gradlew ktlintCheck` - Check Kotlin code style (CI also runs `./gradlew lint`)
- `./gradlew ktlintFormat` - Auto-fix Kotlin code style issues
- `./gradlew clean` - Clean build artifacts

### Running the App
- **Android**: Use Android Studio or `./gradlew :app:installDebug` to install on device/emulator
- Debug builds read `BACKEND_URL` (and optionally `GOOGLE_SERVER_CLIENT_ID`) from local.properties. There is no AI key in
  the app: every AI call goes through the backend

## Architecture Overview

BudgetHunter is a **Kotlin Multiplatform** budget tracking app (Android and iOS) with a Compose Multiplatform UI. It works offline-first on a local database and syncs with the BudgetHunter backend (collaborative budgets, real-time updates).

### Tech Stack
- **Kotlin 2.2.21** with **Compose Multiplatform** (versions in `gradle/libs.versions.toml`)
- **MVI (Model-View-Intent)** architecture pattern
- **Navigation Compose** for type-safe screen navigation
- **SqlDelight 2.0.2** for local database with SQLite
- **Koin 4.0.0** for dependency injection
- **Ktor 3.3.3** for the backend API and SSE
- **Google Gemini** (called by the backend, never by the app) for receipt reading and categorization

### Current Project Structure
Single Gradle module `composeApp` (shared code plus the Android and iOS source sets), with an Xcode wrapper in `iosApp/`:

```
BudgetHunter/
├── composeApp/src/
│   ├── commonMain/kotlin/com/meneses/budgethunter/
│   │   ├── auth/                # Sign in / sign up, tokens, offline mode
│   │   ├── budgetList/          # Main budget listing feature and budget sync
│   │   ├── budgetDetail/        # Individual budget management
│   │   ├── budgetEntry/         # Budget entry creation/editing, entry API and sync
│   │   ├── budgetMetrics/       # Analytics and automatic categorization offer
│   │   ├── collaborator/        # Sharing a budget with other users
│   │   ├── settings/            # App configuration and account preferences sync
│   │   ├── splash/              # App initialization
│   │   ├── sms/                 # SMS transaction parsing
│   │   ├── commons/             # Shared utilities, network, sync base classes, UI components
│   │   ├── navigation/          # Routes and screen wiring
│   │   ├── theme/               # App theming and design system
│   │   └── di/                  # Koin modules
│   ├── androidMain/             # Android implementations (SMS receiver, files, ...)
│   ├── iosMain/                 # iOS implementations
│   ├── commonTest/              # Shared tests
│   └── androidUnitTest/         # JVM unit tests (MockK, Ktor MockEngine)
└── iosApp/                      # Xcode wrapper
```

### Key Architectural Patterns

**Feature Module Structure:**
- `domain/` - Business entities and domain logic
- `data/` - Repository pattern with local data sources
- `application/` - Use cases, events, and state management
- `ui/` - Compose screens and UI components

**Database:**
- SqlDelight schema in `composeApp/src/commonMain/sqldelight/`
- Database operations through Repository pattern
- Driver created per platform and provided through Koin

**State Management:**
- Each feature has a ViewModel with MVI pattern
- State classes define UI state
- Event classes handle user interactions
- StateFlow for reactive state updates

### Navigation
Type-safe navigation using Navigation Compose with serializable route objects. Main navigation flow:
- SplashScreen → BudgetListScreen → BudgetDetailScreen → BudgetEntryScreen
- Settings and UserGuide accessible from various screens

### AI Integration
- Receipts (images and PDFs) are read by the backend: `ReceiptApiService` posts the file Base64-encoded to
  `POST /api/receipts/extract`; the server holds the Gemini key and the prompt, and answers what it read.
  `CreateBudgetEntryFromImageUseCase` merges that into the form; the platform `AIImageProcessor`s only turn the
  file into Base64 (PDFs as-is up to 12 MB, otherwise the first page as JPEG).
- Reading a receipt needs a signed-in account (offline mode gets `SIGN_IN_REQUIRED`) and counts against the
  plan's monthly scans; past them the server answers 402 (`QUOTA_EXCEEDED`). See "Monetization" below.
- The HTTP log never prints that request (it carries the whole file).
- The AI processing toggle in settings is kept on the account too (`SyncUserPreferencesUseCase`), because it also gates the server-side categorization below

### SMS Processing
- SmsService and SmsBroadcastReceiver handle incoming transaction SMS
- Bank-specific configuration in `sms/domain/SupportedBanks.kt` for different SMS formats
- Automatic budget entry creation from SMS transactions (`RegisterSmsEntryUseCase`, shared by both platforms),
  always in the default budget of Settings. The description is the message's first sentence, opened with the bank
  name ("Bancolombia: Transferiste ...") unless it already names the bank (`BankSmsConfig.describe`); notifications too
- Banks that notify in their app instead of by SMS (Nequi, Nu: `SupportedBanks.NOTIFICATION_BANKS`, matched by
  package name) are read on Android by `BankNotificationListenerService`, only while selected: they share the one
  bank selection of Settings (`SupportedBanks.SELECTABLE_BANKS`, `selectedBankIds`) with the SMS banks, and
  `getBankConfigById` keeps returning SMS banks only. A bank is read from its app when its config has
  `appPackages`: notification-only banks are listed apart from `ALL_BANKS`, while an SMS bank whose app also
  notifies just gets its packages in its existing config. It needs notification access, granted in
  a system screen (Settings explains it first and reopens it from a warning row), and the app's own notifications
  on: while they are off Android drops the listener (Settings warns too). Some devices (seen on Samsung) then leave it
  unbound for good, with everything granted: requestRebind, toggling the component, re-granting the access or the app
  killing itself do not help, only a force stop from the system settings does. `IsNotificationReaderStoppedUseCase`
  detects it (`BankNotificationListenerService.isConnected`, rechecked after a grace period): the budget list shows a
  banner that leads to Settings, which asks the user for that force stop. Title and text are parsed together
  (`BankNotification`) by the same parser, the entry is described by the text (with the bank name, "Nu: ..."), and
  unreadable notifications are dropped silently (apps also post codes and news). The switch is kept on the device only, not on the account.
- iOS cannot read SMS: a Shortcuts automation (trigger "Mensaje") passes the text to the App Intent
  `RegisterExpenseFromMessageIntent` (iosApp/, also an App Shortcut), which calls
  `SmsShortcutEntryPoint` (iosMain) -> `RegisterSmsFromShortcutUseCase` and answers with a dialog.
  It runs with the app closed, so Koin is started with `startKoinIfNeeded()` and the Swift managers with
  `IOSBridgeSetup.configureIfNeeded()`. Settings shows the setup guide (`SmsShortcutTutorialScreen`) on iOS only.
  The intent's texts are English in the Swift code and Spanish in `iosApp/iosApp/es.lproj` (`Localizable.strings`,
  `AppShortcuts.strings` for the Siri phrases). The guide's screenshots are iOS 26 captures in `drawable` (English) and
  `drawable-es`; the automation filters by the bank name in the message, not by sender.
- The same intent takes bank alert emails (Shortcuts trigger "Correo", iOS 17+, needs the account in Apple Mail).
  A text longer than an SMS (> 320 chars) is read as an email (`BankEmail`): only the first sentence that names a
  movement and carries a number is parsed, and the bank is told by its email domain first
  (`BankSmsConfig.emailDomains`), then by the name it mentions first outside that sentence, then inside it, because
  the movement may name the other party's bank ("desde el banco BBVA" in a Nequi email). An email with no such
  sentence is read whole as an SMS; one whose bank cannot be told is answered as not recognized rather than filed
  under a guessed bank.
  Email-only banks (Lulo bank) are in `SupportedBanks.SHORTCUT_BANKS`, not in the Android selector.

### Automatic categorization
Entries carry a category; `UNCATEGORIZED` ("Sin categoría") is its own value, the default for a new entry, and is
not `OTHER` ("looked at it, nothing fits"). It is never offered in the AI's category list
(`BudgetEntry.getAssignableCategories()`) but is in the selector (`BudgetEntry.getCategories()`). There is no
"set by AI / set by user" marker: an entry is waiting if and only if it holds `UNCATEGORIZED`.

- The metrics screen offers a dialog (`CategorizeEntriesDialog`) when AI processing is on, offline mode is off,
  the budget is on the server and at least one of its entries is `UNCATEGORIZED`
  (`GetCategorizationOfferUseCase`; "Not now" is remembered in memory until more entries are waiting).
- On confirm, `CategorizeEntriesUseCase` pushes the budget's unsynced entries, calls
  `POST /api/budgets/{id}/entries/categorize` (the server sends only the descriptions to the AI), pulls
  the entries back and the totals are recomputed. A 403 pushes the preferences once and retries; 409 means a run
  is already going.
- SMS entries are created `UNCATEGORIZED`; a category chosen by the receipt AI is a normal category and is never
  overwritten. The server's rules live in the backend repository.
- Design notes and decisions: `docs/plans/auto-categorization.md`.

### Monetization
Freemium, with RevenueCat for the store purchases (in progress; plan and decisions in `docs/plans/monetization.md`).
- Free: everything manual, SMS reading, metrics, and a monthly allowance of AI receipt scans (server setting,
  10 by default). Premium: a much larger allowance (fair use) and, later, the other paid features.
- The server is the source of truth: `users.plan` and the monthly count live there (`GET /api/receipts/usage`).
  The app never decides on its own whether something is allowed.
