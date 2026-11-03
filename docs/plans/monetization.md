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
2. **Purchases.** Backend: `POST /api/webhooks/revenuecat` (shared-secret header) sets `users.plan` from
   `INITIAL_PURCHASE` / `RENEWAL` / `UNCANCELLATION` / `PRODUCT_CHANGE` (premium) and `EXPIRATION` (free);
   RevenueCat's app user id = the account email (or a stable id). App: `purchases-kmp`, log in to RevenueCat
   with the same id after sign-in, offerings, restore purchases.
3. **Paywall.** A screen opened from the 402 message and from settings, showing what premium includes, the
   prices from RevenueCat and the scans left (`GET /api/receipts/usage`). Show "N scans left" when few remain.
4. **Categorization behind the plan.** Count categorized entries per month like receipts (free: a small number),
   402 past it; the metrics dialog then offers the paywall.
5. **Shared budgets.** Adding a collaborator requires premium (being invited stays free).
6. **Export** (CSV/PDF) as a premium feature.
7. **Receipt backup in the cloud (premium), after parts 2-3.** Not part of the scan request (the entry may not
   exist yet, and many receipts never go through the AI; the scan endpoint stores nothing and should stay that
   way). Upload when the entry syncs: `PUT /api/budgets/{id}/entries/{entryId}/receipt`, into object storage
   (DigitalOcean Spaces / S3), served with short-lived signed URLs. Compress photos on the phone (~200-400 KB);
   PDFs as-is up to 12 MB. Delete with the entry and the account. Collaborators of a shared budget see it.
   After a reinstall, download on demand when an entry is opened. Free accounts keep receipts on the phone only.

## Before release

- Deploy the backend (migration V4, the Nginx `client_max_body_size` for `/api/receipts/extract`) before the
  app build that reads receipts through it.
- Use a paid Gemini key: the whole receipt image is sent, and the free tier may use submitted content.
- Update the privacy policy: receipts are processed by the server and Google's Gemini API.
- Google Play: SMS permissions need the Permissions Declaration Form (SMS-based money management).
