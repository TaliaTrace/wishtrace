# Decision Log

Record only decisions that change product, architecture, truth boundary, schedule or demo.

## Template

### YYYY-MM-DD HH:MM TZ — Decision

- Context:
- Evidence:
- Options:
- Decision:
- Consequence:
- Rollback/fallback:
- Owner:

## Seed decisions

### 2026-07-29 — Native Android

- Context: UX prize and user preference favor a polished mobile app.
- Decision: Jetpack Compose is the primary client.
- Consequence: Prava integration goes through backend + hosted/native-supported approval flow.

### 2026-07-29 — Prava SDK/API default

- Evidence: Builder Handbook recommends SDK/API for native embedded products and lists sandbox support.
- Decision: Do not design the MVP around MCP or CLI.

### 2026-07-29 — Generated design is art direction only

- Context: promotional concepts are strong but contain impossible/incorrect details.
- Decision: translate concepts into a real design system and Compose UI; do not treat them as implementation.

### 2026-07-29 — Gift card not guaranteed

- Context: recognizable cards are visually attractive but commerce support is unverified.
- Decision: select the real demo product only after merchant/Prava validation; physical gift fallback is acceptable.

### 2026-07-30 15:22 PKT — Disclosed pre-kickoff Android prototype

- Context: The local clock is before the official build window. The user explicitly asked to continue after the timing boundary was reported.
- Evidence: The handbook permits existing products only with clear disclosure and requires judged work to be completed during the official window.
- Options: Wait for kickoff, or create a disclosed prototype that cannot be represented as judged-window implementation.
- Decision: Build the requested Compose screen prototype now and record it as pre-existing work.
- Consequence: Submission materials must distinguish this prototype from changes made during the official window.
- Rollback/fallback: Keep an exact disclosure and enumerate later judged-window changes; never relabel the prototype as hackathon-built.
- Owner: WishTrace team / Codex

### 2026-07-30 15:22 PKT — Warm editorial Compose system

- Context: Generated concepts overuse lavender, fake products and decorative dashboard patterns.
- Evidence: Repository UX priorities require warmth, clarity, trust, one dominant action and no unsupported commerce claims. Mobbin free-plan searches returned no usable references.
- Options: Copy the concepts, retain the proposed purple token set, or establish a restrained native system.
- Decision: Use paper, raisin, sage and terracotta tokens; system-serif headlines; system-sans body copy; real Compose shapes; and a single trace motif.
- Consequence: The app remains distinctive without generated imagery or a third-party UI kit.
- Rollback/fallback: Tokens are centralized and can be adjusted after emulator contrast and five-second testing.
- Owner: WishTrace team / Codex

### 2026-07-30 16:04 PKT — Discovery stops at an honest ranking boundary

- Context: The requested prototype needs a discovery experience before merchant, OpenAI and Prava access has been proven.
- Evidence: Product rules prohibit invented product facts, model-created candidates and payment success claims. The screen map calls for meaningful progress without fake percentages.
- Options: Display fictional gift cards, leave the action inert, or exercise a controlled deterministic fixture that stops before recommendation.
- Decision: Run four cancellable controlled stages and end at `ReadyForRanking` with opaque seeded candidate IDs. Show no product, merchant, price, delivery or payment claim.
- Consequence: The UX demonstrates rule order, cancellation and recovery while explicitly stating that no live lookup, OpenAI ranking, Prava session or purchase occurred.
- Rollback/fallback: Replace only the gateway implementation after backend candidate validation is proven; retain the UI state contract and truth boundary.
- Owner: WishTrace team / Codex

### 2026-07-30 16:54 PKT — Indigo scan-first UI supersedes warm editorial prototype

- Context: Device captures showed that large serif headlines and explanatory paragraphs displaced the recipient, occasion and action. The user explicitly rejected the palette and approved a purple/blue/white reset based on the supplied visual motive.
- Evidence: Public UXPeak guidance emphasizes hierarchy, spacing, contrast and concise copy; Android guidance favors value-first onboarding and familiar navigation. The Mobbin free plan still returned no usable reference results.
- Options: Tune the warm editorial system, copy the marketing concept, or establish a compact native Android system.
- Decision: Supersede the 15:22 warm editorial decision with fixed indigo/blue/cool-white tokens, system-sans type, compact factual cards, four destinations plus a center Add action, and three original non-product dimensional accents.
- Consequence: Existing screens migrate through compatibility aliases while the Gold flow is rebuilt. Generated assets remain decorative and never serve as product, merchant, payment or implementation evidence.
- Rollback/fallback: All visual tokens remain centralized; the assets can be removed without changing screen state or navigation.
- Owner: WishTrace team / Codex

### 2026-07-30 17:36 PKT — Remove consumer demo mode and global plus

- Context: Emulator captures showed the center plus competing with the core `Find a gift` action, while visible demo badges and explanatory copy made the UI feel like a prototype rather than a modern app. The user explicitly rejected both.
- Evidence: Android layout guidance reserves a FAB for the single highest-importance action and recommends three to five equal primary navigation destinations. NN/g hierarchy and minimalist heuristics say ambiguous controls and extra information compete with the task. Repeated Mobbin free-plan searches returned only `INVALID_ARGUMENT`, so no Mobbin screen informed the change.
- Options: restyle the plus, retain a visible demo mode, or remove both concepts and put creation beside its destination.
- Decision: Use four equal navigation destinations with contextual `Add` text actions in People and Occasions. Replace the visible demo choice with a conventional Google sign-in route and local `Not now` session. Keep controlled fixture truth in code, evidence and documentation.
- Consequence: The home hierarchy is quieter and the consumer experience no longer advertises a hackathon/demo concept. Google remains non-live until backend validation is supplied.
- Rollback/fallback: A labeled extended action can return only if usability testing proves contextual creation is undiscoverable; do not restore an ambiguous bare plus.
- Owner: WishTrace team / Codex

### 2026-07-30 19:02 PKT — Five purposeful onboarding moments supersede the short-carousel limit

- Context: The single welcome treatment felt dry and did not communicate how WishTrace moves from caring about someone to making a controlled gift decision. The user requested a longer, emotionally engaging onboarding but explicitly rejected extra screens made only to increase length.
- Evidence: Lazyweb's public onboarding research distinguishes longer value education from shorter sign-up flows and shows quizzes are uncommon; public Finch and Up Ahead captures use an expressive focal object, stable progress/action and concise copy. Rendered API 31 review confirmed five visually distinct compositions remain scan-first.
- Options: Keep one welcome, use two or three generic feature slides, add a personalization quiz, or build five product-specific value/trust moments.
- Decision: Use five swipeable and skippable moments: thoughtfulness, date memory, clue capture, clue-based filtering, and explicit user review. Follow with Google/local session choice and the real two-step recipient setup; do not add a quiz unless its answers materially alter the product.
- Consequence: Onboarding is longer in screen count but fast in interaction, low in copy and non-blocking. Motion explains the product logic and snaps to final state when Android animation is disabled.
- Rollback/fallback: Keep the stable pager structure; remove or merge only a moment that fails five-second comprehension testing. Never add a redundant slide to make the sequence feel longer.
- Owner: WishTrace team / Codex

### 2026-07-30 19:02 PKT — One mutable context source feeds every Gold surface

- Context: Recipient and occasion editing must update Home, People, Recipient detail and Occasions without adding Room, DI frameworks or multi-recipient scope.
- Evidence: A shared seeded repository with `StateFlow` supports the Gold one-recipient/one-occasion slice. Connected testing found that refreshing before popping an edit route could expose a transient loading state and trigger a second recovery pop.
- Options: Keep static fixtures per screen, add persistence/DI infrastructure, or use one manually injected in-memory repository with immutable snapshots at route entry.
- Decision: Keep one manually injected mutable repository behind `PeopleRepository` and `OccasionRepository`. Edit routes remember the target snapshot, pop before refreshing observers and validate every save.
- Consequence: All current surfaces remain consistent and backend-ready without architecture expansion. Data is process-local and controlled, not durable or live.
- Rollback/fallback: Replace the repository implementation behind the same contracts when backend persistence exists; keep route snapshots and validation behavior.
- Owner: WishTrace team / Codex

### 2026-07-30 19:34 PKT — Recommendation stays visually empty until product truth exists

- Context: UI milestone 4 needs a complete decision route before merchant, OpenAI and Prava access is proven. Filling the recommendation card with a plausible brand/price would make a cleaner screenshot but violate the product truth boundary.
- Evidence: Merchant validation requires observed product ID, price, availability, variant, delivery, quote, checkout and Prava compatibility. OpenAI orchestration permits ranking only supplied eligible IDs and evidence. No repository evidence currently meets those requirements.
- Options: invent a polished product, expose opaque controlled IDs as products, omit the route, or design an intentional source-needed state while implementing sourced content contracts.
- Decision: Runtime Recommendation shows no candidate facts until a verified source is connected. Build full model-driven content/rejection/alternative components behind strict `ProductCandidate` and `RankedDecision` contracts; show only recipient budget/timing/clue facts in the current route.
- Consequence: The UI journey and recovery behavior can be tested without making a commerce claim. A real source can replace the state without redesigning the screen.
- Rollback/fallback: A recorded real-product fixture may populate the content state only after its source timestamp and mode are stored. Never use an invented fixture to bypass the empty edge.
- Owner: WishTrace team / Codex

### 2026-08-01 14:29 PKT — Official window opened and baseline frozen

- Context: Judged implementation must be distinguishable from the disclosed pre-kickoff prototype.
- Evidence: Local time was `2026-08-01 14:29:55 +05:00`, after the 07:00 PKT kickoff. The workspace had no Git metadata.
- Decision: Initialize Git, secret-scan tracked candidates, and commit the complete pre-kickoff state as `283f5be` before judged-window implementation.
- Consequence: Every later commit and execution-board entry can be attributed to the official window. The baseline contains no tracked `.env`.
- Owner: WishTrace team / Codex

### 2026-08-01 — No runtime fixtures or local authentication escape hatch

- Context: Earlier plans allowed a controlled Bronze fallback and the prototype still wires seeded repositories plus `Not now` local auth.
- Evidence: The user explicitly requires no placeholders/demo behavior and is available to provide integration access. Runtime truth is also stronger finalist evidence than a polished simulation.
- Decision: Supersede every controlled-runtime fallback. Seeded/controlled data may remain only in tests, Compose previews and the preexisting-work disclosure; unavailable integrations produce honest empty/error/recovery states.
- Consequence: Network auth and context repositories now replace the pre-kickoff implementations.
  Runtime commerce stops at an honest unavailable state until a live source exists; preview/test
  fixtures remain isolated from application wiring.
- Owner: WishTrace team / Codex

### 2026-08-01 — Prava proof uses documented polling and a real-merchant attempt

- Context: Older architecture notes assumed a webhook and treated production access as a general next step.
- Evidence: Current Prava docs expose hosted full-checkout session creation, payment-result polling and report-status. The latest organizer announcement requires a sandbox tokenized test-card attempt through browser automation against a real merchant, accepts the expected failure, and warns against premature production applications.
- Decision: Implement create → hosted approval → poll → browser checkout attempt → report status → reconcile. Do not invent a webhook or Browser Harness endpoint. Gate production access until this flow is captured inside the app.
- Consequence: A successful Prava authorization without a verified merchant order produces an authorization/attempt result, never `Gift secured`.
- Owner: WishTrace team / Codex

### 2026-08-01 — Finalist, Visa and UX are the primary proof stack

- Context: The user wants the strongest chance of becoming a finalist and specifically prioritizes Visa and UX.
- Evidence: The live Devfolio prize page lists a $10,000 Open Finalists pool, $5,000 Best Visa Intelligent Commerce Implementation and $800 Best UX. OpenAI has a separate $9,000 winners/finalists pool.
- Decision: Optimize one shared vertical slice: reliable real transaction evidence for finalist judging, explicit consent/token isolation/idempotency for Visa, and five-second comprehension/recovery/emotional completion for UX. OpenAI grounded ranking supports the same flow.
- Consequence: No extra sponsor integration or decorative feature may displace transaction reliability, permission clarity or physical-device UX proof.
- Owner: WishTrace team / Codex

### 2026-08-01 — Supabase TLS proof uses the client connection

- Context: `pg_stat_ssl` observed through Supabase's pooler can describe the pooler's server-side hop rather than the application-to-pooler connection.
- Evidence: psycopg/libpq exposes `PGconn.ssl_in_use` for the actual client connection. A secure
  process-only DSN override returned TLS `true` and PostgreSQL `17.6`; Alembic reached
  `20260801_0004 (head)`. Supabase documents session-mode Supavisor as the supported IPv4
  intermediary and documents that `sslmode=require` always uses SSL:
  https://supabase.com/docs/guides/database/connecting-to-postgres and
  https://supabase.com/docs/guides/platform/ssl-enforcement.
- Decision: Require `sslmode=require` in configuration and health-check client TLS through libpq. Use SQLAlchemy `NullPool` with the session pooler.
- Consequence: Migrations and runtime startup reject an insecure database URL; secrets remain outside
  tracked files. `pg_stat_ssl` behind Supavisor is recorded as the pooler's database-side hop, not
  substituted for the application's libpq TLS observation.
- Owner: WishTrace team / Codex

### 2026-08-01 — Setup collects only facts WishTrace can preserve

- Context: The pre-kickoff editor offered a Photo Picker without server-backed media persistence and
  derived a delivery deadline by subtracting one day from the birthday.
- Evidence: The network recipient contract has no photo field, and the user never supplied that
  delivery rule. Both values would disappear or become an unsupported commerce claim.
- Decision: Remove photo capture until a persisted media contract exists. Store required arrival as
  unknown unless it already came from a real backend record; collect address/deadline just in time
  when a live merchant quote actually requires them.
- Consequence: Recipient initials remain truthful and stable. Deterministic delivery rejection cannot
  run until the user and merchant provide evidence, which is safer than inventing a deadline.
- Owner: WishTrace team / Codex

### 2026-08-01 — Google identity exchanges for an opaque WishTrace session

- Context: Android must authenticate a real user without making Google tokens the long-lived app
  session or placing backend secrets on the phone.
- Evidence: A physical Play-enabled Android 11 phone completed Google account selection and consent,
  the backend consumed one nonce-bound challenge, and the app reached authenticated empty Home.
  Supabase showed one user, one active session and one consumed challenge.
- Decision: Fetch a short-lived backend challenge before Credential Manager, validate the returned ID
  token server-side, consume the nonce once, then return a 24-hour opaque WishTrace bearer token.
  Store only a peppered HMAC server-side and protect the phone copy with Android Keystore AES/GCM.
- Consequence: Replay, wrong audience, expiry, logout and unauthorized recovery have explicit
  boundaries. Stable local/deployed pepper configuration is mandatory before server restart or deploy.
- Owner: WishTrace team / Codex

### 2026-08-01 20:37 PKT — Live physical catalog first; stored value remains gated

- Context: The user wants recognizable Xbox/Steam-style gift cards, while the finalist flow needs a
  real product and Prava-compatible checkout rather than attractive catalog imagery.
- Evidence: The implemented UCP adapter returned 10 live HyperX gaming products with exact variants,
  prices and availability. Turtle Beach returned its own live digital gift card, but no Xbox, Steam
  or Amazon card was observed. Both Shopify UCP profiles omitted the recommended cache header.
- Decision: Keep HyperX physical products as the primary path. Preserve observed gift cards as live
  candidate facts but deterministically reject stored value until an actual merchant + Prava checkout
  proves support. Accept a missing business-profile cache header without caching, record the deviation,
  and reject explicitly unsafe cache directives; keep WishTrace's own agent profile compliant.
- Consequence: Ranking can use only persisted live eligible candidate IDs. Because HyperX's MCP
  endpoint returned `Tool not found` for the official `create_checkout` probe, current candidates are
  stored but rejected as `UNSUPPORTED_CHECKOUT`. Product imagery and prices may appear only from live
  snapshots. Delivery remains unknown, and neither catalog capability nor a Prava session may be
  described as an order.
- Rollback/fallback: Turtle Beach physical products are the backup. Gift cards can be enabled only by
  changing the server-side policy after end-to-end stored-value checkout evidence exists.
