# Plan: automatic entry categorization (app)

Companion plan in the backend repo: `BudgetHunterBackend/docs/plans/auto-categorization.md`
(read its "How the pieces fit" first). The backend does the categorizing with Gemini Flash-Lite;
the app's job is to say *when* a category should be automatic, and to show/keep the result.
Each part is one small PR; ship them in order. Default branch of this repo is `master`.

_Last revised against `master` at `a3409f3` (account-synced preferences, reworked receipt AI,
English default locale)._

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
  `updateLocalEntryFromResponse` on push), and SSE events still trigger a pull, so the AI result
  flows back with no new transport code.
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
  **only if AI processing is on**, otherwise `USER`.
- Form: picking a category sets `USER`; editing the description of an `AUTO` entry keeps it `AUTO`.
  Explicitly picking "Other" is `USER`.
- Tests: mapper round-trip, migration test (existing rows become `USER`), form intent tests.

### Part 2 - Network contract
- `CreateBudgetEntryRequest` / `UpdateBudgetEntryRequest`: `category: String?` - `null` when
  `categorySource == AUTO`, the category name when `USER`.
- `BudgetEntryResponse`: add `categorySource: String? = null` (tolerate older servers); map it in
  `mergeServerEntry` and `updateLocalEntryFromResponse`.
- **Deploy order:** backend Part 1 must be live before this ships, or the old server answers 400 to
  a missing category. Gate it if releases can go out of order.
- Server wins for synced entries; an unsynced local entry keeps its own category.
- Stale flag guard: before pushing pending entries, make sure the account has the current toggle
  value. Smallest change: when the toggle is on and entries are about to be pushed with
  `category = null`, call `syncUserPreferences.push()` first (it is idempotent), and retry a failed
  push on resume the same way pending entry pushes already retry. Skip if too invasive: the server
  just won't classify until the flag is `true`, and the entries stay `OTHER`.
- Tests: serialization with null category; `SyncFlowIntegrationTest` case where the server returns
  an AI category after create; null category only sent when the toggle is on.

### Part 3 - UI
- Entry form: when AI processing is on, the category selector gets an "Automatic" state as the
  default for new entries (shows "Automatic" until the server answers); picking a concrete category
  makes it manual. When AI is off, the option is hidden and the default is a normal choice.
- Entry rows/detail: a small "auto" indicator when `categorySource == AUTO`, so users can tell and
  correct it. The list updates when the pull changes the category (verify the Flow re-emits).
- Update the toggle copy (`ai_processing_description` and its `values-es` version): it now also lets
  the server categorize entries from their description, and says only the description is sent.
- Metrics (`GetTotalsPerCategoryUseCase`): no change; pending entries count as `OTHER` until updated.
- Compose preview/UI tests for the new states; strings in `values` and `values-es`.

### Part 4 - Other entry sources
- **Receipts:** the category the receipt AI returns is sent explicitly and marked `USER` (not
  `AUTO`), so the server does not classify again and a good receipt-based category is not
  overwritten by a description-only guess. If the user declines AI autofill in the confirmation
  dialog, the entry keeps whatever category it had.
- **SMS:** description comes from the bank message; create as `AUTO` with category `OTHER` when the
  toggle is on, so the backend categorizes it on sync (this is the main beneficiary of the feature).
- Entries created offline get categorized the next time they sync; nothing extra is needed.

### Part 5 - Cleanup and docs
- Update `CLAUDE.md` (still describes the pre-KMP layout) and `README.md` with the behavior.
- Check the iOS source set still compiles (`commonMain` changes only); `./gradlew ktlint test` pass.

## Acceptance criteria
- Creating an entry without choosing a category works offline, shows "Other"/"Automatic", and after
  sync the category changes to the AI result without user action.
- An entry whose category the user picked, or that came from a receipt, is never changed by the
  server's AI.
- With the toggle off, entries are saved as before and nothing is classified server-side.
- Older entries and older server responses (no `categorySource`) keep working.
- `./gradlew ktlint test` passes.

## Open questions
- Is an explicit "re-categorize" action wanted on an entry (send `category = null` on update)?
- Should turning the toggle on offer to categorize existing `OTHER` entries?
