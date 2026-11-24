# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Testing
- `./gradlew build` - Build the entire project
- `./gradlew :composeApp:assembleDebug` - Build Android APK
- `./gradlew :composeApp:testDebugUnitTest` - Run unit tests (what CI runs; most live in `androidUnitTest`, shared ones in `commonTest`)
- `./gradlew :composeApp:iosSimulatorArm64Test` - Run the iOS tests (Mac only, not in CI; `iosTest` checks the iOS Koin graph).
  It also compiles `commonTest` for Kotlin/Native, so tests there cannot use MockK (JVM only: put them in
  `androidUnitTest`) and their backtick names cannot contain `,` `(` `)` `.` `:` `/` `[` `]` `<` `>`
- `./gradlew connectedAndroidTest` - Run instrumented tests on connected device/emulator
- `./gradlew ktlintCheck` - Check Kotlin code style (CI also runs `./gradlew lint`)
- `./gradlew ktlintFormat` - Auto-fix Kotlin code style issues
- `./gradlew clean` - Clean build artifacts

### Running the App
- **Android**: Use Android Studio or `./gradlew :app:installDebug` to install on device/emulator
- Debug builds read `BACKEND_URL` (and optionally `GOOGLE_SERVER_CLIENT_ID` and `REVENUECAT_ANDROID_API_KEY`) from local.properties. There is no AI key in
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

The Xcode target is still called `iosApp`, but its product is `BudgetHunter` (`PRODUCT_NAME` in
`iosApp/Configuration/Config.xcconfig`; the target must not override it with `$(TARGET_NAME)`). That name is the
bundle's `CFBundleName`, which iOS shows in Shortcuts, Siri and some system screens; the home screen uses
`CFBundleDisplayName` ("Budget Hunter").

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

### Sync
Offline-first: every change is written to SQLite first and pushed later; screens only read the database.
`BudgetSyncManager` (budgets) and `BudgetEntrySyncManager` (entries of one budget, serialized by a mutex) push
then pull; `RealTimeSyncManager` pulls a shared budget's entries on SSE events while its detail is open.
- `server_id` null: never reached the server. `is_synced = 0`: holds a change of its own still to be pushed.
- A change still to be pushed wins over the server until it is pushed: a pull never overwrites it (it only takes
  a missing server id, or a budget's collaborator count, written as that column alone), and the server's values
  are stored only with `WHERE is_synced = 1` (`updateFromServerIfSynced`), so an edit saved while a pull runs
  stays. Two people editing the same row: the last to reach the server wins.
- A push stores the server id always, but takes the answer and marks the row synced only if it still holds what
  was pushed (`markPushedIfUnchanged`, `markSyncedIfUnchanged`). Plain updates never clear a server id.
- Deletions: the row is deleted at once and, if it had a server id, recorded in `pending_deletion`
  (`PendingDeletion.sq`), pushed first in every sync (budgets: `syncPendingDeletions`; entries: with the
  budget's pending entries). A deletion is done once the server confirms it or answers 400/403/404
  (`isGoneFromServer`: gone or out of reach); other failures keep it. Pulls skip what is in that table, and
  sign out / account switch empty it. Deleting a budget drops its entries' pending deletions (the server
  cascades).
- Gone from the server: the budgets' pull drops synced budgets the server no longer lists (an empty list is
  not trusted); the entries' pull drops synced entries the server no longer returns (an empty list is trusted:
  they come back if it was wrong); an SSE `DELETED` drops a synced entry. Rows with a change still to be pushed
  are kept: an entry edited here and deleted elsewhere is created again (its PUT gets 400/404, then a POST).
- The backend answers 403 for a deleted budget (its access check runs first) and 400 for a deleted entry, not 404.
- Sync code reads the database, never `BudgetLocalDataSource.getAllCached()`, which lags behind writes.
- `BudgetSyncManager`'s operations (full sync, push, deletions, pull) are serialized by a mutex, like the entries':
  two pushes at once would POST the same new budget twice. A create, edit or delete made while a sync runs waits
  for it. The entries' lock is only ever taken inside the budgets' one.
- Automatic sync: `BudgetListViewModel` syncs the budgets silently (no message, no spinner) when the app opens
  with a connection and whenever it comes back (`NetworkMonitor.isOnline` turning true), signed in and not in
  offline mode; a trigger while one runs is dropped. It lives in the list's view model, the root of every launch
  with a session, not app-wide: the process also starts in the background (SMS, notification listener, workers,
  the iOS Shortcut), where nothing should sync. Entries are not part of it (a budget's screen syncs them when shown).
- Splash: each wait for the store's update answer (`AppUpdateManager.checkForUpdates`) is limited to
  `SplashScreenViewModel.UPDATE_CHECK_TIMEOUT_MS` (5 s); past it the app goes on as if there were no update and
  later answers are dropped. Play may never answer (seen after a reboot), and nothing sets the Android
  `AppUpdateLauncherDelegate`, so an offered update is never launched and never answers again.

### Session
- A failed token refresh (`HttpClientFactory`) drops the tokens only when the server refuses the refresh token
  (401) or there is none left; no connection, timeouts, other statuses and unreadable answers keep the session.
  The backend keeps one refresh token per user, so signing in on another device ends the session here. A refusal
  calls `AuthSession.onSessionExpired`: the app goes to sign in with a notice and the last email filled in,
  clearing nothing, so the same account gets its pending entries pushed. Known trade-off: if the server rotated the token but its answer was lost, the next refresh is
  refused and the user signs in again. Signing out (`SignOutUseCase`) is different: it deletes every local budget
  and entry.
- Deleting the account (Settings, `DeleteAccountUseCase`): `DELETE /api/users/me`, right after a `GET` of the same
  path that renews an expired access token; a 204 runs `SignOutUseCase`, a 401 is not refreshed (`AuthCircuitBreaker`,
  which also makes the request send its own token, see `createHttpClient`'s `sendWithoutRequest`) and ends the session
  like an expired one, anything else keeps it. The confirmation reads the plan (`PremiumRepository.status()`) to warn that a store subscription is not cancelled;
  the sign out it runs also logs the store out.

### Network errors
- The app's HTTP client does not set `expectSuccess` (tests often do: check both), so a plain `body()` reads a
  refusal as the answer: an error body fails as `ParseError`, and `body<Unit>()` takes a 404 or 500 as success.
  Calls read the answer with `bodyOrThrow()` (`ApiError.kt`), which throws the status's `ApiError` first
  (`HttpStatusCode.toApiError()`); the sync's 400/403/404 handling and categorization's 403/409 depend on it.
  New calls use `bodyOrThrow()`, never a plain `body()`, unless they check the status first (and say so).
  `ApiStatusTest` runs the services through the real `createHttpClient`.
- Timeouts: iOS gives the client a 20 s socket timeout (`API_SOCKET_TIMEOUT`; NSURLSession's own is 60 s, and a
  request sent down a silently dead connection kept the spinner up that long); Android keeps OkHttp's 10 s.
  Requests that set their own (SSE, receipts, categorization) keep theirs. Any timeout is `ApiError.Timeout`.
  Known risk: a POST that timed out may still have reached the server, and the sync's retry then creates a
  duplicate (an idempotency key on the backend would fix it).
- `safeApiCall` and `AuthRepository` rethrow `CancellationException`: a cancelled call ends its scope, it is not a
  failure.

### Privacy policy
The backend serves the policy at `/privacidad` (`PrivacyPolicy.url` = `BuildKonfig.BACKEND_URL` + that path) and
records each account's acceptance (backend CLAUDE.md, "Privacy policy acceptance"; Ley 1581 de 2012).
- Sign up with email: a required checkbox ("I accept the privacy policy", with the link); the button stays disabled
  and `SignUpViewModel` sends nothing without it. Google: the welcome says under its button that continuing accepts
  the policy. Both send `acceptedPrivacyPolicyVersion` = `PrivacyPolicy.VERSION`, a constant: nothing can be asked
  before there is an account, and the server ignores (never rejects) a version that is not its current one. Keep it
  equal to the backend's `PRIVACY_POLICY_VERSION`; if it falls behind, the check below asks again right after.
- Existing accounts and policy changes: after every sign in and when the app opens with a session, navigation sends
  `PrivacyPolicyIntent.Check`. `CheckPrivacyPolicyUseCase` reads `privacyPolicy` from `GET /api/users/me` and, when
  `acceptedVersion != currentVersion`, `PrivacyPolicyDialog` (over every screen, no back or tap outside) asks:
  "Accept" posts the server's `currentVersion` to `POST /api/users/me/privacy-policy`; "Sign out" runs
  `SignOutUseCase`, first warning like Settings when unsynced changes would be lost.
  The dialog is never shown over splash or sign in, and reaching sign in drops it (`SessionEnded`): a session can
  expire while it is open. A new check (a new sign in) also drops any question left from the previous session.
- Offline mode is a flag (`PreferencesManager.isOfflineModeEnabled`) that the check skips, so it must not outlive a
  sign in: `PrepareDataForAccountUseCase` clears it on every sign in (also from offline mode's "Sign in" menu), and
  the splash clears one found on with a session (left by builds before that fix).
- It never blocks: offline mode (no account, nothing sent) asks nothing; no connection, an error, a server without
  `privacyPolicy`, or a failed accept or sign out show nothing or close the dialog, and the next check asks again.
- Settings has a "Privacy policy" row that opens the page, offline mode too (Google Play asks for it in the app).

### Google sign in that links an existing account
When Google sign in finds a password account with the same email and no Google link, the server cannot tell who set
that password (it may be someone who registered the email first). It removes the password, ends every other session,
and answers `passwordRemoved: true` with `sharedBudgets` (the budgets the account already shares with other people,
which it keeps: only the user can tell a stranger's budget from a family one). Backend CLAUDE.md, "Authentication
and sessions".
- `SignInWithGoogleUseCase` saves a `GoogleLinkNotice` (`PreferencesManager.googleLinkNotice`, JSON) after
  `PrepareDataForAccountUseCase`, which would otherwise forget it on an account switch. Sign out forgets it too.
- The budget list shows it as a banner until the user dismisses it; tapping it opens `GoogleLinkNoticeDialog`
  (the password is off, set a new one in Settings; the shared budgets with their owner and members, and to delete
  the ones they do not recognize or remove strangers in Collaborators). Only "Got it" closes it, which clears it.
- Kept on the device rather than shown once, so an app killed before the user read it shows it again.

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
- Scans left: the entry screen shows a quiet line under the receipt card when 3 or fewer scans remain this month
  (`Allowance.isRunningLow`; a count, not a share of the limit, so PREMIUM is not nagged from 200 on), and "No scans
  left" with "See Premium" at 0 (also after a 402 with no count known). `GetReceiptScansUseCase` reads `GET /api/ai/usage` once when the screen opens (only with AI processing on,
  signed in and not offline; null on any error, which shows nothing); after that each scan's answer brings the count
  (`ReceiptExtractionResponse.usage`, also in the 402 body, through `AiExtractionResult.scans`), with no other call.
  Only informative: the app never blocks a scan.
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
  listener (Settings warns too). Some devices (seen on Samsung) leave it unbound for good, with everything granted:
  after the app's notifications are switched off and on, and also after the system kills the app for memory while
  the listener is bound (seen on a Galaxy A55 with no toggling at all: `dumpsys activity exit-info` shows the kill at
  importance 125, then only cached processes). requestRebind, toggling the component, re-granting the access or the
  app killing itself do not help, only a force stop from the system settings does.
  `IsNotificationReaderStoppedUseCase` (a single) detects it (`BankNotificationListenerService.isConnected`) while some
  bank uses APP or EMAIL; a reader found unbound gets a 3 s grace period for the bind, counted from the first check
  that found it so (reset by any check that finds it connected or not needed), and later checks answer at once, so
  Settings shows the warning without a pause. The budget list shows a banner that leads to Settings, which asks the
  user for that force stop. Title and text are parsed together (`BankNotification`) by the same parser, the entry is
  described by the text (with the bank name, "Nu: ..."), and unreadable notifications are dropped silently (apps also
  post codes and news).
- With the app closed, `NotificationReaderWatchWorker` (WorkManager, every 15 min, scheduled in `MyApplication`) runs
  `WarnIfNotificationReaderStoppedUseCase`: a reader still stopped a minute after it was found so gets a notification
  that opens Settings, once per stop (`PreferencesManager.isReaderStoppedNotified`, cleared when a run finds it
  running, even with notifications off; with them off nothing is shown nor marked). That notification has a fixed id
  of its own (`showReaderStoppedNotification`), so movement notifications never replace it, and a run that clears
  the mark also removes it from the shade (`cancelReaderStoppedNotification`).
- Bank emails are read from the email app's notification (Gmail, Outlook, Samsung Email...:
  `BankEmail.EMAIL_APP_PACKAGES`) by the same listener, only for banks with the EMAIL channel. The sender is checked
  before anything else is read (`BankEmail.findSender`): the display name (the title) can be written by anyone, so
  when the email app attaches the sender's address (`EXTRA_PEOPLE_LIST` / `EXTRA_PEOPLE`, "mailto:..."), the address
  decides: an address under a bank's `emailDomains` is that bank's whatever its name, and a bank's name on any other
  address is dropped as a fake. Only banks without known domains, or apps that attach no address, fall back to the
  name (`BankSmsConfig.isSender`). The domains in `SupportedBanks` need checking against real emails: a wrong one
  drops that bank's real emails. Then subject and the start of the body (the expanded text without its first line,
  the subject) are read like a bank app notification (`BankEmail.toNotification`), only up to the sentence that
  completes the movement: the footer's security tips ("código de seguridad") would otherwise mark it as no movement;
  anything but a movement or a missing default budget is dropped silently.
- Notifications posted again are registered once. Gmail posts an email again while it is in the shade (on sync, after
  a reboot), and the process may have died since (frequent LOW_MEMORY kills on the Galaxy A55), so the in-memory
  `RecentNotifications` (key + `when`) is only a quick filter: `RegisteredMovements.registerOnce` keeps the last 200
  movements on disk (DataStore key `registered_movements`, hashed by `MovementFingerprints`), under a lock (a Koin
  single), and remembers one right after its row is stored locally (`BudgetEntryRepository.createLocally`; the push
  runs after, outside the lock, and a row left unpushed goes up with the next full sync): a failed store is tried
  again by the next copy, and codes, ads or a missing default budget are never remembered. When in doubt (the list
  cannot be read or written) it registers: a copy the user can delete is better than a lost movement. An email
  movement is told by bank, when the email arrived (the notification's `when`, which Gmail keeps on a repost; the read
  day if missing), type, amount and the email's whole text as shown, subject and body
  (`RegisterSmsEntryUseCase.registerEmail`), not the description (first sentence only). So a repost is the same
  movement, while two emails that read alike but arrived apart are two; two email apps showing different snippets or
  times of an email register it twice. A bank app notification is told by its key + `when` only
  (`registerNotification`'s `identity`; equal transfers are real: "te envió 1000"), and one without `when` is not
  remembered on disk. This is not the SMS + email deduplication above: those texts differ.
- iOS cannot read SMS: a Shortcuts automation (trigger "Mensaje") passes the text to the App Intent
  `RegisterExpenseFromMessageIntent` (iosApp/, also an App Shortcut), which calls
  `SmsShortcutEntryPoint` (iosMain) -> `RegisterSmsFromShortcutUseCase` and answers with a dialog.
  It runs with the app closed, so `SmsShortcutEntryPoint` starts Koin itself (`startKoinIfNeeded`) with the Swift
  managers (`IOSNativeDependencies`, implementing the Kotlin `NativeDependencies`, created only when Koin starts), like
  `MainViewController` does.
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
- The AI part has a monthly allowance per plan (server side: FREE 50, PREMIUM 1000 descriptions sent to the AI;
  keyword rules and cache are free). Past it the run still answers 200 with the rest pending and `quotaExceeded`;
  the metrics snackbar then says so with a "See Premium" action (`CategorizationOutcome.Done.quotaExceeded`).
- SMS entries are created `UNCATEGORIZED`; a category chosen by the receipt AI is a normal category and is never
  overwritten. The server's rules live in the backend repository.
- Design notes and decisions: `docs/plans/auto-categorization.md`.

### Monetization
Freemium, with RevenueCat for the store purchases (in progress; plan and decisions in `docs/plans/monetization.md`).
- Free: everything manual, SMS reading, metrics, and monthly allowances of AI receipt scans (10) and AI
  categorizations (50), both server settings. Premium: much larger allowances (fair use) and, later, the other
  paid features.
- The server is the source of truth: `users.plan` and the monthly counts live there (`GET /api/ai/usage` has both;
  `GET /api/receipts/usage` only the scans).
  The app never decides on its own whether something is allowed.
- Purchases (`billing/`): `PremiumRepository` asks the backend for the account's RevenueCat id
  (`GET /api/billing/status`, a random id, never the email), logs the store in with it before every offer list,
  purchase and restore (so a purchase cannot land on the previous account), and after buying or restoring calls
  `POST /api/billing/sync` so premium applies at once (if that fails the webhook applies it a moment later).
  `StoreBilling` is the platform side: `AndroidStoreBilling` uses RevenueCat's Android SDK (`com.revenuecat.purchases`,
  configured lazily with the public `goog_` key from `REVENUECAT_ANDROID_API_KEY`; empty = `isAvailable` false and
  nothing can be bought), with `automaticDeviceIdentifierCollectionEnabled(false)`: no device identifiers for ad
  attribution, so none are declared in Play's Data safety form. iOS has `UnavailableStoreBilling` until the iOS launch. Sign out logs the store out.
  The purchase sheet needs the Activity, handed over by `MainActivity` like Google sign-in.
- Paywall: `PremiumScreen` / `PremiumViewModel` (`billing/`), opened from Settings (signed-in accounts only), from
  the "no scans left" snackbar's "See Premium" action (`BudgetEntryEvent.ShowNotification.offersPremium`) and from
  the entry screen's scans line (`BudgetEntryEvent.OpenPremium`). It shows
  the plan and this month's AI usage from the server (scans and categorizations, `GET /api/ai/usage`), the store's plans (yearly first) and restore; a premium account
  gets "manage in Google Play" instead. A purchase the server has not synced yet says premium arrives in minutes.
  The renewal and cancellation text under the plans is required by Google Play: keep it.
- Shared budgets: FREE owns 1 shared budget with 1 person besides the owner, PREMIUM is unlimited; the budget
  **owner's** plan decides, being invited is free, and nothing already shared is taken away. Past a limit,
  `POST /api/budgets/{id}/collaborators` answers 402 `SHARING_LIMIT_REACHED`, read by `CollaboratorApiService` as
  `ApiError.SharingLimitReached(limitType, callerIsOwner)`: the collaborators snackbar offers "See Premium" only
  when `callerIsOwner` (only the owner's upgrade lifts it; a member is told the owner's plan allows no more).
  `BillingStatusResponse.sharing` (null on older servers; a null limit is unlimited) becomes `SharingStatus`: the
  owner (`UserInfo.isOwner`) of a budget on FREE sees a hint with the limits, and the paywall a "Sharing" card. Both
  are only informative.