- Owner: WishTrace team / Codex

### 2026-08-01 21:04 PKT — Prava credentials cross only an exact, idempotent purchase boundary

- Context: Finalist and Visa evidence require explicit consent, duplicate protection and a truthful
  distinction between hosted approval, one-time credentials and a verified merchant order.
- Evidence: Current Prava documentation exposes hosted session creation, polling and report-status.
  Transport tests prove exact minor-unit conversion, safe response handling and masked credential
  models. Supabase migration `20260801_0006` is live over TLS and matches SQLAlchemy metadata.
- Decision: Snapshot the reviewed merchant, variant and amount before Prava; store only hashes of the
  idempotency key/request; replay a completed create; freeze uncertain creates as `UNKNOWN`; and
  accept `awaiting_result` credentials only when a single line item matches the reviewed merchant
  origin and total. Treat network, 5xx, redirect and malformed-success responses after a create POST
  as uncertain. Discard the session token and never persist card token, CVV or expiry.
- Consequence: Provider approval cannot silently change the basket and `completed` cannot become
  `Gift secured` without an independently verified merchant order. Live purchase routes remain
  unreachable while merchant checkout support is unproven.
- Owner: WishTrace team / Codex

### 2026-08-01 21:33 PKT — Ranking is evidence-linked and cannot bypass checkout truth

- Context: Azure ranking must strengthen the same real transaction path without turning provider
  availability or polished model prose into a substitute for a purchasable product.
- Evidence: The implementation accepts only persisted `LIVE` snapshots that still pass checkout,
  availability, variant, USD and budget checks. Strict-schema and SDK-wire tests pass, Supabase is at
  `20260801_0007`, and Alembic reports no drift. The current merchant snapshots have no verified
  checkout path. A judged-window Azure probe failed DNS before a response because the configured
  Foundry hostname does not resolve.
- Decision: Return `NO_ELIGIBLE_CANDIDATES` before any provider call when hard rules leave no option.
  Send Azure only opaque eligible/evidence IDs and minimal fit text; validate every dynamic ID and
  disallow commerce claims. Repair malformed output once, then use only a high-uncertainty direct-
  evidence fallback or require explicit user choice. Persist evidence and ordered rationales for
  owner-scoped audit, but never persist provider secrets or payment data.
- Consequence: The OpenAI boundary is production-shaped and test-proven, yet no runtime OpenAI success
  is claimed until both the exact Azure endpoint resolves and a real merchant candidate becomes
  checkout-eligible. A model timeout or invalid response cannot create a product or success state.
- Owner: WishTrace team / Codex

### 2026-08-01 22:34 PKT — One $5 digital Jackbox card supersedes physical breadth

- Context: The user has no US shipping address and wants the finished system to be capable of a real
  $5 gift after Prava production access, while backend completion must precede final UX/pitch polish.
- Evidence: Jackbox Games' official Shopify store publishes UCP `2026-04-08` and an observed $5
  digital gift-card variant. A live cart returned exactly 500 USD minor units,
  `gift_card=true`, and `requires_shipping=false`; checkout rendered contact, PCI card and billing
  fields with no shipping address. Jackbox documents email delivery and store/Steam-code redemption.
  Amazon catalog and gift-card APIs require separate program onboarding and credentials, and Turtle
  Beach's observed digital card starts at $50.
- Decision: Make the exact Jackbox $5 SKU the only primary commerce path. Keep runtime checkout and
  stored-value eligibility off by default until Prava confirms the policy. Retire HyperX as the
  primary path for this user; do not add Amazon scraping or a generic gift-card marketplace.
- User benefit: The flagship story becomes concrete—Zaid likes games, a $5 digital gift needs no
  shipping, and the purchaser can forward the emailed card—while keeping one recipient, occasion
  and purchase. WishTrace does not claim that Jackbox sends it directly to the recipient.
- Judging impact: The same path demonstrates deterministic facts, grounded OpenAI ranking, explicit
  Visa/Prava consent, one-use credential handling, a real merchant attempt and truthful recovery.
- Technical consequence: The backend now has an allowlisted Shopify browser actor, peppered quote
  idempotency, exact merchant/Prava state reconciliation, persisted merchant order evidence, and an
  owned editable personal message. A dedicated Windows Proactor worker lets Playwright coexist with
  psycopg's Selector loop, and the production actor passed a live $5 non-payment quote. Supabase
  migration `20260801_0009` is live over verified TLS.
- Cut/fallback: If Prava disallows stored value or Jackbox's regional rules reject the real user, show
  unavailable and select one other observed low-value digital SKU. Never substitute an invented card.
- Owner: WishTrace team / Codex

### 2026-08-01 22:58 PKT — Deploy the browser actor as a locked Azure container

- Context: A normal Python web host can serve FastAPI but does not prove that the exact Shopify
  checkout actor has a compatible Chromium binary and OS dependencies.
- Evidence: Playwright requires a browser revision matching its installed package. Azure Container
  Apps can build a local Dockerfile and expose its target port over managed HTTPS. The repository lock
  currently resolves Python 3.12 and Playwright 1.62.0.
- Decision: Package FastAPI and the Playwright-managed Chromium revision in one Python 3.12 container,
  install from frozen `uv.lock`, and allow an omitted machine-specific browser path. Keep checkout and
  stored-value flags off, exclude secrets from the build context, and require deliberate migrations.
- Consequence: Deployment can execute the same allowlisted browser boundary as local development,
  while cloud resource creation and spend remain pending explicit subscription/cost confirmation.
- Owner: WishTrace team / Codex

### 2026-08-01 23:23 PKT — Use the proven Azure v1 resource endpoint

- Context: The first provider probe used a guessed Foundry hostname and timed out at DNS, while the
  Azure portal and CLI exposed several endpoint families for the same AI Services account.
- Evidence: Microsoft documents both the Azure OpenAI resource endpoint and the AI Services resource
  endpoint with `/openai/v1/` for the OpenAI SDK. Tenant-scoped CLI access showed that
  `gpt-5.6-terra` is the account's only succeeded deployment. The configured
  `services.ai.azure.com/openai/v1/` endpoint then returned a completed strict-schema response in
  3,662 ms with a provider request ID and `store=false`.
- Decision: Keep the existing OpenAI SDK boundary and the proven resource v1 endpoint. Do not add a
  Foundry agent framework, project-scoped API, model fallback or another deployment to this narrow
  flow.
- Consequence: Azure is now live-provider proven. Runtime gift ranking remains gated only by real
  candidate eligibility and Prava's stored-value answer, not by model connectivity.
- Rollback/fallback: A provider outage yields the existing explicit recovery/user-choice state; it
  never creates a candidate or bypasses deterministic rules.
- Owner: WishTrace team / Codex

### 2026-08-02 00:37 PKT — Public Azure staging carries the one exact sandbox path

- Context: The organizer requires an end-to-end in-app Prava sandbox integration and a tokenized
  test-card attempt against a real merchant, explicitly accepting the expected merchant failure.
  The backend and exact $5 Jackbox actor were locally proven, but a private localhost flow could not
  receive Prava's HTTPS return or serve as submission evidence.
- Evidence: Azure Container Apps now serves the locked browser image over managed HTTPS; public
  health observes Supabase TLS, the public UCP profile is cache-compliant, and a real phone session
  reaches authenticated empty Home. Current public Prava docs do not state a stored-value ban, but
  no live sandbox attempt or production permission has yet been observed.
- Decision: Use a single Azure Container Apps revision, managed ACR pull identity and encrypted
  secret references. Keep checkout/stored-value disabled by code default, but enable the exact
  allowlisted Jackbox $5 path on staging solely to collect the organizer-required expected-failure
  sandbox evidence. Do not generalize this into production stored-value support.
- Consequence: Android can now traverse live discovery, grounded Azure ranking, exact quote, hosted
  approval and backend reconciliation without a runtime fixture. Success language remains impossible
  without a verified merchant order; the expected sandbox decline becomes an authorization result.
- Rollback: Set both staging flags false to make the candidate deterministically ineligible without
  changing or redeploying code.
- Owner: WishTrace team / Codex

### 2026-08-02 22:11 PKT — Associate mandates from current Prava facts, never a guessed session field

