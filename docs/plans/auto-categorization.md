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
  `category TEXT NOT NULL`, so the app cannot tell "user chose Other" from "user did not choose". A new
  explicit **"Sin categoría"** value (`UNCATEGORIZED`, below) fixes that.
- `BudgetEntry.getCategories()` is used by four things that want different lists: the form's selector
  (`BudgetEntryComponents`), the metrics totals (`GetTotalsPerCategoryUseCase`), the receipt prompt
  (`CreateBudgetEntryFromImageUseCase.categoryGuide`) and, through `Category.entries`, the receipt response
  schema in `GeminiApiClient`. The last two must never offer "Sin categoría" to the AI.
  This feature needs **no database migration**: the category column is `TEXT AS Category`, and a new enum value
  is stored by name.
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

### Part 1 - An explicit "Sin categoría" (local data and UI of the selector)
There is **no "who chose it" marker** (an earlier revision had a `categorySource` USER/AUTO column; it was
dropped as redundant). Two facts are enough: the entry has no category, and AI processing is on. Whatever
already has a category, whoever chose it and "Otros" included, is never touched.
- **New `Category.UNCATEGORIZED`** (label "Sin categoría" / "Uncategorized"; real strings in `values` and
  `values-es`). It is the default of `BudgetEntry.category` and what the form selects when the user selects
  nothing. It is the absence of a category, not a category: "Otros" (`OTHER`) stays a real choice, and it is
  also what the AI answers when nothing fits (that entry is then settled). The backend stores `UNCATEGORIZED`
  as is (see its plan for why not `null`: older app builds would crash on a null `category`).
- "Waiting" = `category == UNCATEGORIZED`. It does not depend on the AI toggle, which is what lets entries saved
  while it was off be offered once it is on.
- Split the lists: keep `getCategories()` for the form selector (with "Sin categoría" first) and the metrics
  totals; add `getAssignableCategories()` (the original 11) and use it in the receipt prompt and as the
  enum of the receipt response schema. Add the colour for the new slice in `ChartColors`.
- Form: "Sin categoría" is the first option and the default for new entries. Picking it explicitly is the same
  as picking nothing; picking any real category, "Otros" included, is a choice.
- Existing rows keep what they have (`OTHER`): there is no way to tell whether that "Other" was a choice. The
  mapper's fallback for unknown values stays `OTHER`, which is also what an older app shows for
  `UNCATEGORIZED`.
- Tests: mapper round-trip of the new value, selector lists (assignable list excludes it), form default.

### Part 2 - Network contract
- `CreateBudgetEntryRequest` / `UpdateBudgetEntryRequest`: **no shape change**, `category: String` stays.
  The app sends `UNCATEGORIZED` for a waiting entry (the server treats it as "not chosen") and the category
  name otherwise. No `null`, so nothing here depends on the backend accepting a missing field.
- `BudgetEntryResponse`: no change; `mergeServerEntry` and `updateLocalEntryFromResponse` already copy the
  server's category, `UNCATEGORIZED` included.
- New call in `BudgetEntryApiService` + `ApiEndpoints`: `POST /api/budgets/{id}/entries/categorize`
  -> `CategorizeEntriesResponse(categorized: Int, pending: Long)`. Failures to handle: 403 (the account's
  AI preference is off or not saved yet, or no access), 409 (a run is already in flight), network errors.
- **Deploy order:** the backend that understands `UNCATEGORIZED` (its Parts 1 and 4) must be live before
  this ships; an older server would store the string as if it were the user's choice.
- Server wins for synced entries; an unsynced local entry keeps its own category.
- Tests: serialization of `UNCATEGORIZED` and of the new response; `SyncFlowIntegrationTest` case where
  a pull brings categories chosen by the server.

### Part 3 - Metrics screen: ask, then categorize
This is where the feature becomes visible.
- **When the dialog appears:** the user opens the metrics screen **and** AI processing is on **and** the
  budget is synced to the server (has a `serverId`) **and** offline mode is off **and** at least one entry
  of the budget is waiting (`UNCATEGORIZED`). Never otherwise.
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
- Entry form: with the toggle on, an entry left on "Sin categoría" is categorized later from the metrics
  screen; with it off, the user who turns it on later is offered the entry then.
- Entry rows/detail: "Sin categoría" shown as such. A category assigned automatically looks like any other;
  the user corrects it by editing the entry.
- Update the toggle copy (`ai_processing_description` and its `values-es` version): it also lets the
  server categorize entries from their description, on request, and says only the description is sent.
- Metrics totals (`GetTotalsPerCategoryUseCase`): "Sin categoría" appears as its own slice, separate from
  "Otros", which is exactly what the dialog offers to fix.
- Tests: ViewModel (dialog conditions, confirm flow order, each error), compose previews for the dialog
  and the loading state; strings in `values` and `values-es`.

### Part 4 - Other entry sources
- **Receipts:** the category the receipt AI returns (one of the assignable 11, never "Sin categoría") is sent
  as a normal category, so the server never touches it and a good receipt-based category is not overwritten by
  a description-only guess. If the user declines AI autofill in the confirmation dialog, the entry keeps
  whatever category it had.
- **SMS:** description comes from the bank message; create with category `UNCATEGORIZED`, so the metrics
  screen can offer to categorize it (the main beneficiary of the feature), whatever the toggle said when it
  arrived.
- Entries created offline are categorized the same way once they have synced.

### Part 5 - Cleanup and docs
- Update `CLAUDE.md` (still describes the pre-KMP layout) and `README.md` with the behavior.
- Check the iOS source set still compiles (`commonMain` changes only); `./gradlew ktlint test` pass.

## Acceptance criteria
- Creating an entry without choosing a category works offline, is stored `UNCATEGORIZED`, and nothing is
  sent anywhere at that moment.
- Opening the metrics screen with AI processing on and waiting entries offers to categorize them; confirming
  categorizes them and updates the chart; declining changes nothing.
- An entry that has a category (picked by the user, from a receipt or already assigned), "Otros" included, is
  never changed by the server's AI,
  and a second run does not redo the first.
- With the toggle off, nothing is offered and nothing is classified; entries saved meanwhile are offered once it is turned on.
- Older entries, older servers and older app builds (which show `UNCATEGORIZED` as "Otros") keep working.
- `./gradlew ktlint test` passes.

## Open questions
- How often to ask: every time the metrics screen opens while entries are waiting (simple, nags), once per
  app session, or until the waiting count grows after a "Not now" (recommended)?
- Entries that existed before this feature keep their "Other" (it may be a default or a choice; there is no
  way to tell), so they are not offered. Offer them through a separate "treat my Other
  entries as uncategorized" action, or leave them?
- Is an explicit "categorize this entry again" action wanted on a single entry?
