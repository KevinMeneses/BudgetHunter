# Design: User Feedback for Sync Operations + UiEvent Refactor

**Date**: 2026-03-12
**Task**: Phase 10.5 — Add User Feedback for Sync Operations
**Scope**: Introduce a typed `UiEvent` channel per ViewModel for one-shot events (navigation, messages), replacing the current pattern of embedding transient events in state. Add new snackbar feedback messages for sync and SSE operations.

---

## Problem

Several one-shot events are currently stored as nullable/boolean fields in `BudgetListState` and `BudgetDetailState`:

- `navigateToBudget`, `navigateToSignIn` in `BudgetListState`
- `goBack`, `showEntry`, `syncError` in `BudgetDetailState`

These require matching `Clear*` events and `ClearNavigation` events to reset them after consumption. This creates boilerplate, a risk of events being consumed multiple times, and a known bug (`goBack` is never cleared). Additionally, no user-facing feedback exists for successful sync operations or real-time SSE notifications.

---

## Goals

1. Replace one-shot state fields with a `Channel`-backed `Flow` per ViewModel
2. Define typed sealed interfaces (`BudgetListUiEvent`, `BudgetDetailUiEvent`) for all one-shot outputs
3. Remove the corresponding `Clear*`, navigation, and one-shot events from `BudgetListEvent` and `BudgetDetailEvent`
4. Add snackbar feedback messages for sync operations and SSE notifications

---

## Architecture

Each ViewModel exposes two streams:

```
ViewModel
  ├── uiState: StateFlow<S>       — persistent UI state (loading flags, lists, modal visibility)
  └── uiEvents: Flow<UiEvent>     — one-shot events via Channel(BUFFERED)
```

`Channel(Channel.BUFFERED)` is used over `SharedFlow` to guarantee delivery even if the collector is temporarily inactive (e.g., screen briefly backgrounded). Note: events buffered in a Channel are lost if the ViewModel is cleared and recreated (e.g., process death). This is an accepted limitation — snackbar messages are ephemeral and do not need to survive process death.

---

## Sealed UiEvent Interfaces

**Package**: same `application/` subpackage as the existing event classes for each feature.

### Format Args Pattern

`ShowMessage` and `ShowError` carry `formatArgs: List<Any>` to support parameterized string resources (e.g., "Signed in as %s"). `List<Any>` is used instead of `Array<Any>` because `List` implements structural equality — this is important for unit tests that assert on emitted events. Format substitution happens at the screen's collection site using `StringResourceProvider.getString(resource, *formatArgs.toTypedArray())`. Non-parameterized messages pass an empty list.

### BudgetListUiEvent

**File**: `budgetList/application/BudgetListUiEvent.kt`

```kotlin
sealed interface BudgetListUiEvent {
    data class NavigateToBudget(val budget: Budget) : BudgetListUiEvent
    data class ShowMessage(val message: StringResource, val formatArgs: List<Any> = emptyList()) : BudgetListUiEvent
    data class ShowError(val message: StringResource, val formatArgs: List<Any> = emptyList()) : BudgetListUiEvent
    object NavigateToSignIn : BudgetListUiEvent
}
```

`ShowError` is shown with `SnackbarDuration.Long`; `ShowMessage` with `SnackbarDuration.Short`.

### BudgetDetailUiEvent

**File**: `budgetDetail/application/BudgetDetailUiEvent.kt`

```kotlin
sealed interface BudgetDetailUiEvent {
    object NavigateBack : BudgetDetailUiEvent
    data class NavigateToEntry(val entry: BudgetEntry) : BudgetDetailUiEvent
    data class ShowError(val message: StringResource, val formatArgs: List<Any> = emptyList()) : BudgetDetailUiEvent
    data class ShowMessage(val message: StringResource, val formatArgs: List<Any> = emptyList()) : BudgetDetailUiEvent
}
```

`ShowError` is displayed with `SnackbarDuration.Long`; `ShowMessage` with `SnackbarDuration.Short`.

Example ViewModel emission for a parameterized message:
```kotlin
channel.send(BudgetDetailUiEvent.ShowMessage(Res.string.sse_entry_created, listOf(notification.userInfo.name)))
```

