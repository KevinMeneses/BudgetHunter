# Plan: automatic entry categorization (app)

Companion plan in the backend repo: `BudgetHunterBackend/docs/plans/auto-categorization.md`
(read its "Client contract" first). The backend does the categorizing with Gemini Flash-Lite; the
app's job is to say *when* a category should be automatic, and to show/keep the result.
Each part is one small PR; ship them in order.

## Goal

When the user does not pick a category for an entry, the app leaves it to the backend. The entry
shows up as "Other" immediately (offline-first, no waiting) and updates itself once the
server-side categorization arrives through the existing sync/SSE pull.

## Current state (relevant bits)

- `BudgetEntry.category` is non-null, default `Category.OTHER`; the DB column is
  `category TEXT NOT NULL` (`BudgetEntry.sq`), so the app cannot tell "user chose Other" from
  "user did not choose".
- `CreateBudgetEntryRequest` / `UpdateBudgetEntryRequest` always send `category: String`.
- `BudgetEntrySyncManager.mergeServerEntry` / `updateLocalEntryFromResponse` already copy the
  server's category onto synced entries, and SSE events already trigger a pull, so the AI result
  will flow back without new transport code.
- Entries created from SMS and from receipt images (`CreateBudgetEntryFromImageUseCase`, which
  already gets a category from the app-side Gemini call) are other sources of entries.
- Categories (11): `FOOD, GROCERIES, SELF_CARE, TRANSPORTATION, HOUSEHOLD_ITEMS, SERVICES,
  EDUCATION, HEALTH, LEISURE, TAXES, OTHER`. This list is the contract with the backend; do not
  rename values without a coordinated change.

## Parts

### Part 1 - Track where a category came from (local data)
- SqlDelight migration `3.sqm`: add `category_source TEXT NOT NULL DEFAULT 'USER'` to
  `budget_entry` (existing entries count as user-chosen). Update `BudgetEntry.sq` insert/update
  statements, mapper, and `BudgetEntryLocalDataSource`.
- Domain: `BudgetEntry.categorySource: CategorySource { USER, AUTO }`, default `AUTO` for new
  entries until the user picks one.
- Form behavior: picking a category in `BudgetEntryForm` sets `USER`; editing the description of
  an `AUTO` entry keeps it `AUTO`. A user who explicitly picks "Other" stays `USER`.
- Tests: mapper round-trip, migration test (existing rows become `USER`), form intent tests.

### Part 2 - Network contract
- `CreateBudgetEntryRequest` / `UpdateBudgetEntryRequest`: `category: String?` - send `null` when
  `categorySource == AUTO`, the category name when `USER`.
- `BudgetEntryResponse`: add `categorySource: String? = null` (tolerate older servers that do not
  send it); map to the domain in `mergeServerEntry` and `updateLocalEntryFromResponse`.
- **Deploy order:** backend Part 1 must be live before this ships, otherwise the old server rejects
  a missing category with 400. Gate with a `BuildConfig`/remote-config flag if releases may be
  out of order.
- Server-wins rule stays: for synced entries the server's category overwrites the local one; an
  unsynced local entry keeps its own category (already how `mergeServerEntry` treats dates).
- Tests: serialization with null category, `SyncFlowIntegrationTest` case where the server returns
  an AI category after create.

### Part 3 - UI
- Entry form: category selector gets an "Automatic" state as the default for new entries (shows
  "Automatic" until the server answers); choosing any concrete category turns it into a manual
  choice. Entry rows/detail: small "auto" indicator when `categorySource == AUTO`, so users can
  tell and correct it.
- Entry list updates reactively when the sync pull changes the category (verify the existing Flow
  re-emits).
- Metrics (`GetTotalsPerCategoryUseCase`): no change needed; entries pending categorization count
  as `OTHER` until updated. Compose preview/UI tests for the new states.
- Strings in all supported languages.

### Part 4 - Other entry sources
- SMS-created entries: description comes from the bank message; create as `AUTO` with category
  `OTHER` so the backend categorizes them when synced.
- Receipt images: keep the app-side Gemini category as `USER`-equivalent? Recommendation: mark it
  `AUTO` too (it is a model guess), but send it as the category so no second AI call is made - to
  support that, extend the contract later with `categorySource` in requests. Decide with the
  backend plan's open questions; skipping this part is acceptable for the first release.
- Entries created offline get categorized the next time they sync; nothing extra needed.

### Part 5 - Cleanup and docs
- Update `CLAUDE.md` (it still describes the pre-KMP layout) and `README.md` with the
  auto-categorization behavior.
- Check iOS source set compiles (`commonMain` changes only) and `./gradlew ktlint test` pass.

## Acceptance criteria
- Creating an entry without choosing a category works offline, shows "Other"/"Automatic", and
  after sync the category changes to the AI result without user action.
- An entry whose category the user picked is never changed by the server's AI.
- Older entries and older server responses (no `categorySource`) keep working.
- `./gradlew ktlint test` passes.

## Open questions
- Is an explicit "re-categorize" action wanted on an entry (send `category = null` on update)?
- Should receipt/SMS entries trust the app-side category (Part 4)?
