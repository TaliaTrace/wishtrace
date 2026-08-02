# WishTrace Execution Board

Update this file during the hackathon. Do not manage the project from memory.

## Current gates

| Gate | Evidence required | Status | Owner | Last checked |
|---|---|---|---|---|
| Official-window baseline frozen | secret scan + preexisting commit | PASS — `283f5be` | Codex | 2026-08-01 14:29 PKT |
| Supabase TLS + migration | client TLS true + Alembic head | PASS — TLS, PostgreSQL 17.6, `20260802_0012` | Codex | 2026-08-02 23:39 PKT |
| Backend foundation | pytest + Ruff + mypy + health/UCP tests | PASS PUBLICLY — 143 tests; healthy Azure revision `savedcard1`; database TLS true | Codex | 2026-08-03 00:11 PKT |
| Google authentication | nonce-bound exchange + real physical-phone account | PASS PUBLICLY — real session reopened empty Home through Azure | Codex/user | 2026-08-02 00:36 PKT |
| Recipient persistence | owned create + close/reopen recovery | PASS PUBLICLY — user-created context survived a physical-phone force-stop/relaunch | Codex/user | 2026-08-02 01:04 PKT |
| Prava auth works | smallest official sandbox request | PASS — authenticated `NOT_FOUND`, response ID recorded | Codex/user | 2026-08-01 |
| Prava transaction path understood | session + authoritative status | LIVE USER ACTION — saved enrollment failed device binding and was retired; first clean team-card enrollment returned `PROVISION_ERROR`; one explicit no-preselection retry is pending with zero charges | Codex/user | 2026-08-03 00:25 PKT |
| Primary merchant validated | search/product/quote/checkout facts | PARTIAL PASS — real Jackbox $5 SKU + runtime quote + card form; Prava attempt pending | Codex | 2026-08-01 22:40 PKT |
| Backup merchant validated | documented fallback | NONE ENABLED — honest unavailable if Jackbox policy/region fails | Codex | 2026-08-01 22:34 PKT |
| OpenAI structured output works | valid live candidate IDs returned | PASS PUBLICLY — validated Azure decision returned the eligible live Jackbox candidate on the phone | Codex/user | 2026-08-02 01:05 PKT |
| Android Compose foundation builds | debug APK + unit tests + lint | PASS — authenticated network runtime; latest APK installed without crash | Codex | 2026-08-02 23:46 PKT |
| Android onboarding + home build | routed screens + state handling + build evidence | PASS — UI MILESTONE 2 | Codex | 2026-07-30 17:36 PKT |
| Android recipient context build | editable recipient + occasion + validation + tests | PASS — UI MILESTONE 3 | Codex | 2026-07-30 19:02 PKT |
| Android grounded decision build | discovery + recommendation + message + recovery | PASS — public live discovery/ranking and owned message API wired | Codex | 2026-08-02 00:36 PKT |
| Android device UX evidence | API 31 route tests + screenshots + text scale + contrast | PASS — UI MILESTONE 3 | Codex | 2026-07-30 19:02 PKT |
| Android return flow works | approval → app → reconciled state | PARTIAL — physical app-link reconciliation rendered the authoritative hosted failure; automatic Prava redirect still pending | Codex/user | 2026-08-02 22:34 PKT |
| Bronze flow repeats twice | full device demo | NOT STARTED | | |
| Five-second UX understood | fresh viewer explanation | NOT STARTED | | |
| Clean build works | fresh checkout command log | NOT STARTED | | |
| Submission uploaded | links verified externally | NOT STARTED | | |

## Now

1. User completes the final explicit no-preselection hosted approval with the organizer-issued team card; Codex performs no phone inspection, Chrome automation or card entry.
2. Reconcile the mandate once and, only if it is `ACTIVE`, execute exactly one tokenized merchant attempt.
3. If Prava returns another provisioning/device failure, retain the safe evidence and escalate without another blind retry.

## Next

1. Run hosted Prava approval and the organizer-required real-merchant browser attempt.
2. Repeat the physical-phone path once after the first evidence run without a duplicate action.
3. Apply for production access only with the captured sandbox evidence.

## Milestone evidence

### 2026-08-03 — Reuse the enrolled card and expire abandoned approval sessions

- The production adapter now reads the official customer-scoped active-card list and supplies
  `card.card_id` only when one active default enrollment, or one unambiguous active enrollment,
  exists. It never chooses between multiple cards and never reads or persists PAN/CVV data.
- The live sandbox returned exactly one active default enrollment for the current WishTrace user.
  A single mandate setup preselected it, but the hosted result was authoritative
  `DEVICE_BINDING_FAILED`, with no mandate, credential or charge. The enrollment was then retired
  through the official `POST /v1/deleteCard` endpoint; Prava confirmed zero active cards remain.
