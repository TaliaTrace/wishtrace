# Integration Status

Update this with observed facts, IDs and dates. Do not leave a successful spike only in chat history.

## Prava

- Environment: sandbox configured in ignored local environment
- Path: hosted full-checkout API through the backend; official polling + report-status
- Dashboard/API key created: YES (user-provided environment; value never recorded here)
- Authentication request verified: YES during official window — authenticated missing-session probe
  returned expected `404 NOT_FOUND`, response `9fecc0ee-838b-40c4-9ece-048f5f16bb5c`
- Session creation verified: CONTRACT + MOCK TRANSPORT — exact decimal money, hosted URL allowlist,
  response-ID capture and session-token discard tested; the live judged-window session is the next
  physical-phone step
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
- Backend boundary: purchase ledger, peppered quote/session idempotency, exact state transitions,
  fixed app return, authoritative polling, one-attempt Shopify automation and report-status are
  implemented. Billing exists only in request/browser memory and Prava credentials stay in the live
  browser context only. A repeated session tap cannot issue a second provider call; an interrupted
  checkout becomes `UNKNOWN` and is not blindly retried. If the merchant result was persisted before
  report-status was interrupted, reconciliation re-reports that result and never checks out again.
  Prava facts must match the approved merchant origin and total.
- Known blockers: no judged-window hosted session, tokenized-card attempt or authoritative provider
  result yet. Staging permits the exact $5 stored-value path solely for the organizer-required
  sandbox expected-failure proof; production compatibility still requires Prava approval/evidence.
- Last verified: 2026-08-02 00:37 PKT
- Evidence location: safe response ID above; `backend/app/prava.py`, `backend/app/purchase.py` and
  isolated transport/state tests; no credential or provider payload retained

Organizer truth boundary: production access requires the sandbox integration to work end to end in
the Android app and a tokenized test-card transaction to be attempted through browser automation
against a real merchant. The expected sandbox merchant failure is accepted; it is not an order.

## Commerce

- Primary merchant/path: Jackbox Games official Shopify/UCP store, one fixed $5 digital gift card
- Backup merchant/path: none enabled; HyperX physical was retired because the user has no US
  shipping address, and Turtle Beach's digital card starts at $50
- Mode: live only; no controlled/runtime fixture fallback
- UCP profile verified: YES — `checkout.jackboxgames.com/.well-known/ucp` advertises UCP
  `2026-04-08`, Shopify catalog/cart/checkout/order, and card payment handlers. An app UCP search
  still requires the permanent public WishTrace profile URL after deployment.
- Product detail verified: YES — official product `6734381809798`, variant `39783705149574`,
  SKU `GC20221246`, title `Jackbox Games Gift Card - $5 USD`.
- Price/availability verified: YES AT PRODUCT/CART LEVEL — official product JSON and a live cart
  returned exactly 500 USD minor units for one available variant.
- Shipping verified: NOT REQUIRED — the live cart returned `requires_shipping=false` and
  `gift_card=true`; checkout showed billing fields and no shipping address.
- Delivery fact verified: PARTIAL — Jackbox states the purchaser receives the digital card by email
  and forwards it to the recipient. Exact timing is not promised. Runtime copy identifies the
  checkout contact and manual-forwarding step rather than claiming direct recipient delivery.
- Quote/total verified: YES THROUGH THE RUNTIME ACTOR — after synthetic US billing in a non-payment
  probe, the same gateway wired into the API returned item 500, shipping 0, tax 0 and total 500 USD
  minor units, with a fresh 20-minute quote. No payment was submitted.
- Checkout compatibility verified: LIVE QUOTE + FORM + ONE-CLICK BOUNDARY — the exact Shopify card
  form and PCI iframes were observed. On Windows, Playwright runs on a dedicated Proactor loop beside
  psycopg's Selector loop; the production gateway passed live with this boundary. No credential or
  payment was submitted.
- Prava compatibility: CONTRACT/TESTED BOUNDARY ONLY — code accepts one matching in-memory
  credential, makes one merchant attempt, reports `APPROVED`/`DECLINED`, and re-polls. Stored-value
  permission and the live sandbox attempt remain pending.
- Runtime gate: checkout and stored value remain disabled by code default. Both explicit flags are
  enabled only on the Azure staging revision for the organizer-required sandbox attempt. This does
  not claim production stored-value support.
- Amazon decision: rejected for this sprint. Its current catalog and Incentives gift-card APIs need
  separate program onboarding/credentials and cannot provide a truthful hackathon integration now.
- Geography risk: Jackbox limits purchase/use to supported regions. A future real $5 gift remains
  conditional on the cardholder/recipient region and Prava's stored-value policy.
- Last verified: 2026-08-01 22:40 PKT during the official window
- Evidence location: `artifacts/backend/jackbox-digital-checkout-probe-2026-08-01.png`,
  `artifacts/backend/jackbox-runtime-quote-2026-08-01.json`,
  `backend/app/merchant_browser.py`, `backend/tests/test_merchant_browser.py`

## OpenAI

- Account/project used: Azure AI Foundry, configured in ignored environment
- Model selected: `gpt-5.6-terra`, the only observed deployment in the account with provisioning
  state `Succeeded`
- Structured extraction verified:
- Structured ranking verified: LIVE PROVIDER + CONTRACT — the configured
  `services.ai.azure.com/openai/v1/` endpoint returned a completed, schema-valid response for an
  opaque test candidate through the production transport. The response carried a safe provider
  request ID. Strict dynamic JSON Schema, `store=false`, known candidate/evidence enums and
  post-response validation also pass automated tests. No recipient hint or payment fact was used in
  the live probe. A subsequent authenticated physical-phone run passed the exact eligible live
  Jackbox candidate through the same boundary and rendered the validated decision.
- Multimodal verified:
- Message generation verified:
- Invalid-output fallback tested: YES — malformed schema, unknown/rejected candidate ID, unsupported
  commerce claim, model no-selection, provider failure, one repair, direct-evidence fallback and
  explicit user-choice recovery are covered
- Latency: 3,662 ms for the 2026-08-01 judged-window structured-output probe
- Backend boundary: authenticated rank/read routes persist one immutable evidence-linked decision per
  discovery. Only still-eligible `LIVE` snapshots can reach the model; recipient name, merchant URL,
  money, delivery and payment data are omitted from provider input.
- Known blockers: none at the ranking boundary. The remaining flow blocker is the human-entered
  billing/Prava interaction after ranking; the provider itself is no longer a blocker.
- Azure runtime: DEPLOYED — Azure Container Apps serves the locked browser image over managed HTTPS
  from a single active revision. A Basic ACR uses a managed pull identity; secrets are Container Apps
  secret references, not image or Android values. Public health reports PostgreSQL TLS true and the
  public UCP profile returns `2026-04-08` with cache headers.
- Last verified: 2026-08-02 01:05 PKT; live provider, public runtime and exact-candidate phone proofs passed

## Supabase PostgreSQL

- Path: session pooler on port 5432 with SQLAlchemy async psycopg 3 and `NullPool`
- Client TLS verified: YES via libpq `ssl_in_use`
- Server version observed: PostgreSQL 17.6
- Migration status: `20260801_0009 (head)`; Alembic model/schema drift check passes
- Migration content: foundation; Google users/challenges/sessions; owned recipients, preferences,
  hints and occasions; one-recipient Gold uniqueness; owned immutable discovery runs, live candidate
  snapshots and deterministic rejection records; exact purchase snapshots, public Prava session
  identifiers, hashed idempotency operations and immutable transaction transitions; owned ranking
  runs, immutable evidence snapshots and ordered evidence-linked decisions; idempotent merchant
  quotes, merchant/Prava outcome evidence, and one owned editable personal message per purchase
- Permanent ignored local `.env` contains `sslmode=require`: YES; a fresh settings load and read-only
  connection probe used that value directly and reported client TLS true
- Stable local `SESSION_TOKEN_PEPPER`: YES; presence and minimum length were checked without printing
  the value
- Last verified: 2026-08-01 23:23 PKT during official window

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
- API connection verified: YES over public Azure HTTPS on the physical phone; the existing real
  Google session reopened an authenticated, genuinely empty Home after the public-URL APK update.
  The user then created one real runtime recipient/occasion context and a force-stop/relaunch restored
  it from the backend.
- Azure runtime packaging: DEPLOYED — the frozen Python/Playwright image runs in Azure Container
  Apps behind managed HTTPS; one healthy revision receives 100% traffic
- Custom tab/hosted approval verified: CODE + BUILD — AndroidX Browser `1.10.0` opens only the exact
  HTTPS sandbox/production Prava collection hosts; an actual hosted session remains pending
- App link verified: DEVICE RECOVERY PASS — a synthetic return carrying only a random, nonexistent
  UUID reached the singleTop activity, called the authenticated public backend and rendered its safe
  `PURCHASE_INTENT_NOT_FOUND` recovery instead of trusting browser state. An actual Prava return
  remains pending.
- Process recreation tested: PASS for authenticated recipient/home recovery after a physical-device
  force-stop/relaunch; payment-return process-death recovery still needs the hosted run.
- Accessibility checked: primary/back targets asserted at 48dp; semantic headings/labels and onboarding page semantics present; core contrast pairs are 5.95:1–14.91:1; onboarding captured at 130% text scale; motion snaps when animator duration is disabled. TalkBack/manual switch-access testing remains pending.
- Keyboard/IME checked: API 30 physical-device form collapse was fixed by removing duplicate IME
  inset consumption; connected Compose regression passed 1/1 with name and relationship visible
- Evidence location: `artifacts/screenshots/milestone-4/`, `artifacts/screenshots/milestone-3/`, `artifacts/screenshots/milestone-2/`, `android/evidence/`, `android/app/build/reports/androidTests/connected/debug/`
- Live decision proof: PASS — the phone submitted user-created recipient context and a
  user-authorized editable 2026-08-09 sandbox occasion, restored it after restart, retrieved the
  real Jackbox $5 candidate and rendered the grounded Azure ranking. The date is not claimed as the
  recipient's verified birthday.
- Known blockers: the app is at the just-in-time billing form. Exact live quote, Prava hosted
  interaction, tokenized merchant attempt, authoritative reconciliation and persisted message remain
  unproven until the user completes the private billing and hosted approval steps.

## Demo

- Bronze flow repeat count:
- Primary video:
- Backup video:
- Five-second test:
- Clean-checkout build:
- Submission URL:
