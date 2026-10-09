# Plan: automatic entry categorization (app)

Companion plan in the backend repo: `BudgetHunterBackend/docs/plans/auto-categorization.md`
(read its "How the pieces fit" first). The backend categorizes with rules, a cache and Gemini
Flash-Lite, **on demand**; the app's job is to save entries as "waiting for a category", to ask for the
categorization at the right moment (the metrics screen) and to show the result. Each part is one small
PR; ship them in order. Default branch of this repo is `master`.

_Design change:_ an earlier revision categorized every entry in the background and refreshed the app over
SSE. Dropped: people rarely look at an entry's category unless they open it or open the metrics screen, so
the category only has to be right when they get there, and asking at that moment needs no live updates, no
SSE handling and no notification changes.

_Last revised against `master` at `a3409f3`._

## What exists today (and changes this plan)

- **The AI toggle already syncs with the account.** `SettingsIntent.ToggleAiProcessing` writes
  `PreferencesManager` (`ai_processing_enabled`, default on) and calls
  `SyncUserPreferencesUseCase.push()`; `pull()` runs when settings open and after sign-in. The
  backend stores it in `users.ai_processing_enabled` via `GET/PUT /api/users/me/preferences`.
  So this plan **adds no new toggle plumbing** (the previous revision's Part 2b is gone). The same
  flag now means "on-device receipt AI **and** server-side categorization"; only the wording
  changes.
- Gaps in that sync that matter here: `push()` is best effort (failures are only logged, no retry),
  and `PreferencesManager.clearUserPreferences()` on sign-out drops the local value, which then
  reads as on (`!= false`) until the next `pull()` adopts the account's. The server is the safety
  net (it re-reads the flag before classifying), but see Part 2.
- **Receipt AI was reworked:** `CreateBudgetEntryFromImageUseCase` now uses structured output, a
  per-category guide (`categoryHints`), failure reasons, retries, a 30s deadline and native PDF
  upload; `BudgetEntryViewModel` asks for confirmation before overwriting an amount/description the
  user already typed. The receipt flow already yields a category, so it must not trigger a second
  server-side classification.
- `BudgetEntry.category` is still non-null, default `Category.OTHER`; the column is
  `category TEXT NOT NULL`, so the app cannot tell "user chose Other" from "user did not choose".
  SQLDelight migrations are still `1.sqm`, `2.sqm`, so the next one is `3.sqm`.
- `CreateBudgetEntryRequest` / `UpdateBudgetEntryRequest` still always send `category: String`.
- `BudgetEntrySyncManager` still copies the server's category onto synced entries (merge on pull,
  `updateLocalEntryFromResponse` on push), and `pullEntriesFromServer` brings the server's categories
  back, so after an on-demand run one pull is all the app needs (no SSE involved).
- SMS-created entries never set a category (they get the default `OTHER`).
- Categories (11): `FOOD, GROCERIES, SELF_CARE, TRANSPORTATION, HOUSEHOLD_ITEMS, SERVICES,
  EDUCATION, HEALTH, LEISURE, TAXES, OTHER`. This is the contract with the backend. The one-line
  meanings in `categoryHints` are reused by the server prompt; if you change them, change both.
- The app is now **English by default with Spanish in `values-es`**: new strings go in both.
- The backend never receives the invoice file, so server-side processing is categorization from
  the description only.

## Parts

### Part 1 - Track where a category came from (local data)
- SqlDelight migration `3.sqm`: add `category_source TEXT NOT NULL DEFAULT 'USER'` to
  `budget_entry` (existing entries count as user-chosen). Update `BudgetEntry.sq`, the mapper and
  `BudgetEntryLocalDataSource`.
- Domain: `BudgetEntry.categorySource: CategorySource { USER, AUTO }`. New entries start `AUTO`
  **whatever the AI toggle says**: `AUTO` records that the user has not chosen a category (a default is not
  a choice), not that the AI may act on it. That is what lets entries saved while the toggle was off be offered
  once it is on. "Waiting" = `AUTO` and `Category.OTHER`.
- Form: picking a category sets `USER`; editing the description of an `AUTO` entry keeps it `AUTO`.
  Explicitly picking "Other" is `USER`.
- Tests: mapper round-trip, migration test (existing rows become `USER`), form intent tests.

### Part 2 - Network contract
- `CreateBudgetEntryRequest` / `UpdateBudgetEntryRequest`: `category: String?` - `null` when
  `categorySource == AUTO`, the category name when `USER`.
- `BudgetEntryResponse`: add `categorySource: String? = null` (tolerate older servers); map it in
  `mergeServerEntry` and `updateLocalEntryFromResponse`.