Example for a non-parameterized message (e.g. from `ApiError.messageResource`):
```kotlin
channel.send(BudgetDetailUiEvent.ShowError(apiError.messageResource))
```

---

## State Cleanup

Fields removed from state and the events that are removed alongside them:

| State class | Fields removed | Events removed from event class |
|---|---|---|
| `BudgetListState` | `navigateToBudget`, `navigateToSignIn` | `OpenBudget`, `SignIn`, `ClearNavigation`, `ClearSignInNavigation` |
| `BudgetDetailState` | `goBack`, `syncError` | `ClearNavigation`, `ClearSyncError` |

`OpenBudget` and `SignIn` are removed from `BudgetListEvent` because they existed solely to set the now-removed state fields — their responsibility moves to the ViewModel emitting `NavigateToBudget` / `NavigateToSignIn` UiEvents directly.

The corresponding `when` branches must be deleted from `sendEvent()` in both ViewModels:
- `BudgetListViewModel.sendEvent()`: remove branches for `OpenBudget`, `SignIn`, `ClearNavigation`, `ClearSignInNavigation`
- `BudgetDetailViewModel.sendEvent()`: remove branches for `ClearNavigation`, `ClearSyncError`

Dead private functions that become unreachable after branch removal must also be deleted:
- `BudgetListViewModel`: delete `signIn()`, `clearNavigation()`, `clearSignInNavigation()`. **Do not delete `openBudget()`** yet — see note below.
- `BudgetDetailViewModel`: delete `clearNavigation()`, `clearSyncError()`

`BudgetListViewModel.createBudget()` currently calls `openBudget(budgetSaved)` after creating a budget. Once `openBudget()` is removed as a private function, this internal call must be replaced with `channel.send(BudgetListUiEvent.NavigateToBudget(budgetSaved))` inside the `createBudget()` coroutine body. After updating `createBudget()`, delete `openBudget()`.

`BudgetListViewModel.signOut()` corrected form — `_uiState.update` must come before `channel.send` (state updates synchronously; channel send is `suspend` and cannot be inside `_uiState.update {}`):
```kotlin
_uiState.update { it.copy(isAuthenticated = false) }
channel.send(BudgetListUiEvent.NavigateToSignIn)
```

`BudgetDetailViewModel.deleteBudget()` currently calls `budgetDetailRepository.deleteBudget(budgetId)` and sets `goBack = true` inside a single `_uiState.update {}` lambda. The entire `_uiState.update {}` wrapper must be removed. The corrected form is:

```kotlin
private fun deleteBudget() = viewModelScope.launch {
    val budgetId = _uiState.value.budgetDetail.budget.id
    budgetDetailRepository.deleteBudget(budgetId)
    channel.send(BudgetDetailUiEvent.NavigateBack)
}
```

`channel.send()` is a `suspend` function and cannot be called inside the synchronous `_uiState.update {}` lambda.

`showEntry` is removed from `BudgetDetailState` but **`BudgetDetailEvent.ShowEntry` is kept** — the screen still fires `onEvent(ShowEntry(entry))` from the AppBar click handler. The ViewModel's `showEntry()` implementation changes from `_uiState.update { it.copy(showEntry = entry) }` to emitting `BudgetDetailUiEvent.NavigateToEntry(entry)`.

All loading flags (`isSyncing`, `isSyncingEntries`, `isCreatingBudget`, etc.) and modal visibility fields remain in state.

---

## New Feedback Messages (Task 10.5)

| Trigger | ViewModel | UiEvent emitted | Message |
|---|---|---|---|
| Pull-to-refresh sync completes (success) | `BudgetListViewModel` | `ShowMessage` | "Budgets synced successfully" (emitted in the `try` block after `budgetRepository.sync()` completes, before `finally`) |
| Pull-to-refresh sync fails | `BudgetListViewModel` | `ShowError` | Uses `ApiError.messageResource` (emitted in the `catch` block, before the `finally` delay — acceptable for the snackbar to appear while the refresh spinner is still dismissing) |
| Manual entry sync completes (success) | `BudgetDetailViewModel` | `ShowMessage` | "Entries synced successfully" |
| Manual entry sync fails | `BudgetDetailViewModel` | `ShowError` | Uses `ApiError.messageResource` |
| SSE event received (CREATED) | `BudgetDetailViewModel` | `ShowMessage` | "New entry from [collaborator name]" |
| SSE event received (UPDATED) | `BudgetDetailViewModel` | `ShowMessage` | "Entry updated by [collaborator name]" |
| SSE event received (DELETED) | `BudgetDetailViewModel` | `ShowMessage` | "Entry deleted by [collaborator name]" |
| Sign-in completes | `BudgetListViewModel` | `ShowMessage` | "Signed in as [user name]" |