- A clean no-preselection setup then let the user enter the organizer-issued team card through
  Prava's hosted surface. Its result was authoritative `PROVISION_ERROR`, with no saved enrollment,
  mandate, credential or charge. At the user's explicit request, one final audited retry is pending;
  it is not an automatic retry loop.
- A prior untouched pending setup passed its provider expiry. Refresh now converts that state to
  `EXPIRED/SESSION_EXPIRED` instead of leaving the app waiting forever.
- Quality: 143 backend tests, Ruff, strict mypy and Alembic head checks pass. ACR run `chc`
  succeeded; healthy Azure revision `wishtrace-api--savedcard1` serves 100% traffic and `/health`
  reports PostgreSQL 17.6 with TLS true.
- Truth boundary: a real final hosted approval is pending after two safely reconciled provider
  setup failures. No passkey approval, mandate activation,
  one-use credential, merchant attempt, Prava report, Visa confirmation or order is claimed.

### 2026-08-02 — Exact Prava request audit and deployed failure diagnostics

- Applied the organizer-supplied request audit: mandate setup sends explicit `integration_type` and
  `intent`, uses a routable verified email, and restricts merchant URLs to a bare HTTPS origin on a
  delegated-domain-shaped host. No card number or card fragment exists in runtime source.
- The live sandbox accepted the exact request but omitted the documented `authorizeOnly` response
  marker. WishTrace accepts omission only because request intent is explicit and rejects an explicit
  `false`. The line item now carries the observed $5 candidate price rather than the mandate cap.
- Safe setup failure categories are persisted and shown to the user; credentials, provider payloads,
  card data and arbitrary provider messages remain excluded. Migration `20260802_0012` is live over
  verified PostgreSQL TLS.
- Quality: 140 backend tests, Ruff and strict mypy pass. Android assembly, unit tests and lint pass;
  the APK installed on the physical phone without a WishTrace fatal exception.
- Deployment: ACR run `chb` succeeded and healthy Azure revision
  `wishtrace-api--pravaaudit1` serves 100% traffic. `/health` reports PostgreSQL 17.6/TLS true and
  `/.well-known/ucp` remains a direct cacheable 200.
- A single server-side read found the current setup still `pending`, with zero transactions, zero
  one-use credentials and no safe error code. No retry, browser submission or merchant attempt was
  made during this audit.

### 2026-08-02 — Live hosted provisioning failure and recovery

- The first physical-phone card attempt produced authoritative Prava session status `failed` with
  safe provider category `PROVISION_ERROR`. Prava created no mandate, issued no one-use credential,
  and no merchant charge/order occurred. This is not the organizer-accepted merchant decline yet.
- The backend now reconciles a zero-result mandate list against the hosted session's official
  payment-result endpoint. A failed session becomes a retryable `FAILED` mandate; a success-like
  session without a uniquely matching mandate becomes `UNKNOWN`, never armed.
- Android rendered “Autopilot did not arm” and explicitly stated that no mandate or merchant charge
  was created. The hosted Prava page was never dumped or screenshotted.
- Quality: backend pytest 126/126, Ruff and strict mypy pass. Android debug assembly, unit tests and
  lint pass. Commit `6a57af7` is deployed as healthy Azure revision
  `wishtrace-api--prava6a57af7`; public health reports PostgreSQL 17.6 and client TLS true.
- A fresh hosted retry was opened at this checkpoint. Its latest server-side status is recorded in
  the newer request-audit milestone above; approval, mandate activation, one-use credential,
  merchant attempt, Prava charge report, Visa confirmation and order remain unclaimed.

### 2026-08-02 — Gift DNA mandate path and live hosted-session gate

- Android path: Home → live discovery → grounded recommendation → Autopilot is wired on the
  physical RMX3201. The app opens only allowlisted Prava HTTPS hosts and keeps the organizer-issued
  sandbox card entirely inside Prava's hosted collection surface.
- Backend mandate boundary: setup, customer-scoped association, charge, one merchant attempt and
  report are typed against Prava's current camelCase REST contract. Current charge credentials are
  accepted only in `awaiting_result`, remain backend-memory-only, and never enter Android, logs or
  persistence. Ambiguous mandate association and mismatched charge/report IDs fail closed.
- Persistence: Supabase is at `20260801_0011 (head)` over verified client TLS. Migrations add owned
  mandates/charges plus Gift-DNA personality, age and recurrence evidence without changing the
  one-recipient scope.
- Live recovery: an early hosted-session create exposed an ORM timestamp refresh bug; the local row
  rolled back and the unopened external session was left to expire. A later retry exposed Prava's
  safe `DUPLICATE_EXTERNAL_ORDER_REF`; setup now uses the persisted mandate-attempt UUID, stable per
  attempt and unique across recoverable retries.
