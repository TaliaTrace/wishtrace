# Integration Status

Update this with observed facts, IDs and dates. Do not leave a successful spike only in chat history.

## Prava

- Environment: sandbox configured in ignored local environment
- Path: hosted full-checkout API through the backend; official polling + report-status
- Dashboard/API key created: YES (user-provided environment; value never recorded here)
- Authentication request verified: YES during official window — authenticated missing-session probe
  returned expected `404 NOT_FOUND`, response `9fecc0ee-838b-40c4-9ece-048f5f16bb5c`
- Session creation verified: CONTRACT + MOCK TRANSPORT — exact decimal money, hosted URL allowlist,
  response-ID capture and session-token discard tested; no live sandbox session created yet
- Hosted approval verified:
- App return verified:
- Payment-result polling verified: CONTRACT + MOCK TRANSPORT — credentials remain masked and
  memory-only; no live approval result polled yet
- Report-status verified: CONTRACT + MOCK TRANSPORT — only the documented purchase outcome fields
  are sent; no live merchant attempt has been reported yet
- Real-merchant browser attempt verified:
- Authoritative success verified:
- Decline/cancel/unknown tested: create timeout, server error, unsafe redirect and malformed success
  freeze as `UNKNOWN`; replay refusal and invalid provider facts pass automated tests; live decline
  and cancel remain pending
- Production access requested: NO — intentionally gated behind organizer-required sandbox evidence
- Backend boundary: purchase ledger, hashed idempotency mapping, exact state transitions, fixed app
  return and authoritative polling are implemented. A repeated session tap cannot issue a second
  provider call; an uncertain create cannot be blindly retried. Prava facts must match the approved
  merchant origin and total before credentials become usable.
- Known blockers: official docs describe Browser Harness but expose no SDK/API invocation contract;
  no judged-window hosted session, tokenized-card attempt or authoritative result yet. Birdie support
  answer is required before implementing or inventing this boundary.
- Last verified: 2026-08-01 21:04 PKT
- Evidence location: safe response ID above; `backend/app/prava.py`, `backend/app/purchase.py` and
  isolated transport/state tests; no credential or provider payload retained

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
- Structured ranking verified: CONTRACT + SDK WIRE TEST — strict dynamic JSON Schema, official
  Responses client, `store=false`, known candidate/evidence enums and post-response validation pass
  automated tests. No runtime recommendation has been generated.
- Multimodal verified:
- Message generation verified:
- Invalid-output fallback tested: YES — malformed schema, unknown/rejected candidate ID, unsupported
  commerce claim, model no-selection, provider failure, one repair, direct-evidence fallback and
  explicit user-choice recovery are covered
- Latency: no judged-window provider latency available
- Backend boundary: authenticated rank/read routes persist one immutable evidence-linked decision per
  discovery. Only still-eligible `LIVE` snapshots can reach the model; recipient name, merchant URL,
  money, delivery and payment data are omitted from provider input.
- Known blockers: all observed live products currently fail checkout support, so runtime ranking
  correctly stops before Azure. The configured Azure hostname also failed DNS resolution during a
  2026-08-01 21:31 PKT test-only structured-output probe; the SDK returned `MODEL_TIMEOUT`. Replace
  `AZURE_OPENAI_BASE_URL` with the exact deployment target URI from Foundry before retesting.
- Last verified: 2026-08-01 21:33 PKT; implementation/tests current, live provider proof blocked

## Supabase PostgreSQL

- Path: session pooler on port 5432 with SQLAlchemy async psycopg 3 and `NullPool`
- Client TLS verified: YES via libpq `ssl_in_use`
- Server version observed: PostgreSQL 17.6
- Migration status: `20260801_0007 (head)`; Alembic model/schema drift check passes
- Migration content: foundation; Google users/challenges/sessions; owned recipients, preferences,
  hints and occasions; one-recipient Gold uniqueness; owned immutable discovery runs, live candidate
  snapshots and deterministic rejection records; exact purchase snapshots, public Prava session
  identifiers, hashed idempotency operations and immutable transaction transitions; owned ranking
  runs, immutable evidence snapshots and ordered evidence-linked decisions
- Permanent local `.env` contains `sslmode=require`: USER CONFIRMATION PENDING; verification used a process-only secure override
- Stable local `SESSION_TOKEN_PEPPER`: USER CONFIRMATION PENDING; the current local server uses a
  process-only value and must not be restarted before the permanent value is added
- Last verified: 2026-08-01 21:33 PKT during official window

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
