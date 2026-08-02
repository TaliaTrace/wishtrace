# Integration Status

Update this with observed facts, IDs and dates. Do not leave a successful spike only in chat history.

## Prava

- Environment: sandbox configured in ignored local environment
- Path: hosted mandate setup through the backend; customer-scoped mandate association; official
  charge + mandate-report; exact one-shot merchant browser attempt
- Dashboard/API key created: YES (user-provided environment; value never recorded here)
- Authentication request verified: YES during official window — authenticated missing-session probe
  returned expected `404 NOT_FOUND`, response `9fecc0ee-838b-40c4-9ece-048f5f16bb5c`
- Session creation verified: LIVE SANDBOX — a real authorize-only mandate setup session was created
  by the deployed backend and its allowlisted hosted URL opened in an Android Custom Tab. Exact
  decimal money, response-ID handling and session-token discard also pass transport tests.
- Hosted approval verified: LIVE SANDBOX PASS — after earlier safely recorded provisioning/device
  failures, the public sandbox card ending `7912` passed card collection, OTP and Android passkey
  creation. Prava returned one active mandate; no card number, OTP or passkey entered WishTrace.
  An earlier exact-contract hosted attempt
  returned authoritative `failed` with safe categories `PROVISION_ERROR` and
  `DEVICE_BINDING_FAILED` before creating a mandate or credential. A later abandoned session
  expired with zero transactions/credentials. The production adapter then observed and explicitly
  preselected one active default saved-card enrollment; that setup also returned
  `DEVICE_BINDING_FAILED`. WishTrace retired the failed enrollment through Prava's documented
  delete-card endpoint. The organizer-issued team card then returned `PROVISION_ERROR`; Birdie
  explicitly authorized the standard public cards as the immediate sandbox fallback.
- App return verified: PARTIAL — the hosted failure remained in the Custom Tab, so no automatic
  Prava redirect was observed. A validated physical-phone app-link opened WishTrace and reconciled
  the exact backend failure into its terminal recovery UI.
- Mandate association verified: LIVE LIST + CONTRACT — the real sandbox list endpoint parsed through
  the production adapter and now returned one active mandate after approval. Post-return association
  matched customer, time, merchant, amount, currency, frequency and scope and refused ambiguity;
  WishTrace persisted the authoritative provider mandate ID and rendered Autopilot on.
- Mandate charge verified: LIVE PROVIDER FAILURE + CONTRACT — one documented `$5.00` charge against
  the active live mandate returned provider transaction
  `txn_01KZ1ZTD34T08SD7QPFS24XFJ1` and `failed / FETCH_AGENTIC_CREDS_ERROR`. No credential or
  reportable transaction reference was issued; no merchant submission followed. Current successful
  `mandateId`, `transactionId`, nested credentials and `awaiting_result` shapes remain transport-tested.
  One explicit docs-sanctioned retry was guarded by provider `active`, `$0.00` spent and zero counted
  charges. It failed identically as transaction `txn_01KZ218YJV1NBFM3DZH9T54N7M`, response
  `67174297-c74e-4f54-beb3-e5ac17f08660`, again before credentials or merchant submission.
- Mandate report verified: CURRENT CONTRACT + MOCK TRANSPORT — current completed/failed result,
  mandate/transaction identity and Visa confirmation are checked; the live merchant attempt remained
  unknown, so WishTrace correctly did not report an invented approval or decline.
- Standard hosted-session fallback: LIVE PROVIDER FAILURE — one real hosted session was created for
  purchase intent `36e40790-751f-451d-9b45-6f3e7b59338c` and the same exact `$5.00` Jackbox quote.
  Prava displayed that identity was verified but payment could not complete. Authoritative
  reconciliation returned `FAILED`, provider status `failed`, no merchant outcome and no order.
  Provider transaction `txn_01KZ21VFY5C91EFYKBCGYB0DFC` reported
  `FETCH_AGENTIC_CREDS_ERROR`. Reopening the same existing hosted URL created no new session; the
  provider page recorded transaction `txn_01KZ21X8G8RRP5CNYM535W5NMK` with
  `FIDO_START_FAILED`. No usable credential was issued in either attempt.
- Real-merchant browser attempt verified: LIVE SANDBOX PARTIAL PASS — the newest mandate produced a
  one-time credential and the allowlisted Jackbox actor clicked Pay once for exact Drawful 2 at
  `$9.99`. The page exposed neither a verified order nor a recognized explicit decline within 45
  seconds, so the immutable result is `UNKNOWN`; no report or retry followed.
- Authoritative success verified:
- Decline/cancel/unknown tested: create timeout, server error, unsafe redirect and malformed success
  freeze as `UNKNOWN`; replay refusal and invalid provider facts pass automated tests. A live hosted
  provisioning failure now becomes retryable `FAILED`; live merchant decline and cancel remain pending.
- Production access requested: NO — intentionally gated behind organizer-required sandbox evidence
- Backend boundary: purchase ledger, peppered quote/session idempotency, exact state transitions,
  fixed app return, authoritative polling, one-attempt Shopify automation and report-status are
  implemented. Billing exists only in request/browser memory and Prava credentials stay in the live
  browser context only. A repeated session tap cannot issue a second provider call; an interrupted
  checkout becomes `UNKNOWN` and is not blindly retried. If the merchant result was persisted before
  report-status was interrupted, reconciliation re-reports that result and never checks out again.
  Prava facts must match the approved merchant origin and total.
- Request sanitation: PASS — explicit mandate `intent` and `integration_type`, routable-email
  enforcement, delegated bare HTTPS merchant origin, exact decimal money, actual item price,
  single user-triggered retry boundary, unambiguous saved-card handling, Custom Tab handoff, and safe
  provider failure categories.
  The observed sandbox create response omitted documented `authorizeOnly`; omission is accepted only
  with explicit request intent, while an explicit `false` fails closed.
- Saved-card fact: LIVE SANDBOX, SUPERSEDING THE EARLIER DELETE ASSUMPTION — a fresh read-only
  provider list showed two active enrollments. Card ending `7789` is again present and marked
  default; card ending `7912`, which completed OTP/passkey enrollment, is active and non-default.
  The current provider list is authoritative even though an earlier delete response appeared to
  retire `7789`. WishTrace now preselects a card only when exactly one active enrollment exists;
  with these two cards it omits `card_id` and requires the owner to choose in Prava's hosted UI.
  Card metadata is used only to distinguish enrollments and no PAN/CVV is stored.
- Latest approval evidence: LIVE SANDBOX PARTIAL PASS — the phone completed hosted approval for a
  `$10` Drawful 2 mandate and returned through the app. A later mandate charge returned a one-time
  credential, and the Jackbox actor submitted Pay once at `$9.99`. WishTrace did not expose or
  persist the credential. No merchant order or accepted decline is claimed.
- Sandbox browser-automation contract: ORGANIZER-CONFIRMED — Prava's hosted Browser Harness is not
  available in sandbox. Teams must build their own automation and call report-status themselves.
  WishTrace's exact-product Playwright actor is therefore the correct post-mint implementation.
- Known blocker: the observed post-mint merchant result is unknown and must remain locked. A
  separate different-product approval is allowed only by an explicit user action in the sandbox
  environment; production cannot replace an unknown charge.
- Conflict recovery: DEPLOYED — Android automatically refreshes an existing mandate after a setup
  conflict. An explicit different-gift sandbox recovery must name the exact latest locked mandate,
  prove its post-mint charge is `UNKNOWN`, and select a different live product. The old mandate is
  preserved and never retried or silently deleted.
- Last verified: 2026-08-03 03:48 PKT
- Evidence location: migration `20260803_0014`; ACR build `chj`, image digest
  `sha256:f2655f58ab9e46cea25050bf9f1bfea6165ea0aa8f94623a8f2774d935ca1a8e`, and healthy deployed
  revision `wishtrace-api--reconcile1` at 100% traffic. Public health reports PostgreSQL 17.6 over
  TLS; the UCP profile returns 200 with `public, max-age=300`; 158 backend tests plus Android
  build/unit/lint pass; the matching APK installed in place. No credential, card data or provider
  payload is retained.

Organizer truth boundary: production access requires the sandbox integration to work end to end in
the Android app and a tokenized test-card transaction to be attempted through browser automation
against a real merchant. The expected sandbox merchant failure is accepted; it is not an order.

## Commerce

- Primary merchant/path: Jackbox Games official Shopify/UCP store, four exact cart-verified digital
  products: the $5 store gift card plus Quiplash 2 InterLASHional, Drawful 2 and Quiplash
- Backup merchant/path: none enabled; HyperX physical was retired because the user has no US
  shipping address, and Turtle Beach's digital card starts at $50
- Mode: live only; no controlled/runtime fixture fallback
- Repeat-discovery rule: product IDs already selected into a mandate for the same occasion recede
  only when another candidate has independently passed every live commerce constraint. If live
  inventory has no fresh eligible alternative, the prior product remains available; WishTrace does
  not fabricate variety.
