# UI redesign (Nov 2026)

Proposal with before/after screenshots and mockups: https://claude.ai/artifact/Tbz2tLPukzQtaBqAAv1SPM

## Principles

- Yellow is an accent, not a background: `primaryContainer` only for the budget summary, selections
  and the main floating button. Cards and lists use `surfaceContainer`.
- Answer first: "how much is left" is the big number in the list and in the detail.
- Show only the exception: the sync state appears only when something is pending; the author of an
  entry only when more than one person writes in the budget.
- Actions in sight: metrics, collaborators, search and sort are chips in the detail; scanning a
  receipt has its own button.

## Status

| # | Change | Status | Where |
|---|--------|--------|-------|
| 1 | Entry row in two lines with category icon; no "Created/Updated by"; grouped by day with the day's net | Done | `budgetDetail/ui/BudgetDetailContent.kt`, `budgetEntry/ui/CategoryIcon.kt`, `commons/ui/DateLabels.kt` |
| 2 | Budget card with "Available", rounded bar, "Over budget" chip, "Not synced" chip only when pending; extended FAB | Done | `budgetList/ui/BudgetListContent.kt`, `BudgetListScreen.kt`, `commons/ui/StatusChip.kt` |
| 3 | Radius scale (8/12/16/28), localized dates ("Sáb 3 oct"), sentence case, Spanish strings for every new text | Done | `theme/Shape.kt`, `composeResources/values*/strings.xml` |
| 4 | Metrics / Collaborate / Search / Sort as chips; overflow menu keeps Settings and Delete (apart); "New entry" FAB + camera FAB that opens the form with the receipt picker | Done | `BudgetDetailScreen.kt`, `BudgetDetailMenu.kt`, `BudgetEntryScreen(scanReceipt)` |
| 5 | Single summary on top of the detail: balance, bar, budget (tap to edit), income, expenses | Done | `BudgetDetailContent.kt` |
| 6 | Entry form: amount as headline with the keyboard open, receipt card that offers the AI when it is on, category chips (one tap), Today/Yesterday/Other date, save button at the bottom, authorship at the foot | Done | `budgetEntry/ui/BudgetEntryForm.kt`, `EntryFormSections.kt`; `BudgetEntryState.isAiProcessingEnabled` |
| 7 | Settings as a grouped list: account header (email), full subtitles, plural banks, sign out with confirmation | Done | `settings/ui/SettingsScreen.kt`; `SettingsViewModel` gets `SignOutUseCase` and a `SettingsEvent.NavigateToSignIn` |
| 8 | Collaborators: explainer, inline invite, "(you)" on your own row without remove, empty note | Done | `collaborator/ui/CollaboratorsScreen.kt`; `CollaboratorsState.currentUserEmail` from `PreferencesManager.getLastSignedInEmail()` |
| 10 | Top app bar: title on the left (was centered), optional subtitle; Metrics and Collaborators show the budget as subtitle; list search moved next to the menu | Done | `commons/ui/AppBar.kt` |
| 9 | Metrics: category rows with icon open the detail filtered by that category and type | Done | `BudgetMetricsContent.kt`, `ScreenRouting.kt` (`METRICS_CATEGORY_FILTER_KEY` through `savedStateHandle`) |

| 11 | Sign in in two steps: a welcome (mark, tagline, Google / email / without an account) and the short email form behind it, both states of `SignInScreen`; sign up with name, email and password only, the minimum shown from the start and validation errors under their field | Done | `auth/ui/SignInScreen.kt`, `SignUpScreen.kt`, `AuthComponents.kt`; `SignUpState`/`SignUpIntent` lost the confirmation field |

## Decisions and deviations from the proposal

- The detail summary always describes the whole budget (`BudgetDetailState.budgetIncomes` /
  `budgetExpenses`, computed before the filter). A filter only narrows the list, and its own net
  shows in the filter bar: "4 de 17 movimientos · -$375,000". Mixing both made the balance and the
  bar meaningless while filtering.

- Category badges are neutral (only "Sin categoría" is blue): the metrics screen colors categories by
  rank, so a fixed color per category in the list would contradict it.
- The form shows the common categories (Food, Groceries, Transportation, Services) plus the selected
  one, and a "More" chip with the rest in a menu: all twelve as chips took five rows and pushed the
  save button down.
- `MainActivity` uses `windowSoftInputMode="adjustResize"`: without it, on Android 14 and older the
  keyboard covered the save button instead of resizing the screen.
- The list's ⋮ became an account circle (`AccountButton`, initial of `PreferencesManager.getLastSignedInEmail()`):
  signed in it opens Settings, where signing out now lives; in offline mode it shows a person icon and
  opens the old menu, which offers to sign in.
- The entry form closes with an X instead of a back arrow; unsaved changes are still confirmed.
- No collaborator count on the list cards or the detail chip at first; the cards got it later (see
  "Follow-ups").
- No entry count per category in metrics: it needs a change in the aggregation use case.

## After the PR review

- Sign out in Settings catches a failure, keeps the user signed in and shows "No se pudo cerrar
  sesión" in a snackbar, instead of crashing.
- Sign up keeps one error per field (`nameError`, `emailError`, `passwordError`) besides the general
  one, and checks every field at once.
- The metrics filter travels in two `savedStateHandle` keys, only to a budget detail, read once and
  removed.
- Day grouping is the pure `groupedByDay()` (`budgetDetail/domain/EntryDays.kt`), remembered per list;
  "Hoy / Ayer" is the pure `LocalDate.relativeTo(today)`.
- Strings the redesign left unused were removed from both locales.

## Follow-ups (after #66)

- Sign out ends the session first and clears the device afterwards (`SignOutUseCase`). A failure
  ending the session throws with nothing deleted; a failure in the cleanup is logged and left to
  `PrepareDataForAccountUseCase`, which drops the previous account's data when somebody else signs in.
- The sign-out confirmation warns when budgets or entries were never synced, since signing out
  deletes them (`SignOutUseCase.hasUnsyncedChanges`, `sign_out_unsynced_warning`).
- Navigation to sign in after a sign out follows `AuthSession.isSignInRedirectPending`, consumed once
  in `ScreenRouting` (`NavigateToSignInOnSignOut`), instead of `SettingsEvent.NavigateToSignIn`: the
  sign out runs in the application scope and the event was lost if the user had left Settings.
  The list menu's sign-out option was removed: that menu only opens in offline mode.
- Sign up with an email that already has an account shows the error under the email field
  (`EmailAlreadyRegisteredException`). The backend now answers 409; the app also accepts the old
  400 "Email already exists" until every deployment is updated.
- Budget cards show a people icon with the number of people sharing the budget, only when more than
  one. `collaboratorCount` comes in the server's `BudgetResponse` (one grouped query per list), is
  stored in `budget.collaborator_count` (migration `3.sqm`, default 1) and refreshed when the
  collaborators screen loads its list.
- Decided not to do: checking `MIN_PASSWORD_LENGTH` against the backend, and the WorkManager/goAsync
  hardening of the SMS push.

## Pending / ideas

- Password recovery: without the confirmation field a typo in the password is only caught by the
  eye icon; there is no "forgot password" flow yet (product decision).
- Collaborator count on the detail's chip.
- Entry count per category in metrics.
- Sign in / sign up and the iOS Shortcuts guide were not reviewed.
