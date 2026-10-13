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
- Debug builds use local.properties for GEMINI_API_KEY configuration

## Architecture Overview

BudgetHunter is a **Kotlin Multiplatform** budget tracking app (Android and iOS) with a Compose Multiplatform UI. It works offline-first on a local database and syncs with the BudgetHunter backend (collaborative budgets, real-time updates).

### Tech Stack
- **Kotlin 2.2.21** with **Compose Multiplatform** (versions in `gradle/libs.versions.toml`)
- **MVI (Model-View-Intent)** architecture pattern
- **Navigation Compose** for type-safe screen navigation
- **SqlDelight 2.0.2** for local database with SQLite
- **Koin 4.0.0** for dependency injection
- **Ktor 3.3.3** for the backend API, SSE and Gemini calls
- **Google Gemini** for AI-powered budget entry extraction from images

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
- Google Generative AI (Gemini) processes receipt images to extract budget entries
- API key stored in local.properties as GEMINI_API_KEY
- AI functionality in `CreateBudgetEntryFromImageUseCase` and `GeminiApiClient`
- The AI processing toggle in settings is kept on the account too (`SyncUserPreferencesUseCase`), because it also gates the server-side categorization below

### SMS Processing
- SmsService and SmsBroadcastReceiver handle incoming transaction SMS
- Bank-specific configuration in commons/bank/ for different SMS formats
- Automatic budget entry creation from SMS transactions

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