- UCP profile verified: YES — `checkout.jackboxgames.com/.well-known/ucp` advertises UCP
  `2026-04-08`, Shopify catalog/cart/checkout/order, and card payment handlers. An app UCP search
  still requires the permanent public WishTrace profile URL after deployment.
- Product detail verified: YES — each enabled product is bound to one exact official Shopify
  product ID, product path and variant ID. The gift card is product `6734381809798`, variant
  `39783705149574`, SKU `GC20221246`; the three enabled games are recorded in
  `docs/MERCHANT_VALIDATION.md` and `backend/app/merchant_browser.py`.
- Price/availability verified: YES AT PRODUCT/CART LEVEL — official UCP results and isolated live
  carts returned the available $5 card at 500 USD minor units and each enabled game at 999 USD
  minor units. Search de-duplicates repeated product IDs.
- Shipping verified: NOT REQUIRED — all four live carts returned `requires_shipping=false`; the
  store card returned `gift_card=true` and the games returned `gift_card=false`.
- Delivery fact verified: PARTIAL — Jackbox states the purchaser receives the digital card by email
  and forwards it to the recipient. The games are digital codes. Exact timing is not promised, so
  runtime copy says timing is unconfirmed rather than claiming instant or direct delivery.
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
- Last verified: 2026-08-03 01:40 PKT during the official window; later repeated catalog probes were
  rate-limited, so the recorded isolated cart evidence is retained rather than retried
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
- Migration status: `20260803_0014 (head)`; Alembic model/schema drift check passes
- Migration content: foundation; Google users/challenges/sessions; owned recipients, preferences,
  hints and occasions; one-recipient Gold uniqueness; owned immutable discovery runs, live candidate
  snapshots and deterministic rejection records; exact purchase snapshots, public Prava session
  identifiers, hashed idempotency operations and immutable transaction transitions; owned ranking
  runs, immutable evidence snapshots and ordered evidence-linked decisions; idempotent merchant
  quotes, merchant/Prava outcome evidence, one owned editable personal message per purchase, owned
  mandate/charge audit rows, Gift-DNA personality/age evidence, explicit occasion recurrence,
  normalized mandate-setup failure categories, explicit digital-product kind, and immutable mandate
  history across user-authorized recovery attempts
- Permanent ignored local `.env` contains `sslmode=require`: YES; a fresh settings load and read-only
  connection probe used that value directly and reported client TLS true
- Stable local `SESSION_TOKEN_PEPPER`: YES; presence and minimum length were checked without printing
  the value
- Last verified: 2026-08-03 01:45 PKT during official window

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
  Apps behind managed HTTPS; healthy revision `wishtrace-api--reconcile1` receives 100% traffic
- Custom tab/hosted approval verified: LIVE SANDBOX PASS — AndroidX Browser `1.10.0` opened the real
  Prava sandbox collection host, the user completed approval, the app return reconciled an active
  `$10` mandate, and Prava later issued one one-time credential to the backend-only merchant path.
  An earlier standard purchase-intent session remains truthfully recorded as terminal `FAILED`.
- App link verified: LIVE SANDBOX PASS — a real hosted approval returned to WishTrace and reconciled
  its exact active mandate. A synthetic return carrying only a random, nonexistent UUID also reached
  the singleTop activity and rendered safe `PURCHASE_INTENT_NOT_FOUND` recovery instead of trusting
  browser state.
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
- Current local recovery: PASS — recommendation alternatives are selectable and a newly selected
  product starts a fresh mandate while preserving earlier audit rows. Once an occasion has any
  mandate, Home and recipient actions reopen and refresh that Autopilot state instead of silently
  restarting discovery. Home distinguishes active, handled, awaiting-approval and failed-attempt
  states. The updated APK assembled, passed unit/lint checks and installed in place.
- Current recovery UX: passkey start/authentication/cancellation failures name the exact pre-mandate
  boundary and expose one explicit `Retry Prava approval` action. Provisioning and mint failures stay
  terminal. Debug assembly, unit tests and lint pass; the updated APK is installed in place.
- Known blockers: no further payment attempt is safe for this mandate. Prava report, Visa
  confirmation and merchant order remain unproven; the installed recovery build must show the
  sticky unknown state without a looping action.

## Demo

- Bronze flow repeat count:
- Primary video:
- Backup video:
- Five-second test:
- Clean-checkout build:
- Submission URL:
