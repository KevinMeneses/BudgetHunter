# Plan: monetization (freemium with RevenueCat)

Companion changes in the backend repo: `receipts/` package, `users.plan`, `ai_usage` (see its CLAUDE.md,
"Receipts and plans"). Each part is one small PR per repo; ship the backend first.

## Decisions

- **Model:** freemium subscription. RevenueCat for Google Play Billing and StoreKit (KMP SDK
  `purchases-kmp`), monthly and yearly. A lifetime option may come later, priced at 3-4x the yearly plan.
- **What is free:** everything manual, SMS reading (it runs on the phone, costs nothing and is what sets the
  app apart), metrics, and a monthly allowance of AI receipt scans so people see it work.
- **What is paid:** AI without the free cap (receipts now, categorization next), creating shared budgets,
  export. Limiting the number of budgets is not a lever (most people have one or two).
- **Starting prices (Colombia):** COP 9.900/month, COP 59.900/year, 7-day trial. Use the stores' regional
  price tiers elsewhere.
- **The server decides.** The plan and the monthly counters live on the backend; the app only shows them.
  A check that lives only in the app is bypassed by editing the app.
- **No AI key in the app.** It could be pulled out of the APK/IPA and billed to us; all AI goes through the
  backend.

## Parts

1. **Receipts through the server + monthly scans (done).**
   Backend: `POST /api/receipts/extract`, `GET /api/receipts/usage`, `users.plan` (FREE/PREMIUM),
   `ai_usage` per UTC month, 402 `AI_QUOTA_EXCEEDED`. Free 10, premium 200 (fair use), both env settings.
   App: `ReceiptApiService` replaces `GeminiApiClient`; `GEMINI_API_KEY` removed from Gradle, Info.plist and CI;
   new failure reasons `QUOTA_EXCEEDED` and `SIGN_IN_REQUIRED`.
   A scan that comes back "not a receipt" counts; a failure on the AI's side is given back.
2. **Purchases.** Backend (done): `users.billing_id` (random) is the RevenueCat app user id;
   `POST /api/webhooks/revenuecat` (shared-secret header) only names the accounts, and the server reads each one's
   entitlement from RevenueCat's API into `users.plan` / `plan_expires_at`; `GET /api/billing/status`,
   `POST /api/billing/sync`. App: RevenueCat's Android SDK behind `StoreBilling` (not `purchases-kmp`: on iOS it
   needs RevenueCat's framework linked in Xcode, and iOS launches later), `PremiumRepository` (log in with the
   backend's id before each store call, sync after buying or restoring), store log out on sign out. Deleting
   the account runs the same sign out; it never cancels the store subscription, so its confirmation warns
   premium accounts and links to the store's subscriptions page.
3. **Paywall (done).** `PremiumScreen`, opened from the 402 message ("See Premium") and from settings, showing what
   premium includes, the prices from RevenueCat and this month's AI usage (`GET /api/ai/usage`). Still open: a
   "N scans left" hint on the entry screen when few remain.
4. **Categorization behind the plan (done).** FREE 50, PREMIUM 1000 descriptions sent to the AI per month; rules and
   cache are free. Past the limit there is no 402: the run categorizes what it can and answers `quotaExceeded` and
   `usage`; the metrics screen then says so with a "See Premium" action.
5. **Shared budgets.** Adding a collaborator requires premium (being invited stays free).
6. **Export** (CSV/PDF) as a premium feature.
7. **Receipt backup in the cloud (premium), after parts 2-3.** Not part of the scan request (the entry may not
   exist yet, and many receipts never go through the AI; the scan endpoint stores nothing and should stay that
   way). Upload when the entry syncs: `PUT /api/budgets/{id}/entries/{entryId}/receipt`, into object storage
   (DigitalOcean Spaces / S3), served with short-lived signed URLs. Compress photos on the phone (~200-400 KB);
   PDFs as-is up to 12 MB. Delete with the entry and the account. Collaborators of a shared budget see it.
   After a reinstall, download on demand when an entry is opened. Free accounts keep receipts on the phone only.

## Store setup (Android)

1. RevenueCat: project, Android app (package name + Google service account JSON), entitlement `premium`.
2. Upload a build that contains this SDK (it brings Google Play Billing) to the Play Console's internal testing
   track: Google does not allow creating subscriptions before that.
3. Play Console: subscription `premium` with monthly and yearly base plans and the 7-day trial; license testers.
4. RevenueCat: import the products, attach them to `premium`, offering `default` with the monthly and annual
   packages. Put the public `goog_` key in `local.properties` / the `REVENUECAT_ANDROID_API_KEY` secret.
5. Backend: `REVENUECAT_SECRET_API_KEY`, `REVENUECAT_WEBHOOK_AUTHORIZATION`, the webhook URL (see its DEPLOYMENT.md).

Changing the package name later means a new app in Play Console and in RevenueCat (the products are per app),
a new Android OAuth client for Google sign-in, and a new `goog_` key; no code here depends on it.

## Before release

- Deploy the backend (migration V4, the Nginx `client_max_body_size` for `/api/receipts/extract`) before the
  app build that reads receipts through it.
- Use a paid Gemini key: the whole receipt image is sent, and the free tier may use submitted content.
- Update the privacy policy: receipts are processed by the server and Google's Gemini API.
- Google Play: SMS permissions need the Permissions Declaration Form (SMS-based money management).
