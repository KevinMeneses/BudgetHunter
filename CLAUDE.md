# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Testing
- `./gradlew build` - Build the entire project
- `./gradlew :composeApp:assembleDebug` - Build Android APK
- `./gradlew :composeApp:testDebugUnitTest` - Run unit tests (what CI runs; most live in `androidUnitTest`, shared ones in `commonTest`)
- `./gradlew :composeApp:iosSimulatorArm64Test` - Run the iOS tests (Mac only, not in CI; `iosTest` checks the iOS Koin graph)
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
- Automatic reading on Android is decided per bank: Settings lists the banks read ("Agregar banco" adds one), each
  with chips for its channels (`ReadingChannel`: SMS, APP, EMAIL; `BankChannels`), several allowed since a bank may
  send some alerts by one channel and others by another. There are no global switches: the channels are the one
  source of truth, and each channel's permission is asked for when it is picked (SMS: runtime permission; APP and
  EMAIL: notification access) and warned about in Settings while missing. A bank added gets its defaults (its SMS
  and/or app, never email); one with nothing stored (e.g. brought by the account) is read by those defaults too; one
  can be kept with no channel ("bankId:" stored), read from nothing. Channels live on the device only
  (`PreferencesManager.getBankChannels`); the selection (`selectedBankIds`) is on the account, and so is
  `smsReadingEnabled`, which now only follows the channels (SMS in use) and decides nothing. The first
  `getBankChannels` after updating turns the old switches into channels (`BankChannels.migrate`), so nothing starts
  or stops being read. A bank read from two channels shows, under it, that a movement reported by both is added
  twice: there is no deduplication.
- Banks that notify in their app instead of by SMS (BBVA, Nequi, Nu: `SupportedBanks.NOTIFICATION_BANKS`, matched by
  package name) are read by `BankNotificationListenerService` while their APP channel is on. `getBankConfigById`
  keeps returning SMS banks only. A bank is read from its app when its config has `appPackages`: notification-only
  banks are listed apart from `ALL_BANKS`, while an SMS bank whose app also notifies just gets its packages in its
  existing config. The listener needs notification access, granted in a system screen (Settings explains it first
  and reopens it from a warning row), and the app's own notifications on: while they are off Android drops the
  listener (Settings warns too). Some devices (seen on Samsung) then leave it unbound for good, with everything
  granted: requestRebind, toggling the component, re-granting the access or the app killing itself do not help,
  only a force stop from the system settings does. `IsNotificationReaderStoppedUseCase` detects it
  (`BankNotificationListenerService.isConnected`, rechecked after a grace period) while some bank uses APP or EMAIL:
  the budget list shows a banner that leads to Settings, which asks the user for that force stop. Title and text are
  parsed together (`BankNotification`) by the same parser, the entry is described by the text (with the bank name,
  "Nu: ..."), and unreadable notifications are dropped silently (apps also post codes and news).
- Bank emails are read from the email app's notification (Gmail, Outlook, Samsung Email...:
  `BankEmail.EMAIL_APP_PACKAGES`) by the same listener, only for banks with the EMAIL channel. The sender is checked
  before anything else is read (`BankEmail.findSender`): the display name (the title) can be written by anyone, so
  when the email app attaches the sender's address (`EXTRA_PEOPLE_LIST` / `EXTRA_PEOPLE`, "mailto:..."), the address
  decides: an address under a bank's `emailDomains` is that bank's whatever its name, and a bank's name on any other
  address is dropped as a fake. Only banks without known domains, or apps that attach no address, fall back to the
  name (`BankSmsConfig.isSender`). The domains in `SupportedBanks` need checking against real emails: a wrong one
  drops that bank's real emails. Then subject and the start of the body (the expanded text without its first line,
  the subject) are read like a bank app notification (`BankEmail.toNotification`); anything but a movement or a
  missing default budget is dropped silently.
- iOS cannot read SMS: a Shortcuts automation (trigger "Mensaje") passes the text to the App Intent
  `RegisterExpenseFromMessageIntent` (iosApp/, also an App Shortcut), which calls
  `SmsShortcutEntryPoint` (iosMain) -> `RegisterSmsFromShortcutUseCase` and answers with a dialog.
  It runs with the app closed, so `SmsShortcutEntryPoint` starts Koin itself (`startKoinIfNeeded`) with the Swift
  managers (`IOSNativeDependencies`, implementing the Kotlin `NativeDependencies`), like `MainViewController` does.
  Settings shows the setup guide (`SmsShortcutTutorialScreen`) on iOS only.
  The intent's texts are English in the Swift code and Spanish in `iosApp/iosApp/es.lproj` (`Localizable.strings`,
  `AppShortcuts.strings` for the Siri phrases). The guide's screenshots are iOS 26 captures in `drawable` (English) and
  `drawable-es`; the automation filters by the bank name in the message, not by sender.

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
