# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Testing
- `./gradlew build` - Build the entire project
- `./gradlew :app:assembleDebug` - Build Android APK
- `./gradlew test` - Run unit tests  
- `./gradlew connectedAndroidTest` - Run instrumented tests on connected device/emulator
- `./gradlew ktlint` - Check Kotlin code style
- `./gradlew ktlintFormat` - Auto-fix Kotlin code style issues
- `./gradlew clean` - Clean build artifacts

### Running the App
- **Android**: Use Android Studio or `./gradlew :app:installDebug` to install on device/emulator
- Debug builds use local.properties for GEMINI_API_KEY configuration

## Architecture Overview

BudgetHunter is currently a native Android budget tracking app that is **ready for Kotlin Multiplatform (KMP) migration**.

### Tech Stack
- **Kotlin 2.1.0** with **Jetpack Compose** for Android UI  
- **MVI (Model-View-Intent)** architecture pattern
- **Navigation Compose** for type-safe screen navigation (KMP compatible)
- **SqlDelight 2.0.2** for local database with SQLite
- **Koin 4.0.0** for dependency injection
- **Ktor 2.3.12** for WebSocket communication
- **Google Generative AI** for AI-powered budget entry extraction from images

### Current Project Structure
Standard Android app with clean architecture and feature-based packaging:

```
BudgetHunter/
└── app/                      # Android application
    └── src/main/java/com/meneses/budgethunter/
        ├── budgetList/          # Main budget listing feature
        ├── budgetDetail/        # Individual budget management
        ├── budgetEntry/         # Budget entry creation/editing
        ├── budgetMetrics/       # Analytics and reporting
        ├── settings/            # App configuration
        ├── splash/              # App initialization
        ├── sms/                 # SMS transaction parsing
        ├── commons/             # Shared utilities and UI components
        ├── theme/               # App theming and design system
        └── di/                  # Dependency injection modules
```

### Key Architectural Patterns

**Feature Module Structure:**
- `domain/` - Business entities and domain logic
- `data/` - Repository pattern with local data sources
- `application/` - Use cases, events, and state management
- `ui/` - Compose screens and UI components

**Database:**
- SqlDelight schema in `sqldelight/` directory
- Database operations through Repository pattern
- Singleton driver managed in MyApplication

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
- AI functionality in GetAIBudgetEntryFromImageUseCase

### SMS Processing
- SmsService and SmsBroadcastReceiver handle incoming transaction SMS
- Bank-specific configuration in commons/bank/ for different SMS formats
- Automatic budget entry creation from SMS transactions