### "Signed in as [name]" Implementation

The user's name is available in `AuthResponse` during sign-in. `AuthRepository` already uses `TokenStorage` (a DataStore wrapper) for auth-related persistence. `TokenStorage` will be extended with `getUserName(): String?`, `setUserName(name: String)`, and `clearUserName()` methods. `AuthRepository.signIn()` will call `tokenStorage.setUserName(response.name)` after storing tokens. `AuthRepository.refreshToken()` must **not** call `setUserName()` — token refresh is a silent session-continuity operation, not a new sign-in, and calling it there would re-arm the one-time signal incorrectly. A new `AuthRepository.getStoredUserName(): String?` delegates to `tokenStorage.getUserName()`.

`BudgetListViewModel.checkAuthState()` already detects the authenticated state. It will be updated to emit `ShowMessage("Signed in as [name]")` using the following logic: emit the message **if and only if** a stored user name exists in `TokenStorage`. The stored name is written on sign-in and cleared on sign-out (see `TokenStorage` changes below). This means:
- Fresh app launch while already authenticated: `checkAuthState()` finds a name → emits the message ❌

To avoid the false-positive on app restart, the stored username is **deleted from `TokenStorage` immediately after being read** in `checkAuthState()`. This makes the "name present" condition a one-time-consumable signal: it is written once on sign-in and consumed (and cleared) once on the next `checkAuthState()` call. Sign-out also clears it as a safety measure via `clearTokens()`.

`checkAuthState()` and `collectBudgetList()` run concurrently from `init`. The "Signed in as" message may fire while `isLoading = true` is still showing. This is acceptable — snackbars appear over any screen content, including loading states.

The read of the username and the subsequent `clearUserName()` in `checkAuthState()` are two separate DataStore writes and are not atomic. If the process is killed between them, the welcome message may fire again on the next cold start. This is an accepted limitation consistent with the process-death caveat for Channel events stated above.

`BudgetListViewModel.signOut()` currently sets `_uiState.update { it.copy(navigateToSignIn = true, isAuthenticated = false) }` after sign-out. With `navigateToSignIn` removed from state, replace this line with:

```kotlin
_uiState.update { it.copy(isAuthenticated = false) }
channel.send(BudgetListUiEvent.NavigateToSignIn)
```

The `isAuthenticated = false` state update must be preserved — it controls whether the sign-in/sign-out menu item is shown correctly.

### SSE Collaborator Name

The SSE event payload already includes `userInfo: UserInfo(email, name)`. `RealTimeSyncManager` currently discards this field. See the `RealTimeSyncManager` changes section below.

---

## RealTimeSyncManager Changes

`RealTimeSyncManager` currently launches a coroutine internally, calls `syncManager.pullEntriesFromServer()` directly, and exposes no data to the ViewModel beyond start/stop lifecycle control.

To surface `UserInfo`, add a `SharedFlow<BudgetEntryEvent>` to `RealTimeSyncManager`. `BudgetEntryEvent` already carries `action: BudgetEntryAction`, `entryId: Long`, and `userInfo: UserInfo` — no new type is needed.

```kotlin
// In RealTimeSyncManager:
private val _notifications = MutableSharedFlow<BudgetEntryEvent>(extraBufferCapacity = 10)
val notifications: SharedFlow<BudgetEntryEvent> = _notifications.asSharedFlow()
```

`RealTimeSyncManager` emits to `_notifications` for each SSE event received. The `CREATED` and `UPDATED` actions previously called `syncManager.pullEntriesFromServer()` directly — this call is removed and replaced by the notification emission; `BudgetDetailViewModel` becomes responsible for triggering the pull for these actions.

The `DELETED` action previously performed a **local database delete** (`localDataSource.delete(existingEntry.id)`) — this direct delete is **retained** in `RealTimeSyncManager` alongside the notification emission. Delegating local deletes to the ViewModel would require breaking the repository layer, so the hybrid approach is correct: `RealTimeSyncManager` handles DELETED locally and emits the notification; the ViewModel does **not** call `syncEntries()` for DELETED events.

**Bug fix required**: The existing `syncEntries()` in `BudgetDetailViewModel` has a logic error — when `showErrors = false`, it sets `isLoading = true` (the opposite of intended). The line `isLoading = if (showErrors) it.isLoading else true` must be changed to `isLoading = if (showErrors) it.isLoading else false` as part of this task.

`BudgetDetailViewModel` collects `realTimeSyncManager.notifications` in its `init` block (alongside the existing SSE start/stop logic). For each notification:
1. If action is `CREATED` or `UPDATED`: calls `syncEntries(budgetId = _uiState.value.budgetDetail.budget.id, serverId = _uiState.value.budgetDetail.budget.serverId, showErrors = false)` to pull the latest data silently (no loading indicator, no error snackbar — background operation). Note: fix only the `_uiState.update` setup block (`isLoading = if (showErrors) it.isLoading else false`); the `finally` block's `isLoading` reset is correct and must not be changed.
2. If action is `DELETED`: skips `syncEntries()` — the local delete was already handled by `RealTimeSyncManager`.
3. Emits a `ShowMessage` UiEvent with the collaborator's `userInfo.name` for all three actions.

---

## Screen-side Collection

### String Resolution in LaunchedEffect

`stringResource()` is a `@Composable` function and cannot be called inside a `LaunchedEffect` (which runs in a coroutine). Both screens must inject `StringResourceProvider` (already in the Koin DI graph) via `koinInject()` and use it to resolve `StringResource` to `String` inside the coroutine. Note: `StringResourceProvider.getString()` is a `suspend` function and must only be called from within a coroutine context — the `LaunchedEffect` body qualifies.

```kotlin
val stringProvider = koinInject<StringResourceProvider>()

LaunchedEffect(Unit) {
    viewModel.uiEvents.collect { event ->
        when (event) {
            is SomeUiEvent.ShowMessage ->
                snackbarHostState.showSnackbar(stringProvider.getString(event.message), ...)
        }
    }
}
```

### BudgetDetailScreen

`BudgetDetailScreen.Show()` already receives injected lambdas: `goBack: () -> Unit` and `showBudgetEntry: (BudgetEntry) -> Unit`. The `LaunchedEffect(Unit)` collector calls these lambdas for navigation events:

```kotlin
val stringProvider = koinInject<StringResourceProvider>()

LaunchedEffect(Unit) {
    viewModel.uiEvents.collect { event ->
        when (event) {
            is BudgetDetailUiEvent.NavigateBack -> goBack()
            is BudgetDetailUiEvent.NavigateToEntry -> showBudgetEntry(event.entry)
            is BudgetDetailUiEvent.ShowError ->
                snackbarHostState.showSnackbar(stringProvider.getString(event.message, *event.formatArgs.toTypedArray()), duration = SnackbarDuration.Long)
            is BudgetDetailUiEvent.ShowMessage ->
                snackbarHostState.showSnackbar(stringProvider.getString(event.message, *event.formatArgs.toTypedArray()), duration = SnackbarDuration.Short)
        }
    }
}
```

The existing `LaunchedEffect(syncErrorMessage)` block is removed.

`BudgetDetailScreen` has a `DisposableEffect(Unit)` that currently: (1) calls `SetBudget` and `GetBudgetDetail` in its setup body, and (2) calls `ClearNavigation` in its `onDispose` body. The `onDispose { ClearNavigation.run(onEvent) }` call is replaced with an empty `onDispose { }` block — Compose's `DisposableEffect` API requires an `onDispose` block to always be present. The setup calls (`SetBudget`, `GetBudgetDetail`) remain unchanged.

Also remove the two state-driven `LaunchedEffect` blocks that become dead code once `goBack` and `showEntry` are removed from state:
- `LaunchedEffect(key1 = uiState.goBack) { ... }`
- `LaunchedEffect(key1 = uiState.showEntry) { ... }`

### BudgetListScreen

`BudgetListScreen.Show()` is the **single collector** for all `BudgetListUiEvent`s. To avoid a split-collection race condition with `ScreenRouting`, a `navigateToSignIn: () -> Unit` lambda is added as a new parameter to `BudgetListScreen.Show()` (consistent with how `BudgetDetailScreen` already handles navigation via injected lambdas).

The existing `showBudgetDetail: (Budget) -> Unit` parameter is **renamed** to `openBudget: (Budget) -> Unit` for clarity. `BudgetListContent` also receives `openBudget: (Budget) -> Unit` as a new parameter (added alongside the existing `onEvent`) and calls it directly from the card's `onClick` instead of `BudgetListEvent.OpenBudget(budget).run(onEvent)`, bypassing the ViewModel since there is no business logic involved.

`BudgetListMenu`'s sign-in click currently uses `onSignInClick: () -> Unit` as a parameter. **Rename** `onSignInClick` to `navigateToSignIn` in `BudgetListMenu`'s signature (do not add a second parameter alongside the existing one). The call site inside `BudgetListScreen.Show()` changes from `onSignInClick = { BudgetListEvent.SignIn.run(onEvent) }` to `navigateToSignIn = navigateToSignIn`. `BudgetListScreen.Show()` must also pass `openBudget = openBudget` to the `BudgetListContent(...)` call (adding `openBudget` alongside the existing `onEvent` parameter).

`ScreenRouting.kt` updates its call site to use the renamed `openBudget` parameter and also passes the `navigateToSignIn` lambda.

```kotlin
val stringProvider = koinInject<StringResourceProvider>()

LaunchedEffect(Unit) {
    viewModel.uiEvents.collect { event ->
        when (event) {
            is BudgetListUiEvent.NavigateToBudget -> openBudget(event.budget)
            is BudgetListUiEvent.ShowMessage ->
                snackbarHostState.showSnackbar(stringProvider.getString(event.message, *event.formatArgs.toTypedArray()), duration = SnackbarDuration.Short)
            is BudgetListUiEvent.ShowError ->
                snackbarHostState.showSnackbar(stringProvider.getString(event.message, *event.formatArgs.toTypedArray()), duration = SnackbarDuration.Long)
            is BudgetListUiEvent.NavigateToSignIn -> navigateToSignIn()
        }
    }
}
```

`BudgetListScreen` already has an unused `SnackbarHostState` — it will be wired up to the `Scaffold`'s `snackbarHost`.

`BudgetListScreen` currently has a `DisposableEffect(Unit) { onDispose { ClearNavigation.run(onEvent) } }`. This is removed entirely — with the channel-based approach, `NavigateToBudget` is a one-shot emission consumed exactly once by the collector; there is nothing to clear after consumption.

Also remove the existing `LaunchedEffect(key1 = uiState.navigateToBudget)` block that currently calls `showBudgetDetail(it)` — this state-driven navigation is replaced by the `NavigateToBudget` UiEvent collector.

### ScreenRouting

`ScreenRouting.kt` passes the sign-in navigation lambda when calling `BudgetListScreen.Show()`. The `showBudgetDetail` parameter is renamed to `openBudget` at the call site. It includes the correct `navOptions` to clear the back stack (preserving the existing sign-out behavior):

```kotlin
BudgetListScreen.Show(
    uiState = uiState,
    onEvent = budgetListViewModel::sendEvent,
    openBudget = { budget -> navController.navigate(BudgetDetailScreen(budget)) },  // renamed from showBudgetDetail
    showSettings = { navController.navigate(SettingsScreen) },
    networkMonitor = networkMonitor,
    navigateToSignIn = {
        navController.navigate(SignInScreen) {
            popUpTo<BudgetListScreen> { inclusive = true }
        }
    }
)
```

The existing `LaunchedEffect(uiState.navigateToSignIn)` block in `ScreenRouting.kt` is removed entirely.

---

## String Resources

New strings to add to `strings.xml` (English and Spanish):

- `sync_budgets_success`: "Budgets synced successfully"
- `sync_entries_success`: "Entries synced successfully"
- `signed_in_as`: "Signed in as %s"
- `sse_entry_created`: "New entry from %s"
- `sse_entry_updated`: "Entry updated by %s"
- `sse_entry_deleted`: "Entry deleted by %s"

---

## Files Affected