- New call in `BudgetEntryApiService` + `ApiEndpoints`: `POST /api/budgets/{id}/entries/categorize`
  -> `CategorizeEntriesResponse(categorized: Int, pending: Long)`. Failures to handle: 403 (the account's
  AI preference is off or not saved yet, or no access), 409 (a run is already in flight), network errors.
- **Deploy order:** backend Part 1 (optional category) and Part 4 (endpoint) must be live before this ships,
  or the old server answers 400 to a missing category.
- Server wins for synced entries; an unsynced local entry keeps its own category.
- Tests: serialization with null category and with the new response; `SyncFlowIntegrationTest` case where
  a pull brings categories chosen by the server.

### Part 3 - Metrics screen: ask, then categorize
This is where the feature becomes visible.
- **When the dialog appears:** the user opens the metrics screen **and** AI processing is on **and** the
  budget is synced to the server (has a `serverId`) **and** offline mode is off **and** at least one entry
  of the budget is waiting (`AUTO` + `OTHER`). Never otherwise.
- **Dialog:** "Categorize your entries automatically?" Says how many entries, and that only their
  descriptions are sent to be categorized (no amounts, no names). Confirm / Not now.
- **On confirm** (a new intent in `BudgetMetricsViewModel`, with its own use case):
  1. push the budget's unsynced entries first (`syncPendingEntries`), because the server can only
     categorize what it has, and push the preferences if needed so the server's AI flag is current;
  2. call the endpoint, with a loading state (it works before it answers, seconds for a big budget);
  3. pull the budget's entries (`pullEntriesFromServer`) and recompute the totals, so the chart updates;
  4. show the result: "N entries categorized" and, if `pending > 0`, that some could not be placed or can
     be retried later.
  Errors: 403 -> push the preferences once and retry; if still 403, say AI processing is off for the
  account. 409 -> "already running". Network -> keep the dialog's offer available, nothing is lost.
- **Not nagging:** "Not now" suppresses the dialog for this budget until the number of waiting entries
  grows, or the app restarts (decide: see open questions).
- Entry form: when AI processing is on, the category selector gets an "Automatic" state as the default for
  new entries (it is categorized later, from the metrics screen); picking a concrete category makes it
  manual. When AI is off the selector looks and behaves as today (default "Other"), but an entry saved
  without touching it is still stored `AUTO`/waiting, so a user who turns the toggle on later is offered it.
  Only an explicit pick, "Other" included, makes it `USER`.
- Entry rows/detail: optional small "auto" indicator when `categorySource == AUTO` and the category is not
  `OTHER`, so users can tell it was automatic and correct it.
- Update the toggle copy (`ai_processing_description` and its `values-es` version): it also lets the
  server categorize entries from their description, on request, and says only the description is sent.
- Metrics totals (`GetTotalsPerCategoryUseCase`): no change; waiting entries count as `OTHER` until
  categorized, which is exactly what the dialog offers to fix.
- Tests: ViewModel (dialog conditions, confirm flow order, each error), compose previews for the dialog
  and the loading state; strings in `values` and `values-es`.

### Part 4 - Other entry sources
- **Receipts:** the category the receipt AI returns is sent explicitly and marked `USER` (not `AUTO`), so
  the server never touches it and a good receipt-based category is not overwritten by a description-only
  guess. If the user declines AI autofill in the confirmation dialog, the entry keeps whatever category it
  had.
- **SMS:** description comes from the bank message; create as `AUTO` with category `OTHER`, so the metrics
  screen can offer to categorize it (the main beneficiary of the feature), whatever the toggle said when it
  arrived.
- Entries created offline are categorized the same way once they have synced.

### Part 5 - Cleanup and docs
- Update `CLAUDE.md` (still describes the pre-KMP layout) and `README.md` with the behavior.
- Check the iOS source set still compiles (`commonMain` changes only); `./gradlew ktlint test` pass.

## Acceptance criteria
- Creating an entry without choosing a category works offline, is stored `AUTO`/`OTHER`, and nothing is
  sent anywhere at that moment.
- Opening the metrics screen with AI processing on and waiting entries offers to categorize them; confirming
  categorizes them and updates the chart; declining changes nothing.
- An entry whose category the user picked, or that came from a receipt, is never changed by the server's AI,
  and a second run does not redo the first.
- With the toggle off, nothing is offered and nothing is classified; entries saved meanwhile are offered once it is turned on.
- Older entries and older server responses (no `categorySource`) keep working.
- `./gradlew ktlint test` passes.

## Open questions
- How often to ask: every time the metrics screen opens while entries are waiting (simple, nags), once per
  app session, or until the waiting count grows after a "Not now" (recommended)?
- Entries that existed before this feature were migrated to `USER` (their "Other" may be a default or a
  choice; there is no way to tell), so they are not offered. Offer them through a separate "treat my Other
  entries as waiting" action, or leave them?
- Is an explicit "categorize this entry again" action wanted on a single entry?
