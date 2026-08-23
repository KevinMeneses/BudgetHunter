# Repository Guidelines

## Project Structure & Module Organization
- `app/` is the Android entry point with Jetpack Compose UI, domain logic, and SqlDelight schema under `app/src/main/sqldelight`; tests live in `app/src/test/java` and `app/src/androidTest/java`.
- `composeApp/` hosts Kotlin Multiplatform code: `commonMain` for shared models/use cases, platform glue in `androidMain` and `iosMain`, and shared assets in `composeResources/`.
- `iosApp/` is the Xcode wrapper that links the framework emitted by `composeApp`; adjust Gradle modules and `settings.gradle` when adding new shared code.

## Build, Test, and Development Commands
- `./gradlew build` compiles every variant and runs JVM unit tests; run this before opening a pull request.
- `./gradlew app:assembleDebug` (optionally followed by `app:installDebug`) produces a device-ready build.
- `./gradlew test` runs JVM specs, while `./gradlew connectedAndroidTest` covers instrumentation cases on an attached device or emulator.
- `./gradlew ktlint` checks formatting and `./gradlew ktlintFormat` applies automated fixes.

## Coding Style & Naming Conventions
- Kotlin code uses four-space indentation, expression-bodied functions when brief, and relies on ktlint for enforcement.
- Compose functions stay in `PascalCase` (`BudgetListScreen`), treat UI state as parameters, and keep previews adjacent to the implementation.
- Resource IDs use `snake_case` (for example, `ic_budget_add.xml`); SQLDelight tables are singular (`BudgetEntry`) to mirror domain models.
- Packages remain under `com.meneses.budgethunter.*`; create matching test packages so ktlint and IDE tooling resolve imports cleanly.

## Testing Guidelines
- Unit specs go in `app/src/test/java` using JUnit4, MockK, and `kotlinx-coroutines-test`; suffix files with `Test` and mirror production packages.
- Shared logic that targets Android and iOS belongs in `composeApp/src/commonTest`; prefer descriptive backtick-style test names.
- Use `runTest` with `StandardTestDispatcher` for coroutine code, and collect Flow results explicitly (see `BudgetListViewModelTest`).
- Validate platform-dependent work with `./gradlew test connectedAndroidTest`.

## Commit & Pull Request Guidelines
- Follow the current history: imperative, concise subjects with optional prefixes (`fix:`, `feat:`); example `feat: add SMS transaction parser`.
- Group related changes per commit and document notable side effects or follow-ups in the body as short bullet points.
- Pull requests should outline behavior changes, include screenshots for UI work, and link tracking issues alongside the commands you executed.

## Security & Configuration Tips
- Store secrets such as `GEMINI_API_KEY` only in `local.properties` (ignored by Git) and share mock values via documentation rather than commits.
- For CI, prefer environment variables or encrypted secrets over raw files, and call out any new keys in the pull request to alert reviewers.