| File | Change |
|---|---|
| `budgetList/application/BudgetListState.kt` | Remove `navigateToBudget`, `navigateToSignIn` |
| `budgetList/application/BudgetListEvent.kt` | Remove `OpenBudget`, `SignIn`, `ClearNavigation`, `ClearSignInNavigation` |
| `budgetList/application/BudgetListUiEvent.kt` | **New file** |
| `budgetList/BudgetListViewModel.kt` | Add channel + `uiEvents`, replace state mutations with UiEvent emissions (including `signOut()` → emits `NavigateToSignIn`); replace silent `catch` in `syncBudgets()` with `channel.send(ShowError(e.toApiError().messageResource))` in the `catch` block (before the `finally` delay — the snackbar firing while `isSyncing = true` is acceptable); add sync success feedback; add sign-in message |
| `budgetList/ui/BudgetListScreen.kt` | Rename `showBudgetDetail` → `openBudget`, add `navigateToSignIn` lambda param, collect `uiEvents`, wire up `SnackbarHostState`, remove state-driven navigation, remove `DisposableEffect(ClearNavigation)`, pass `navigateToSignIn` to `BudgetListMenu` |
| `budgetList/ui/BudgetListContent.kt` | Add `openBudget: (Budget) -> Unit` param; thread it down into the private `BudgetItem` composable; replace `onEvent(OpenBudget(budget))` in `BudgetItem`'s `onClick` with `openBudget(budget)` |
| `budgetList/ui/BudgetListMenu.kt` | Rename `onSignInClick` → `navigateToSignIn` in both the public signature and the private `SignInOption` helper; replace `BudgetListEvent.SignIn.run(onEvent)` with `navigateToSignIn()` |
| `budgetDetail/application/BudgetDetailState.kt` | Remove `goBack`, `showEntry`, `syncError` |
| `budgetDetail/application/BudgetDetailEvent.kt` | Remove `ClearNavigation`, `ClearSyncError` (keep `ShowEntry` — ViewModel handling changes) |
| `budgetDetail/application/BudgetDetailUiEvent.kt` | **New file** |
| `budgetDetail/BudgetDetailViewModel.kt` | Add channel + `uiEvents`, replace state mutations with UiEvent emissions, collect SSE notifications, fix `isLoading` bug in `syncEntries()` |
| `budgetDetail/ui/BudgetDetailScreen.kt` | Collect `uiEvents`, remove existing `LaunchedEffect(syncErrorMessage)`, remove `DisposableEffect(ClearNavigation)`, wire navigation lambdas |
| `navigation/ScreenRouting.kt` | Remove `LaunchedEffect(uiState.navigateToSignIn)`; update `BudgetListScreen.Show()` call site — rename `showBudgetDetail` to `openBudget`, add `navigateToSignIn` lambda with `popUpTo` navOptions |
| `budgetEntry/data/sync/RealTimeSyncManager.kt` | Add `notifications: SharedFlow<BudgetEntryEvent>`, emit on each SSE event, remove direct `pullEntriesFromServer()` calls for CREATED/UPDATED, remove unused `syncManager: BudgetEntrySyncManager` constructor parameter |
| `di/budgetEntryModule` (or equivalent) | Remove `BudgetEntrySyncManager` from `RealTimeSyncManager` constructor in the DI binding |
| `auth/data/AuthRepository.kt` | Add `getStoredUserName(): String?`; call `tokenStorage.setUserName()` on sign-in |
| `auth/data/TokenStorage.kt` | Add `getUserName(): String?`, `setUserName(name: String)`, `clearUserName()` methods; update `clearTokens()` to also clear the username key |
| `strings.xml` (en + es) | Add 6 new string resources |

---

## Error Handling

- Sync failures emit `ShowError` (long duration) with the existing `ApiError.messageResource` — no new error types needed.
- SSE errors remain silent (consistent with existing behavior) — the offline banner already communicates connectivity issues.

---

## Testing

- Unit tests for `BudgetListViewModel`: verify correct `UiEvent` is emitted for sync success, sync failure, and sign-in detection.
- Unit tests for `BudgetDetailViewModel`: verify correct `UiEvent` is emitted for sync success, sync failure, and each SSE action (CREATED, UPDATED, DELETED).
- Existing sync manager tests are unaffected.
- No UI tests required for this task.