- Deployment: Azure revision `wishtrace-api--pravafcf597f` is healthy at 100% traffic. Public health
  reports PostgreSQL 17.6 and TLS true. The current physical-phone retry created a fresh Prava
  session and opened its hosted Custom Tab.
- Quality: backend pytest 124/124, Ruff, strict mypy and Alembic drift checks pass. Android debug
  assembly, unit tests and lint pass. No hosted card-page screenshot or UI dump was taken.
- Truth boundary: a real sandbox hosted session exists and is awaiting the user's private card and
  passkey steps. No mandate approval, one-use credential, merchant payment attempt, Prava report,
  Visa confirmation or order is claimed yet.

### 2026-08-02 — Real context persistence and grounded phone decision

- Physical persistence: a signed-in user created one recipient with a user-authorized editable
  sandbox occasion date, Gaming/Fitness interests and a $5 USD budget. Force-stopping and reopening
  the app restored the same server-backed context; no seeded runtime record was used.
- Live discovery: the public Azure API retrieved the observed Jackbox Games $5 digital gift-card
  variant, enforced the exact budget and retained delivery as unconfirmed rather than inventing a
  promise.
- Grounded ranking: the deployed Azure model selected that still-eligible live candidate and the
  Android recommendation rendered its exact merchant, price, source mode and source timestamp.
- Review boundary: selecting the candidate created the owned purchase flow and reached the
  just-in-time billing form. The address is not persisted and no quote, Prava session, credential,
  merchant payment attempt or order has occurred at this checkpoint.
- Private evidence: a physical-phone screenshot was captured only in the local temporary directory;
  it is not tracked because it contains recipient context.

### 2026-08-02 — Public Azure runtime and real Android commerce wiring

- Deployment: built the frozen Python 3.12/Playwright image in Azure Container Registry and deployed
  it to Azure Container Apps in East US 2. A user-assigned managed identity has pull-only access;
  registry admin credentials remain disabled. One healthy revision receives 100% HTTPS traffic.
- Public boundary: `/health` returns `ok` with PostgreSQL 17.6 and client TLS true;
  `/.well-known/ucp` returns `2026-04-08` without redirect and with public cache policy; auth challenge
  and safe unauthorized envelopes pass over the external URL.
- Sandbox activation: code defaults remain fail-closed, while the staging revision enables only the
  exact allowlisted Jackbox checkout and stored-value path for the organizer-required expected-fail
  tokenized-card proof. No hosted session, credential or merchant payment has been initiated yet.
- Android: replaced runtime commerce-unavailable wiring with live discovery, deterministic rejection,
  validated Azure ranking, immutable purchase intent, just-in-time non-persisted billing, idempotent
  quote/session actions, Prava Custom Tab allowlisting, validated app return, backend reconciliation,
  truthful authorization/order result types and owned personal-message persistence.
- Physical proof so far: the public-URL APK installed on the RMX3201 and the existing real Google
  session reopened a genuinely empty authenticated Home without crash. The recipient's real occasion
  date remains required before live discovery can run.
- Recovery proof: a device app-link test with a random nonexistent purchase UUID reached the public
  authenticated endpoint and rendered a safe not-found state; no browser-supplied result was trusted.
- Quality at this checkpoint: backend pytest 97, Ruff, strict mypy and Alembic drift checks pass.
  Android debug assembly, unit tests and lint pass; the latest APK is installed and authenticated.
  The actual hosted sandbox flow remains in the current task.

### 2026-08-01 — Live Azure provider and deployment-readiness hardening

- Tenant-scoped interactive Azure CLI login succeeded against the enabled subscription. Read-only
  inventory found the existing AI Services project/deployment but no App Service plan, Web App,
  Container App or registry.
- The configured `gpt-5.6-terra` deployment returned a completed strict-schema response through the
  real production transport in 3,662 ms. The probe used one opaque candidate ID, `store=false`, no
  recipient hint and no payment data; a safe provider request ID was observed.
- Permanent ignored configuration now validates with `sslmode=require`, a stable session pepper,
  Google authentication and Prava present. Checkout and stored-value flags remain deliberately off.
- Added fail-closed deployed configuration checks for public origin, Google client ID, Azure/Prava
  completeness, Prava origin, stored-value dependency and security/no-cache response headers.
- No Azure hosting resource or spend was created in this milestone.

### 2026-08-01 — Azure browser-runtime packaging

- Added a Python 3.12 container that installs the frozen backend environment plus only the matching
  Playwright Chromium runtime and exposes FastAPI on port 8000.
- The image build context excludes environment files and test artifacts. Checkout and stored-value
  flags remain off, migrations remain deliberate, and no Azure resource or spend has been created.
- Playwright-managed Chromium is now supported when a machine-specific executable path is absent;
  local explicit Chrome paths remain supported.
- Quality: backend pytest 91 passed; Ruff and strict mypy passed; Android debug assembly, unit tests
  and lint passed; Supabase probe reported TLS true and Alembic reported no schema drift.

### 2026-08-01 — Narrow $5 digital commerce and complete backend transaction boundary

- Merchant pivot: the primary path is now the exact Jackbox Games $5 digital card, product
  `6734381809798`, variant `39783705149574`, SKU `GC20221246`. The live cart returned 500 USD minor
  units, `gift_card=true`, and `requires_shipping=false`; checkout rendered the exact total, contact,
  PCI card and billing fields with no shipping address.
- Amazon scope decision: no Amazon scraping or marketplace integration. Official catalog/gift-card
  paths require separate onboarding and would not be production-shaped within the event window.
- Browser boundary: added an exact-host/product/variant allowlist, cart invariant checks, real total
  refresh after billing, 20-minute quote context, single payment click, explicit order/decline/unknown
  observation and immediate browser teardown. Billing, token, dynamic CVV and expiry are never stored,
  logged, screenshotted after entry, or returned to Android. On Windows, a dedicated Proactor loop
  runs Playwright beside psycopg's Selector loop; the production actor passed a live non-payment quote
  at item 500, shipping 0, tax 0 and total 500 USD minor units.
- Transaction orchestration: quote and Prava session creation are idempotent. Reconcile now performs
  one matching merchant attempt, reports `APPROVED` or `DECLINED`, re-polls Prava, persists public Visa
  confirmation plus merchant order evidence, and allows `SUCCEEDED` only when both sides verify. If
  report-status was interrupted after a known merchant result, recovery re-reports that result and
  never performs a second merchant checkout.
- Delivery truth: Jackbox emails the purchaser's verified checkout address; the purchaser manually
  forwards the gift card. Runtime copy does not imply direct recipient delivery or guaranteed timing.
- Personal message: added authenticated, owner-scoped create/edit/read persistence for one 500-character
  message per purchase. Runtime starts with user text; no generated memory or delivery claim is invented.
- Persistence: Supabase advanced through `20260801_0008` and `0009` over verified TLS. PostgreSQL 17.6
  remained healthy and Alembic reports no model/schema drift.
- Quality: backend pytest 91 passed; Ruff and strict mypy passed. Android debug assembly, unit tests
  and lint passed. Live payment remains pending and the
  stored-value flags remain off by default.
- Evidence: `artifacts/backend/jackbox-digital-checkout-probe-2026-08-01.png`,
  `artifacts/backend/jackbox-runtime-quote-2026-08-01.json`, `backend/app/merchant_browser.py`,
  `backend/app/purchase.py`, and transaction/message tests.

### 2026-08-01 — Grounded Azure ranking boundary and immutable decision audit

- Ranking boundary: added authenticated create/read routes that accept only persisted candidates which
  still pass source-mode, checkout, availability, variant, USD and budget checks. With the current
  merchants, every candidate remains rejected for unsupported checkout, so the runtime makes zero
  Azure calls and returns `NO_ELIGIBLE_CANDIDATES`.
- Structured output: the official OpenAI Python client targets the configured Azure `/openai/v1/`
  endpoint with `store=false`, a strict JSON Schema whose enums contain only eligible candidate and
  evidence IDs, no tools, one malformed-output repair, and application validation after parsing.
- Privacy and claims: the provider input omits recipient name, merchant URL, money, availability,
  delivery and payment data. Reasons may describe recipient fit only; commerce claims, unknown IDs,
  rejected candidates, duplicate IDs and unknown evidence are rejected.
- Recovery: provider failure or two invalid outputs use a high-uncertainty deterministic result only
  when saved interest/hint text directly matches a candidate. Otherwise the durable run becomes
  `USER_CHOICE_REQUIRED`; no local product or model-like answer is fabricated.
- Persistence: migration `20260801_0007` added one owned ranking run per discovery, immutable evidence
  snapshots and ordered evidence-linked decision items. Supabase advanced over verified TLS;
  PostgreSQL 17.6 remained healthy and Alembic reports no model/schema drift.
- Provider proof: the SDK wire contract passes an isolated mocked HTTP transport test. A judged-window
  live structured-output probe correctly failed as `MODEL_TIMEOUT` because the configured Foundry
  hostname did not resolve in DNS. This is an external configuration blocker, not a successful model
  call; the earlier pre-kickoff provider smoke is not promoted as current evidence.
- Quality: backend pytest 67 passed; Ruff passed; strict mypy passed. Android debug assembly, unit
  tests and lint also passed. Connected tests were not run to preserve the user's pending phone form.
- Evidence: `backend/app/ranking.py`, `backend/app/openai_ranking.py`,
  `backend/tests/test_ranking.py`, and `backend/tests/test_openai_ranking.py`.

### 2026-08-01 — Idempotent Prava boundary and authoritative purchase ledger

- Prava adapter: implemented the documented hosted `full_checkout` session request, payment-result
  polling and `APPROVED`/`DECLINED` report-status contracts behind typed HTTP boundaries. Hosted URLs
  are HTTPS host-allowlisted, redirects and oversized/non-JSON responses are rejected, and provider
  errors expose only safe codes and response IDs.
- Credential isolation: session tokens are discarded. One-time card token, dynamic CVV and expiry use
  masked in-memory types and are absent from API response models, logs and all four new database
  tables. `awaiting_result` advances only for exactly one credential whose merchant origin and total
  match the reviewed purchase facts.
- Duplicate protection: Prava session creation requires a stable idempotency key; only SHA-256 hashes
  of the key and canonical request are stored. A repeated tap returns the existing hosted session.
  A network timeout, server-side error, redirect or malformed success after the POST becomes
  `UNKNOWN` and refuses a blind retry.
- Truth boundary: a Prava `completed` response cannot produce success without a separately verified
  merchant order. The current live merchants remain checkout-ineligible, so no runtime purchase
  intent or hosted session can be created yet.
- Persistence: migration `20260801_0006` added exact purchase snapshots, Prava public identifiers,
  idempotency operations and immutable state transitions. Supabase advanced from `0005` to `0006`
  over verified TLS; PostgreSQL 17.6 remained healthy and Alembic reports no model/schema drift.
- API: added authenticated purchase-intent create/read, idempotent Prava-session create, reconcile,
  public status and fixed Android return routes. The return route conveys only the purchase-intent ID
  and never trusts a provider status from the browser.
- Quality: backend pytest 52 passed; Ruff passed; strict mypy passed; Android `assembleDebug`, unit
  tests and lint passed. Connected tests were intentionally skipped to preserve the user's current
  unsaved recipient form.
- Evidence: `backend/app/prava.py`, `backend/app/purchase.py`,
  `backend/tests/test_prava.py`, `backend/tests/test_purchase.py`.

### 2026-08-01 — Live UCP discovery and immutable candidate evidence

- Live commerce: the production-shaped adapter negotiated both merchant UCP profiles and supplied the
  public WishTrace platform profile in every MCP call. HyperX returned 10 live gaming-headset products;
  Turtle Beach returned five results for `gift card`.
- Exact facts: HyperX Cloud III Black variant `43656365375645`, SKU `727A8AA`, was observed at
  $64.99 USD and available. Turtle Beach's own $50 digital gift-card variant was observed live.
  All candidates remain rejected as `UNSUPPORTED_CHECKOUT` because no checkout path is yet proven;
  the gift card additionally carries stored-value risk.
- Truth boundary: catalog search/lookup, price and availability are live. Delivery is `UNKNOWN`.
  Quote, tax, shipping, checkout, Prava compatibility and order completion remain unverified.
- Persistence: migration `20260801_0005` created owned discovery runs, immutable candidate snapshots
  and separate deterministic rejection audit records in Supabase over verified TLS.
- API: authenticated `POST /v1/discoveries` and `GET /v1/discoveries/{id}` derive the query from saved
  interests, send no recipient name/hint to merchants, enforce checkout → availability → variant →
  budget → dislike rejection order, and expose only persisted `LIVE` facts.
- Protocol handling: both merchant profiles omitted `Cache-Control`; WishTrace records them as
  noncompliant and does not cache them. Its own public profile returns `public, max-age=300`.
- Checkout correction: the official UCP MCP `create_checkout` call returned `Tool not found` from
  HyperX despite its advertised capability. Profile advertisement never flips runtime eligibility;
  only a verified Prava browser checkout path may do so.
- Quality: backend pytest 33 passed; Ruff passed; strict mypy passed; Alembic reports
  `20260801_0005 (head)`; PostgreSQL 17.6 connection reports TLS true. Android
  `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug` also passed after the slice.
- Evidence: `artifacts/backend/ucp-live-proof-2026-08-01.json` and merchant request IDs in
  `docs/MERCHANT_VALIDATION.md`.


### 2026-08-01 — Real Google authentication and owned context runtime

- Backend auth: one-time five-minute challenges store only a nonce hash; Google ID tokens are
  validated for signature, issuer, audience, expiry and nonce; challenges are consumed atomically.
- Session boundary: Android receives an opaque 24-hour WishTrace token, stores it with Android
  Keystore-backed AES/GCM, and the database stores only its peppered HMAC. Logout, expiry and
  unauthorized recovery are explicit.
- Persistence: migrations `20260801_0002` through `20260801_0004` created owned users, challenges,
  sessions, recipients, preferences, hints and occasions, then enforced one recipient per Gold user.
  Birthdays remain local dates with IANA timezones and money remains integer minor units in USD.
- Android runtime: removed the `Not now` escape hatch, local session ViewModel and seeded runtime
  repositories. Home/People/Occasions use the authenticated backend; discovery now reports commerce
  unavailable rather than manufacturing a candidate.
- Runtime audit: giver/recipient copy now comes from authenticated state, and the production
  `SourceMode` permits only `LIVE`; controlled values remain confined to historical evidence.
- Truth corrections: setup no longer offers an unpersisted photo, and it no longer invents a
  birthday-minus-one delivery deadline. Media stays deferred and delivery remains unknown until a
  real quote requires user-provided billing context; the selected digital SKU requires no shipping.
- Physical evidence: the RMX3201 opened the real Google account chooser and consent surface,
  returned to WishTrace, reached the genuine empty Home state, and created exactly one backend user,
  one active session and one consumed challenge. No raw Google or bearer token was captured.
- Physical UX correction: removed duplicate IME inset handling that collapsed the person form on API
  30. A connected test now verifies the name and relationship fields remain visible above the
  keyboard; it passed 1/1 on the RMX3201.
- Quality: backend pytest 19 passed; Ruff passed; strict mypy passed. Android assemble, unit tests,
  lint and Android-test compilation passed; the debug APK installed successfully on the physical
  phone. Every authenticated route now returns to sign-in after backend session invalidation.
- Evidence: `android/evidence/wishtrace-signin.png`, `wishtrace-after-auth-wait.png`,
  `wishtrace-person-form.png` (failure) and `wishtrace-person-form-current.png` (fixed). Account-chooser
  XML was intentionally destroyed after the proof because it contained account identifiers.
- Truth boundary: OAuth, local API transport and Supabase auth persistence are live. Recipient APIs
  are live but await real user entry on the phone. Public Azure deployment, live merchant facts,
  grounded ranking and Prava transaction behavior remain unverified.

### 2026-08-01 — Judged-window security and backend foundation

- Kickoff: local clock verified after the official opening.
- Baseline: initialized Git after a tracked-file secret scan and committed all disclosed pre-kickoff work as `283f5be`.
- Backend: FastAPI/Pydantic v2, async SQLAlchemy + psycopg 3, Alembic, correlation/error middleware, audit-safe request logging, `/health`, and catalog-only `/.well-known/ucp`.
- Database: application-to-pooler TLS verified through libpq `ssl_in_use`; observed PostgreSQL 17.6; Alembic initially upgraded to `20260801_0001 (head)` using `NullPool`.
- Backend quality at this milestone: pytest 7 passed; Ruff passed; strict mypy passed.
- Android baseline: debug assembly, unit tests and lint passed during the official window. Physical RMX3201/API 30 with Google Play Services is ADB-authorized for real OAuth testing.
- Track evidence: live Devfolio page verified the Open Finalists, Visa and Best UX prizes; the proof strategy now uses one shared real transaction path.
- Truth boundary at this milestone: the backend foundation and first migration were live. Later
  evidence above supersedes the then-pending auth/runtime-fixture status.

### 2026-07-30 — UI milestone 4: grounded decision and personal note

- Scope: redesigned full-screen discovery; four concise stages (`Products`, `Budget`, `Delivery`, `Match`); clue-convergence motion; cancel/error/retry/empty-ready handling; source-gated recommendation; fully model-driven sourced recommendation components; collapsed factual rejections; maximum two alternatives; editable/skippable personal note.
- Visual hierarchy: Sophie/date/budget remain pinned above a central gift trace. Interest, budget, timing and exclusion clues converge as stages complete; stage labels and active/completed states remain visible above the sticky action.
- Truthful empty edge: because no merchant/product source has been verified, runtime recommendation shows `One real detail is missing` with budget/timing/clue readiness. It displays no candidate, merchant, price, stock, delivery or purchase claim.
- Backend-ready contracts: added `ProductCandidate`, `CandidateRejection`, `CandidateRationale`, `RankedDecision`, `PurchaseIntent` and `PersonalMessage`; money remains minor-unit safe.
- Ranking validation: requires unique known candidate IDs, no ranked/rejected overlap, at most two alternatives, one rationale per ranked candidate and only supplied evidence IDs.
- Message behavior: starts empty, caps input at 500 characters, supports skip, marks generated drafts explicitly when supplied later and records whether the user edited them.
- Build + unit tests: `android\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain` — PASS.
- Lint: `android\gradlew.bat :app:lintDebug --console=plain` — PASS.
- Connected flows: `android\gradlew.bat :app:connectedDebugAndroidTest --console=plain` — PASS, 2/2 tests on `Doomo_API_31`; flagship route now reaches Discovery → source-needed decision → Personal note.
- Visual evidence: `artifacts/screenshots/milestone-4/discovery-running-final.png`, `discovery-ready-final.png`, `recommendation-source-needed-final.png`, and `message-empty.png`.
- Truth boundary: discovery stages and opaque IDs come from a controlled in-memory gateway. No product candidate is shown as real, no OpenAI call ran, and no purchase intent or Prava session exists.

### 2026-07-30 — UI milestone 3: emotional onboarding and editable context

- Scope: five fast, swipeable onboarding moments; recipient/photo/relationship editing; occasion/date/interests/exclusions/budget/clue editing; inline validation; Material date picker; shared mutable repository; contextual edit routes.
- Onboarding structure: emotion → date memory → clue capture → filtering logic → explicit review/control. Every page has a distinct visual composition and one headline plus one sentence; `Skip` remains available until the final page.
- Visual elements: dimensional gift/calendar/message-heart assets, orbit tiles, countdown rail, clue tokens, candidate rejection tiles, review facts, trace paths and restrained sparkles. These are explanatory or emotional accents, not product/merchant/payment evidence.
- Motion: pager-aware native Compose entrances, float/spring emphasis, candidate receding and selected-gift centering. The motion helper resolves to an instant state when Android animator duration is disabled.
- Navigation correction: edit routes retain a stable recipient/occasion snapshot and pop before repository refresh, preventing a loading-state double-pop back to Home.
- Build + unit tests: `android\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain` — PASS.
- Lint: `android\gradlew.bat :app:lintDebug --console=plain` — PASS.
- Connected flows: `android\gradlew.bat :app:connectedDebugAndroidTest --console=plain` — PASS, 2/2 tests on `Doomo_API_31`.
- Visual evidence: `artifacts/screenshots/milestone-3/onboarding/01-promise-final.png` through `05-control-final.png`, plus `setup-person.png`, `setup-occasion.png` and `setup-occasion-lower.png`.
- Emulator note: the API 31 AVD cold launch still shows substantial OpenGL jank and takes roughly 10–15 seconds; connected instrumentation startup is much faster. Physical-phone profiling remains required.
- Truth boundary: Talia/Sophie are controlled internal fixtures. The screenshots prove local Android presentation and state editing only; no Google account, merchant, OpenAI ranking, Prava session or transaction was verified.

### 2026-07-30 — UI milestone 2: entry and scan-first shell

- Scope: value-first welcome, Google Credential Manager shell, local `Not now` entry, four balanced top-level destinations, compact home, People, month-grid Occasions, Profile and recipient detail.
- Research correction: removed the ambiguous center plus/FAB because adding is not the app-wide highest-priority action; `Add` now appears contextually in People and Occasions.
- Copy correction: removed visible demo entry/badges and explanatory text blocks. Welcome uses one sentence; auth uses one sentence; Home exposes recipient, date, countdown, interests, budget and action in one card.
- Motion: one-shot, staggered Compose entrances plus short route fades/scales; the helper snaps when system animator duration is disabled.
- Build: `android\gradlew.bat :app:assembleDebug` — PASS.
- Tests: `android\gradlew.bat :app:testDebugUnitTest` — PASS.
- Lint: `android\gradlew.bat :app:lintDebug` — PASS.
- Connected flow: `android\gradlew.bat :app:connectedDebugAndroidTest` — PASS on `Doomo_API_31`.
- Visual evidence: `artifacts/screenshots/milestone-2/welcome-refined.png`, `sign-in-refined.png`, `home-refined.png`, and `occasions.png`.
- Truth boundary: Sophie/Talia remain internal controlled fixtures for UI development. No account was validated, no product was ranked, and no merchant, OpenAI or Prava request occurred.

### 2026-07-30 — UI reset milestone 1: design foundation

- Scope: replaced the warm editorial palette and serif display type with the approved indigo/blue/cool-white tokens, system-sans scale, refreshed code-native mark and shared controls.
- Decorative assets: generated and locally keyed three transparent WebP cutouts (`gift`, `calendar`, `message-heart`); no text, logo, merchant or product imagery.
- Build: `android\gradlew.bat :app:assembleDebug` — PASS.
- Tests: `android\gradlew.bat :app:testDebugUnitTest` — PASS.
- Lint: `android\gradlew.bat :app:lintDebug` — PASS.
- Artifact: `android/app/build/outputs/apk/debug/app-debug.apk`.
- Truth boundary: visual foundation only. Existing seeded routes still provide behavior; no Google, merchant, OpenAI or Prava call occurred.

### 2026-07-30 — Android foundation (pre-kickoff prototype)

- Scope: Gradle/Compose app shell, shared design tokens and components, money-safe type.
- Build: `android\gradlew.bat :app:assembleDebug` — PASS.
- Tests: `android\gradlew.bat :app:testDebugUnitTest` — PASS, 2 tests.
- Lint: `android\gradlew.bat :app:lintDebug` — PASS.
- Artifact: `android/app/build/outputs/apk/debug/app-debug.apk`.
- Device evidence: pending; the installed API 31 emulator has not been launched yet.
- Truth boundary: UI foundation only; no backend, merchant, OpenAI or Prava behavior.

### 2026-07-30 — Onboarding and upcoming occasion home (pre-kickoff prototype)

- Scope: native Compose onboarding and home routes, explicit loading/empty/error/content states, Sophie demo profile, occasion countdown, money-safe budget and review-before-payment copy.
- Build: `android\gradlew.bat :app:assembleDebug` — PASS.
- Tests: `android\gradlew.bat :app:testDebugUnitTest` — PASS, 3 tests.
- Lint: `android\gradlew.bat :app:lintDebug` — PASS.
- Compatibility fix: Java time core-library desugaring keeps the explicit local-date/timezone model valid at API 23.
- Artifact: `android/app/build/outputs/apk/debug/app-debug.apk`.
- Device evidence: pending until all four requested routes are connected.
- Truth boundary: Sophie and her occasion are controlled demo data. No catalog, ranking, merchant, OpenAI or Prava operation occurs.

### 2026-07-30 — Recipient profile and gift discovery (pre-kickoff prototype)

- Scope: routed recipient context review; interests, dislikes, hint provenance, money and local-date deadline; cancellable discovery stages; ranking and Prava backend-facing contracts.
- UX behavior: explains hard-rules-before-ranking, uses no fake percentage, keeps cancel/retry/back recovery visible, and distinguishes evidence from inference.
- Build: `android\gradlew.bat :app:assembleDebug` — PASS.
- Tests: `android\gradlew.bat :app:testDebugUnitTest` — PASS, 8 tests.
- Lint: `android\gradlew.bat :app:lintDebug` — PASS.
- State evidence: deterministic stage order, duplicate-start protection, cancellation, controlled gateway failure and duplicate candidate IDs are covered.
- Artifact: `android/app/build/outputs/apk/debug/app-debug.apk`.
- Device evidence: pending.
- Truth boundary: discovery runs a controlled local fixture and stops at `ReadyForRanking`. No live product fact, OpenAI request, Prava session or transaction exists.

### 2026-07-30 — Android device UX gate (pre-kickoff prototype)

- Device: headless `Doomo_API_31` emulator, Android API 31.
- Connected test: `android\gradlew.bat :app:connectedDebugAndroidTest` — PASS, onboarding → home → recipient → discovery.
- Touch evidence: primary and back actions assert at least 48dp.
- Visual evidence: `android/evidence/onboarding.png`, `home.png`, `profile.png`, `discovery_running.png`, `discovery_ready.png`.
- Text scale: `android/evidence/onboarding_font130.png`; primary action remains visible and content remains scrollable at 130%.
- Contrast: core text/background pairs measure 5.95:1–14.91:1.
- Fix from rendered review: removed duplicated status-bar insets from onboarding and home.
- Performance note: first post-install render took about 3.1 seconds on the headless emulator; warm navigation is responsive. Physical-device startup profiling remains pending.
- Truth boundary: screenshots prove rendered local UI only, not external integration behavior.

## Later

1. Signature animation.
2. Audio message.
3. Share-sheet capture.
4. Production-access request.
5. Extra recipients.

## Time checkpoints

### Hour 2

- Prava and merchant assumptions written.
- Primary and fallback paths chosen.

### Hour 8

- Backend and Android skeletons run.
- No new tools without decision log.

### Hour 16

- Bronze flow should work or scope must shrink.

### Hour 24

- Full transaction should repeat.
- UX pass begins only after this.

### Hour 32

- Feature freeze.
- Demo and reliability dominate.

### Hour 40

- Submission package complete.
- Only blockers may change code.

## Cut order

When behind, remove in this order:

1. settings and secondary navigation;
2. multi-recipient support;
3. share-sheet capture;
4. audio message;
5. advanced motion;
6. live broad merchant search;
7. onboarding.

Never cut:

- truthful product data boundary;
- deterministic constraints;
- exact amount review;
- completed Prava sandbox evidence;
- receipt/result;
- duplicate-purchase protection.