- Context: The current Prava create-session response returns a hosted session but no mandate ID;
  charge and report responses use camelCase identities, nested one-use credentials and
  `awaiting_result`. The initial mandate implementation assumed older response fields.
- Evidence: Current official Create Session, List Mandates, Charge a Mandate and Report a Mandate
  Charge documentation was checked against the published REST examples. The real sandbox list
  endpoint parsed through the corrected adapter. A physical-phone setup retry also exposed the safe
  provider error `DUPLICATE_EXTERNAL_ORDER_REF` after an earlier created session lost its local row
  to an ORM serialization rollback.
- Decision: After hosted return, discover the mandate only through Prava's customer-scoped list and
  bind it when time, merchant, amount, currency, frequency and scope all match; refuse zero/ambiguous
  facts rather than guessing. Parse the current charge/report identities exactly and keep one-use
  credentials in backend memory only. Give each persisted setup attempt its own external order
  reference while retaining duplicate-tap protection for that attempt.
- Consequence: A hosted session cannot falsely arm an unrelated mandate, and a recoverable retry
  cannot collide with an abandoned provider session. Azure revision `wishtrace-api--pravafcf597f`
  is healthy and has opened a fresh hosted sandbox session on the physical phone. Approval, charge,
  merchant attempt and report remain pending and are not claimed.
- Rollback/fallback: If Prava's list response cannot uniquely identify the current setup, leave the
  mandate awaiting approval and require support/recovery; never select the newest mandate by guess.
- Owner: WishTrace team / Codex

### 2026-08-02 22:34 PKT — A failed hosted setup is not the expected merchant decline

- Context: The first physical-phone hosted mandate attempt displayed card-processing failure.
- Evidence: Prava's official sandbox guide says a valid test-card flow should reach
  `awaiting_result` with one-use credentials. The live payment-result instead returned `failed` with
  `PROVISION_ERROR`; the customer mandate list remained empty and WishTrace had zero charge rows or
  merchant order. `PROVISION_ERROR` is not documented in Prava's current public error table.
- Decision: Reconcile a missing mandate against the hosted session result. Persist `FAILED` for an
  authoritative failed session, use `UNKNOWN` for a success-like session with no unique mandate,
  and permit a fresh failed-session retry. Never count provisioning failure as the organizer's
  accepted tokenized merchant decline.
- Consequence: The app no longer waits forever or implies approval. Revision
  `wishtrace-api--prava6a57af7` renders an honest failure and a clean retry while retaining safe
  provider response evidence.
- Rollback/fallback: If another current documented sandbox card produces the same category, stop
  retrying and contact Prava support; do not broaden the merchant or fake the required proof.
- Owner: WishTrace team / Codex

### 2026-08-02 23:46 PKT — Enforce the organizer request audit and retain safe failure codes

- Context: The organizer supplied a negative-request audit after participants sent invalid session
  parameters. The live sandbox also omitted the documented `authorizeOnly` field and failed card
  provisioning/device binding before producing a mandate or one-use credential.
- Evidence: The exact setup payload is accepted with explicit `integration_type=full_checkout` and
  `mandate_setup.intent=mandate_setup`. A live response omitted `authorizeOnly`; a later official
  payment-result read returned only safe provider categories. The current untouched session remains
  `pending` with zero transactions and credentials.
- Decision: Enforce routable verified email and a bare HTTPS merchant origin at the request model,
  use the real candidate price in the line item, accept only omission/true for `authorizeOnly`, and
  persist a normalized provider failure code for recovery UI. Never persist card data, arbitrary
  provider messages or one-use credentials, and never auto-retry setup.
- Consequence: Revision `wishtrace-api--pravaaudit1` is healthy at 100% traffic, Supabase migration
  `20260802_0012` is live over TLS, and Android can distinguish provisioning from device-binding
  failure without claiming approval. The external Prava setup blocker remains truthful.
- Rollback/fallback: If support cannot reset/verify the organizer card, retain the pending/failed
  state and submit no production-access claim; do not substitute a shared card or fabricate the
  organizer-required merchant attempt.
- Owner: WishTrace team / Codex
