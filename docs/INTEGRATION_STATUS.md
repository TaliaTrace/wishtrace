# Integration Status

Update this with observed facts, IDs and dates. Do not leave a successful spike only in chat history.

## Prava

- Environment: sandbox configured in ignored local environment
- Path: hosted full-checkout API through the backend; official polling + report-status
- Dashboard/API key created: YES (user-provided environment; value never recorded here)
- Authentication request verified: YES during official window — authenticated missing-session probe
  returned expected `404 NOT_FOUND`, response `9fecc0ee-838b-40c4-9ece-048f5f16bb5c`
- Session creation verified:
- Hosted approval verified:
- App return verified:
- Payment-result polling verified:
- Report-status verified:
- Real-merchant browser attempt verified:
- Authoritative success verified:
- Decline/cancel/unknown tested:
- Production access requested: NO — intentionally gated behind organizer-required sandbox evidence
- Known blockers: official docs describe Browser Harness but expose no SDK/API invocation contract;
  no judged-window session, tokenized-card attempt or authoritative result yet. Birdie support answer
  requested before implementing or inventing this boundary.
- Last verified: 2026-08-01 20:45 PKT
- Evidence location: safe response ID above; no credential or provider payload retained

Organizer truth boundary: production access requires the sandbox integration to work end to end in
the Android app and a tokenized test-card transaction to be attempted through browser automation
against a real merchant. The expected sandbox merchant failure is accepted; it is not an order.

## Commerce

- Primary merchant/path: HyperX US UCP over Shopify MCP
- Backup merchant/path: Turtle Beach USA UCP over Shopify MCP
- Mode: live only; no controlled/runtime fixture fallback
- Search verified: YES — the implemented adapter returned 10 HyperX results for `gaming headset`;
  request `b0274ee3-4e99-4db4-8c7e-8df9bae7d9e0-1785598670`
- Product detail verified: YES AT CATALOG LEVEL — HyperX product and exact variant lookup matched
  product `gid://shopify/Product/7764616118429`, variant
  `gid://shopify/ProductVariant/43656365375645`, SKU `727A8AA`
- Price refresh verified: PARTIAL — repeated search plus lookup observed $64.99 USD for the same
  variant; the final checkout quote/total refresh is not implemented
- Availability verified: YES AT CATALOG LEVEL — selected variant returned `available=true`; the
  pre-approval refresh remains required
- Delivery data verified: NO — normalized as explicit `UNKNOWN`; no delivery promise is shown
- Quote/total verified: NO
- Checkout compatibility verified: NO — advertised checkout capability is not treated as execution
- Checkout probe: HyperX MCP returned `Tool not found: create_checkout` for the official UCP
  `create_checkout` operation (request
  `448fec69-405e-4c5b-8642-9d2151f8a729-1785598905`). All runtime candidates therefore remain
  `UNSUPPORTED_CHECKOUT` until the Prava browser path proves the real merchant handoff.
- Gift-card fact: Turtle Beach returned its own $50 digital gift-card variant live, but WishTrace
  classifies it as stored value and rejects it as `UNSUPPORTED_CHECKOUT` until Prava and merchant
  checkout support are proven. Xbox, Steam and Amazon cards have not been observed and are not shown.
- Protocol deviation: both observed Shopify UCP profiles omitted `Cache-Control`. WishTrace records
  `profile_cache_compliant=false`, performs no profile caching, and rejects explicit `private`,
  `no-store`, or `no-cache`; the WishTrace agent profile itself remains compliant.
- Known blockers: the advertised HyperX checkout capability is not exposed as an MCP checkout tool;
  final product refresh, address-dependent quote/delivery, browser checkout and Prava compatibility
  remain unverified
- Last verified: 2026-08-01 20:37 PKT during the official window
- Evidence location: `artifacts/backend/ucp-live-proof-2026-08-01.json`,
  `backend/app/commerce.py`, `backend/tests/test_commerce.py`

## OpenAI

- Account/project used: Azure AI Foundry, configured in ignored environment
- Model selected: configured Azure deployment; name intentionally not duplicated in docs
- Structured extraction verified:
- Structured ranking verified:
- Multimodal verified:
- Message generation verified:
- Invalid-output fallback tested:
- Latency:
- Known blockers: structured ranking against live candidate IDs is not implemented
- Last verified: pre-kickoff Responses API smoke response only; judged-window orchestration pending

## Supabase PostgreSQL

- Path: session pooler on port 5432 with SQLAlchemy async psycopg 3 and `NullPool`
- Client TLS verified: YES via libpq `ssl_in_use`
- Server version observed: PostgreSQL 17.6
- Migration status: `20260801_0005 (head)`
- Migration content: foundation; Google users/challenges/sessions; owned recipients, preferences,
  hints and occasions; one-recipient Gold uniqueness; owned immutable discovery runs, live candidate
  snapshots and deterministic rejection records
- Permanent local `.env` contains `sslmode=require`: USER CONFIRMATION PENDING; verification used a process-only secure override
- Stable local `SESSION_TOKEN_PEPPER`: USER CONFIRMATION PENDING; the current local server uses a
  process-only value and must not be restarted before the permanent value is added
- Last verified: 2026-08-01 during official window

## Android

- Application ID: `com.wishtrace.app`
- Build command: `cd android; .\gradlew.bat :app:assembleDebug` (verified 2026-08-01)
- Device/emulator: physical `RMX3201`, Android 11/API 30, serial redacted from public evidence; Google Play Services present and ADB authorized
- Navigation verified: five-page onboarding → required Google sign-in → authenticated empty Home;
  Home/People/Occasions/Profile, recipient detail and two-step person/occasion editor remain routed.
  Connected guardrail tests verify the sign-in requirement and safe back navigation.
- Google auth client: Credential Manager `1.6.0` and Google ID `1.2.0` compiled; `WISHTRACE_GOOGLE_WEB_CLIENT_ID` resource injection compiled
- Google account validation: VERIFIED on the physical phone through a real nonce-bound Google
  exchange; one backend user, active session and consumed challenge observed without capturing tokens
- API connection verified: YES locally through ADB reverse to `127.0.0.1:8000`; public HTTPS deploy pending
- Custom tab/hosted approval verified:
- App link verified:
- Process recreation tested:
- Accessibility checked: primary/back targets asserted at 48dp; semantic headings/labels and onboarding page semantics present; core contrast pairs are 5.95:1–14.91:1; onboarding captured at 130% text scale; motion snaps when animator duration is disabled. TalkBack/manual switch-access testing remains pending.
- Keyboard/IME checked: API 30 physical-device form collapse was fixed by removing duplicate IME
  inset consumption; connected Compose regression passed 1/1 with name and relationship visible
- Evidence location: `artifacts/screenshots/milestone-4/`, `artifacts/screenshots/milestone-3/`, `artifacts/screenshots/milestone-2/`, `android/evidence/`, `android/app/build/reports/androidTests/connected/debug/`
- Known blockers: one actual recipient/occasion still needs to be entered and recovered after app
  relaunch; the API is not publicly deployed; the running Android build is not yet wired to the new
  live discovery endpoints

## Demo

- Bronze flow repeat count:
- Primary video:
- Backup video:
- Five-second test:
- Clean-checkout build:
- Submission URL:
