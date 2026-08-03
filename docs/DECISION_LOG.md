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

### 2026-08-03 07:29 PKT — Freeze on occasion-first bento UX and truthful sandbox proof

- Context: The real sandbox path is complete through Prava mandate authorization, one-time
  credential minting, one Jackbox Shopify submission and the expected processor decline. The
  remaining presentation problem was visual hierarchy: returning sessions flashed onboarding,
  several profile surfaces left dead space, and the month grid read as a generic calendar.
- Decision: Freeze the product on a cool indigo/blue/white bento system. Every tile must expose a
  real recipient, occasion, constraint, source or payment state; bento is composition, not invented
  analytics. Start authenticated sessions directly at Home, keep onboarding to first-entry only,
  make the occasion and countdown lead the calendar, and reserve red for semantic failures.
- Consequence: the 60–90 second story reads person → moment → Gift DNA → grounded choice → bounded
  Prava action → exact sandbox result. Selected Android contact photos remain local, opt-in and
  permissionless through the system picker.
- Evidence: final debug assembly, unit tests and lint pass; the APK is installed on `RMX3201` and
  physical-phone captures cover the top-level shell and recipient setup/detail surfaces.
- Truth boundary: visual polish does not upgrade the sandbox decline into an order. “Gift secured”
  remains reserved for a verified merchant order receipt.
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

### 2026-08-03 00:11 PKT — Reuse one unambiguous enrolled card and expire stale setup state

- Context: Repeated hosted mandate setup entered card provisioning/device binding even though the
  current Prava customer already had a saved enrollment. Separately, a provider-pending session
  remained locally `AWAITING_APPROVAL` after its documented expiry.
- Evidence: Current official Prava docs allow `card.card_id` on session creation and expose
  non-sensitive active enrollments through `GET /v1/listCards`. The live sandbox returned exactly
  one active default enrollment for the current WishTrace customer. The old pending session passed
  its provider expiry with zero transactions and credentials.
- Decision: Before mandate setup, list active cards for the exact WishTrace customer. Preselect the
  sole active default, otherwise the sole active card; if there is ambiguity, send no card ID rather
  than guessing. Persist a still-pending setup as `EXPIRED/SESSION_EXPIRED` once its authoritative
  session deadline passes.
- Consequence: A single fresh setup now reuses the existing enrollment and is truthfully
  `AWAITING_APPROVAL/pending` with zero charges. Azure revision `wishtrace-api--savedcard1` is
  healthy at 100% traffic. User-controlled passkey approval, mandate activation, credential minting
  and merchant checkout remain unclaimed.
- Rollback/fallback: If the saved enrollment still fails device binding, stop creating sessions and
  escalate the safe failure evidence to Prava. Never choose between multiple enrollments or insert
  card credentials into WishTrace.
- Owner: WishTrace team / Codex

### 2026-08-03 00:25 PKT — Retire the failed enrollment and permit only one explicit clean retry

- Context: The sole active saved enrollment returned `DEVICE_BINDING_FAILED` when explicitly
  selected. After it was removed, the organizer-issued team card entered through the hosted Prava
  surface returned `PROVISION_ERROR` and did not create a saved enrollment.
- Evidence: Prava's official card API supports customer-scoped deletion and confirmed the failed
  default enrollment was retired; the subsequent active-card list was empty. Both authoritative
  payment results carried one failed transaction, zero credentials and no mandate. The organizer's
  negative prompt classifies both failures as deterministic and forbids automatic retry.
- Decision: Retire only the exact failed sandbox enrollment after verifying customer, last four and
  failure state. Never put PAN/CVV in WishTrace. Permit one further session solely because the user
  explicitly requested a corrected retry, with no saved card preselected and the phone inputs
  checked manually. Do not retry again on provisioning/device failure.
- Consequence: The final hosted session is pending with zero charges. If it activates, WishTrace can
  run the single organizer-required merchant proof; if it fails, the remaining blocker is external
  Prava card provisioning/device binding rather than an unverified WishTrace request payload.
- Rollback/fallback: On another deterministic setup failure, reconcile it, preserve safe support
  evidence and contact Prava. Do not recreate a session until Prava confirms the card/key binding.
- Owner: WishTrace team / Codex

### 2026-08-03 00:44 PKT — An active mandate is not merchant-proof until credential minting succeeds

- Context: A Birdie-approved public sandbox card passed collection, OTP and Android passkey setup,
  creating an authoritative active mandate. Two local browser preflights then uncovered exact
  product-URL and Shopify region-option mismatches before Prava was charged.
- Evidence: After fixing both mismatches, an isolated real Jackbox quote returned exactly $5 USD.
  The first and only live mandate charge then returned provider transaction
  `txn_01KZ1ZTD34T08SD7QPFS24XFJ1`, status `failed` and undocumented
  `FETCH_AGENTIC_CREDS_ERROR`, with no credential. Current Prava docs describe successful merchant
  callers as receiving plaintext credentials and do not document this error.
- Decision: Preserve the two local preflight failures as audit rows, but do not count them as money
  attempts. Count the provider charge as the single live attempt and do not retry it automatically.
  Escalate the safe mandate/transaction evidence to Prava; only after provider confirmation may a
  new explicit charge proceed to the already-proven Jackbox actor.
- Consequence: WishTrace truthfully proves live approval and mandate activation but does not claim a
  tokenized merchant attempt yet. Product URL and state-code fixes are covered by tests and deployed
  in Azure revision `wishtrace-api--merchantfix1`; the remaining proof blocker is Prava sandbox
  credential minting. The installed hackathon APK exposes proof separately from mandate activation,
  and Home can reopen the authoritative attempt instead of collapsing it into “Done.”
- Rollback/fallback: If Prava cannot mint against this mandate, use a one-time hosted session only
  with explicit user approval and a recorded scope decision; never fabricate credentials or call
  the provider decline a merchant decline.
- Owner: WishTrace team / Codex

### 2026-08-03 01:09 PKT — Fall back from broken mandate minting to a standard hosted session

- Context: The organizer accepts a Prava “session/mandate” approval before the one-time-card merchant
  attempt. The active one-time mandate passed every negative-prompt invariant, but both its initial
  charge and the single docs-sanctioned retry failed inside Prava with
  `FETCH_AGENTIC_CREDS_ERROR`; both left spent `$0.00`, charge count zero and no merchant outcome.
- Decision: Preserve the mandate as live approval evidence and stop charging it. Route the current
  gift through the already-implemented standard hosted Prava session, while reusing the same live
  quote, purchase ledger, memory-only credential boundary, one-attempt Jackbox actor and report API.
- Consequence: The fallback can still satisfy the organizer's explicit session/mandate requirement
  without fabricating a credential or weakening payment controls. It requires one user-controlled
  hosted approval; success is claimed only if a real credential reaches the merchant actor.
- Rollback: Remove the conditional fallback once Prava mandate credential minting is healthy.
- Owner: WishTrace team / Codex

### 2026-08-03 01:22 PKT — Freeze retries after both Prava credential routes fail

- Context: The one-time mandate charge failed twice before credentials, so WishTrace tried the
  organizer-accepted standard hosted-session alternative once. The hosted page verified identity
  but reported that payment could not complete.
- Evidence: Purchase intent `36e40790-751f-451d-9b45-6f3e7b59338c` reconciled to terminal `FAILED`
  with provider status `failed`, no merchant outcome and no order. Its first provider transaction
  reported `FETCH_AGENTIC_CREDS_ERROR`. Reopening the same existing session did not create a second
  session and produced `FIDO_START_FAILED`. Neither route supplied a credential or contacted Jackbox.
- Decision: Stop all Prava charge/session attempts. Keep the purchase intent immutable, treat
  `FAILED` as non-recoverable, route Home to the existing status, and expose only `Done` on terminal
  payment states. Do not turn the provider failure into the organizer's accepted merchant decline.
- Consequence: The app remains truthful and duplicate-safe, but it cannot claim tokenized merchant
  checkout, Visa confirmation, an order or a receipt. UX/submission work proceeds around the
  strongest proven boundary.
- Rollback/fallback: Re-enable a payment action only after Prava supplies a verified credential-mint
  fix and the existing audit facts have been reviewed. Never create speculative retries.
- Owner: WishTrace team / Codex

### 2026-08-03 01:54 PKT — Never let a stale default card choose the mandate

- Context: The provider mandate remained `active`, `$0.00` spent and zero completed charges after
  `FETCH_AGENTIC_CREDS_ERROR`. A read-only live card list then showed two active enrollments: the
  previously troublesome card ending `7789` was still marked default, while the card ending `7912`
  that completed OTP/passkey enrollment was non-default.
- Evidence: The request matches Prava's current documented mandate setup and `{amount, reference}`
  charge contracts. Current error docs classify credential-mint failure as `NO_TOKEN`, with one
  retry followed by support. WishTrace had already made that one bounded retry.
- Decision: Preselect `card_id` only when exactly one active enrollment exists. With multiple active
  cards, omit `card` and require the owner to choose in Prava's hosted surface. Preserve every prior
  mandate/charge row, and bind checkout to an exact verified product/variant and unchanged live total.
- Consequence: A fresh explicit mandate can select `7912` without hardcoding or exposing card data.
  This is the remaining code-side recovery; minting and merchant proof remain unclaimed until observed.
- Rollback/fallback: If a fresh `7912` mandate still returns `NO_TOKEN`, stop. The blocker is inside
  Prava/card-network provisioning and requires the recorded X-Response-ID; do not generate retries.
- Owner: WishTrace team / Codex

### 2026-08-03 02:33 PKT — User owns hosted passkey verification

- Context: A real phone setup reached Prava's security step but returned `FIDO_START_FAILED`. A
  bounded Playwright diagnostic reached Visa OTP and its Secure Payment Confirmation popup, then
  returned `AUTH_FAILED` without creating a mandate, credential, charge or merchant request.
- Evidence: Organizer support confirmed that the hosted Browser Harness is unavailable in sandbox;
  teams build the post-mint browser automation themselves. WishTrace already owns that exact-product
  Playwright and report-status boundary. Hosted passkey approval remains a real user ceremony.
- Decision: Stop agent-driven hosted verification. Keep the backend transaction path unchanged and
  let the user perform phone approval. Android may offer a fresh session only after an explicit tap
  for passkey start/authentication failures; provisioning and mint failures remain terminal.
- Consequence: Testing returns to the intended division of responsibility, with exact failure copy
  and no automatic approval, credential, charge or merchant retries.
- Rollback/fallback: None. If the user-approved mandate becomes active, run one merchant proof. If
  it does not, preserve the provider result and proceed truthfully to UX/submission work.
- Owner: WishTrace team / Codex

### 2026-08-03 02:55 PKT — One approval, automatic bounded execution

- Context: The user completed hosted approval and Prava minted a one-time credential, but Jackbox's
  total changed after the initial quote and before Pay. Repeated taps did not submit twice, although
  the UI made the idempotent conflict look like another action was possible.
- Evidence: The live ledger has one `$9.99` charge with `MERCHANT_TOTAL_CHANGED`, a provider charge
  reference, no merchant outcome and no order. Prava lists the approved `$10` mandate as active.
- Decision: Treat the mandate cap—not sticker price—as the authorization boundary; stabilize and
  record the tax-inclusive quote before minting. Use a mandate-derived idempotency key across taps
  and process recreation, reconcile errors immediately, and start the sandbox merchant proof
  automatically when an approved mandate becomes active.
- Consequence: The user approves once for this exact bounded purchase. There is no second proof
  button and no repeated passkey. A different gift, higher cap, expired mandate, or future yearly
  authorization still requires explicit consent; WishTrace never interprets one approval as forever.
- Rollback/fallback: Any unknown post-mint state locks for reconciliation. A total above the cap
  stops before credential minting and shows the exact live total and approved limit.
- Owner: WishTrace team / Codex

### 2026-08-03 02:55 PKT — Keep Prava hosted approval in a secure Custom Tab

- Context: The user asked why approval leaves the Compose surface. Official Prava embedded mode is
  delivered by the browser-only `@prava-sdk/core` JavaScript package and a secure iframe; Prava does
  not document a native Android SDK.
- Decision: Keep hosted approval in an Android Custom Tab. Do not wrap the iframe in an unsupported
  WebView or collect card/passkey material in Compose.
- Consequence: Prava retains its secure origin, browser passkey support and PCI boundary. WishTrace
  makes the transition feel cohesive through its review, return, automatic progress and result UI.
- Rollback/fallback: Reconsider only if Prava publishes and verifies a native Android SDK or a
  supported Android embedded contract.
- Owner: WishTrace team / Codex

### 2026-08-03 03:06 PKT — Existing mandate state outranks a new recommendation

- Context: Home only routed an `ACTIVE` mandate to Autopilot. A locally stale approval, processing
  state, blocked proof or recorded decline could therefore reopen discovery and suggest the gift
  again even though the occasion already owned an auditable mandate.
- Decision: Refresh an existing occasion mandate against Prava and route every non-null mandate to
  its Autopilot state from both Home and recipient detail. Enter discovery only when the occasion has
  no mandate. Keep the automatic proof keyed by the mandate ID so repeated taps cannot create a new
  money-adjacent operation.
- Consequence: The approved gift resumes in place, state recovery is visible, and the user is never
  asked to choose or approve the same bounded purchase merely because local status was stale.
- Rollback/fallback: A deliberate “choose another gift” action can be added inside Autopilot later;
  it must be explicit and preserve the existing audit record.
- Owner: WishTrace team / Codex

### 2026-08-03 03:17 PKT — An unresolved merchant attempt is sticky

- Context: Prava minted a one-time credential and Jackbox Pay was clicked once, but the browser
  observed neither a verified order nor an explicit decline within 45 seconds. The charge correctly
  became `UNKNOWN`; a later Prava refresh only knew the authorization was still active and moved the
  parent mandate back to `ACTIVE`, creating a misleading disabled “Starting…” screen.
- Decision: Local post-mint charge truth outranks provider authorization availability. An unknown
  charge forces the parent mandate to `UNKNOWN` across refreshes, blocks every new operation key, and
  renders an honest recoverable result rather than starting another card or merchant attempt.
- Consequence: No duplicate money-adjacent action can occur. The submission may claim approval,
  credential minting and one tokenized merchant submission, but not a decline, Visa confirmation,
  order or receipt.
- Rollback/fallback: Reconcile only from new authoritative merchant/Prava evidence. Never clear the
  unknown state merely because the mandate remains active.
- Owner: WishTrace team / Codex

### 2026-08-03 03:34 PKT — Freshness is deterministic, not random

- Context: Re-entering discovery after an unresolved purchase could rank the same valid product
  again, making the recovery feel stuck even while other live products remained eligible.
- Decision: Record prior product identity through the occasion's immutable mandate history. Apply
  every checkout, availability, variant, budget and dislike constraint first, then reject a prior
  selection as `RECENTLY_ATTEMPTED` only when at least one fresh candidate already passed those
  constraints. Never alter price facts or invent products to force novelty.
- Consequence: Each subsequent choice leads with a genuinely different live gift while alternatives
  remain. Once the verified catalog is exhausted, WishTrace honestly permits the valid prior item
  instead of showing fake variety or an artificial no-results state.
- Rollback/fallback: Remove only the freshness preference if merchant product identity proves
  unstable; keep the immutable mandate audit and all hard commerce filters.
- Owner: WishTrace team / Codex

### 2026-08-03 03:48 PKT — Reconcile conflicts automatically; replace unknown only in sandbox

- Context: After choosing a fresh product, setup returned `MANDATE_ALREADY_EXISTS` with an
  instruction for the user to refresh manually. The old post-mint attempt was correctly locked
  `UNKNOWN`, but the explicit recovery had not carried its identity into the new setup.
- Decision: On an ordinary setup conflict, Android immediately refreshes the existing mandate and
  renders the authoritative state. For the organizer's no-money sandbox only, an explicit
  `Choose another gift` may create a separate approval when it names the exact latest mandate, its
  latest minted provider charge is `UNKNOWN`, and the replacement has a different verified product
  ID. Keep this path disabled against Prava production.
- Consequence: The user never performs a mechanical refresh, stale state cannot create a second
  action, and the sandbox proof can continue without overwriting or retrying the unknown credential.
  The earlier mandate remains immutable evidence. A production unknown must reconcile or be revoked
  with explicit Prava authentication before another purchase is allowed.
- Rollback/fallback: Disable the sandbox replacement flag and retain automatic refresh. Never widen
  the rule to production or allow the same product through it.
- Owner: WishTrace team / Codex

### 2026-08-03 04:08 PKT — One explicit pre-merchant mint retry, never automatic

- Context: The user completed a new Quiplash 2 approval, but Prava returned
  `FETCH_AGENTIC_CREDS_ERROR` before issuing a one-time credential. The provider mandate remains
  active, reports zero completed charges, and no transaction reference, merchant outcome or order
  exists. WishTrace refreshed Jackbox's live quote but stopped before payment submission.
- Decision: Permit exactly one explicit retry under the same approval only when the latest charge is
  the sole recorded pre-credential mint failure, the provider mandate is still active,
  `total_charges` is below the approved maximum, and no transaction or merchant evidence exists.
  Use a distinct deterministic idempotency key; create no new approval and trigger no passkey. Never
  apply this recovery to a minted credential, merchant attempt or `UNKNOWN` result.
- Consequence: A transient Prava mint failure can recover without asking the owner to repeat card and
  passkey setup, while duplicate or ambiguous money-adjacent actions stay impossible. If the second
  invocation fails before credentials, the action disappears permanently and the provider blocker
  is reported truthfully. This immediate action is limited to the explicit sandbox proof; an armed
  production mandate remains permission for later scheduled revalidation and purchase, not a charge.
- Evidence: 160 backend tests, Ruff, strict mypy and the Android assemble/unit/lint gates pass. Azure
  revision `wishtrace-api--mintretry1` is healthy at 100% traffic; its OpenAPI contract exposes the
  retry flag. The matching APK is installed. The user-triggered retry returned the same
  `FETCH_AGENTIC_CREDS_ERROR`, and the phone now renders only the terminal `Done` action.
- Rollback/fallback: Disable the retry flag without changing the immutable ledger. Do not add a
  third retry or a new approval for the same failed mint.
- Owner: WishTrace team / Codex

### 2026-08-03 04:08 PKT — Freshness includes recommendations, not only purchase attempts

- Context: Two consecutive discovery runs could choose the same primary product when the first
  recommendation never advanced into a mandate. Mandate history alone therefore could not prevent a
  back/re-enter loop from feeling repetitive.
- Decision: Treat prior persisted rank-position primary selections and prior mandate products as the
  same freshness evidence. Apply all factual constraints first and recede seen products only when a
  different eligible live candidate exists.
- Consequence: Re-entering discovery leads with a fresh verified gift while catalog breadth remains,
  without random ranking, fabricated products or a false empty state after exhaustion.
- Rollback/fallback: Remove the ranking-history input if merchant product identity proves unstable;
  retain deterministic hard constraints and honest reuse after catalog exhaustion.
- Owner: WishTrace team / Codex

### 2026-08-03 04:34 PKT — Exhausted mint recovery requires a fresh owner-approved card choice

- Context: The newest mandate invocation and its sole explicit retry both stopped before credentials
  with `FETCH_AGENTIC_CREDS_ERROR`. The user-provided Prava dashboard, whose timestamps are IST,
  separately shows the matching authorization/credential/failure stages. The official Prava skills
  repository confirms the implemented charge and report contracts and forbids treating credential
  generation as a completed merchant transaction.
- Decision: Never create a third charge under the exhausted recovery. In sandbox tools only, expose
  a fresh live-gift route; a later attempt creates a new hosted approval where the owner explicitly
  selects a different approved sandbox card. Keep the hosted ceremony in a real Custom Tab, not an
  embedded Android WebView, and never insert or retain card data in WishTrace.
- Consequence: The user can recover without being trapped or repeating the same failing operation.
  Home shows `Last attempt failed` and reopens the exact recovery after restart. The existing
  credential-generated rows remain evidence of minting only, and the unresolved merchant run
  remains `UNKNOWN`.
- Evidence: official repository commit `f4d7515`; focused Android state test; debug APK assembly,
  unit tests and lint; successful physical-phone install and
  `artifacts/screenshots/prava-exhausted-card-recovery-2026-08-03.png`.
- Rollback/fallback: Hide the sandbox recovery control and retain `Done`. Do not weaken the immutable
  ledger, add another retry, or claim a merchant decline without merchant evidence.
- Owner: WishTrace team / Codex

### 2026-08-03 04:40 PKT — Treat FETCH_AGENTIC_CREDS_ERROR as an exhausted sandbox card

- Context: Prava support received another participant's identical sequence—card, OTP and passkey
  succeeded, followed by `FETCH_AGENTIC_CREDS_ERROR / Visa 400 — Fetching cryptogram failed`—and
  Shubham Kukreti stated that the card was exhausted and arranged a fresh card by email.
- Decision: Classify this exact sandbox error as card exhaustion at the provider credential-mint
  boundary. Do not change merchants, repeat the same mandate charge, reuse a shared public test card,
  or alter the documented request body in response. Recovery requires a fresh Prava-issued card and
  a new owner-approved hosted setup.
- Consequence: The WishTrace charge contract remains unchanged and the terminal no-third-retry UI is
  correct. Existing failures remain pre-merchant; they are not merchant declines, card charges or
  orders. UI work can proceed without pretending a software change can mint against an exhausted
  sandbox card.
- Rollback/fallback: Revisit only if Prava supplies contradictory account-specific evidence tied to
  a safe response ID. Keep the immutable ledger and fail-closed behavior either way.
- Owner: WishTrace team / Codex

### 2026-08-03 05:20 PKT — Revoke old card-bound authority before a fresh-card setup

- Context: Selecting another recommendation did not open Prava. The backend correctly rejected a
  second setup because the latest Zaid mandate remained `ACTIVE`; Android then reconciled that old
  card-bound mandate and returned to `Checkout stopped safely`. A fresh card therefore could never
  enter the flow.
- Decision: On explicit replacement, call Prava's documented no-body
  `POST /v1/mandates/{id}/cancel`, then persist `CANCELLED` without deleting the mandate or charge
  ledger. Permit this only for owned `ACTIVE`, `DECLINED` or `PAUSED` mandates with no unresolved,
  successful or merchant-result-bearing charge. Mark the next setup as requiring a fresh card and
  omit any stored `card_id`, forcing Prava's hosted owner-choice surface.
- Consequence: The old exhausted enrollment cannot be silently reused, and an ambiguous merchant
  attempt still cannot be discarded. A successful provider cancellation makes the normal
  `CANCELLED` state replaceable by a new candidate-specific approval.
- Evidence: commit `c9c9b67`; 166 backend tests, Ruff and strict mypy pass; Android unit tests,
  assembly and lint pass; ACR run `chm` produced digest
  `sha256:fd514172a7e48fdd4db5e354a220218f5b34aae05a07fa5c31049ba590d163e0`;
  healthy revision `wishtrace-api--freshcard1` serves 100% traffic and its public OpenAPI exposes
  both cancellation and `require_fresh_card`. The APK installed without clearing app data.
- Truth boundary: deployment is verified; the user has not yet invoked the new cancellation or
  created the fresh hosted approval. No new payment or merchant result is claimed.
- Owner: WishTrace team / Codex

### 2026-08-03 05:45 PKT — Classify only explicit Shopify payment failure across trusted frames

- Context: A fresh card completed Prava mandate authorization and one-time credential minting, and
  the Jackbox actor clicked Pay once. Its 45-second observer did not match an order or one of five
  decline phrases, leaving the exact charge `UNKNOWN`. A later no-card/no-Pay checkout probe showed
  Shopify's visible alert region, cross-origin payment frames and current `Payment failed` locale
  label. The old browser page no longer exists, so its exact rendered message cannot be recovered.
- Decision: Preserve that immutable `UNKNOWN`. For future attempts, normalize punctuation and accept
  only explicit payment-level failure/decline language from the main Jackbox checkout or exact
  allowlisted Shopify payment hosts. Do not classify card-field validation, generic connection text
  or a timeout as decline, and do not automatically retry Pay.
- Consequence: A future observed Shopify payment failure can be reported once to Prava as
  `DECLINED`, while ambiguous outcomes remain locked. The observer waits up to 60 seconds to cover a
  slow sandbox response without repeating the money-moving click.
- Evidence: commit `220a42c`; backend pytest 178/178, Ruff and strict mypy; Android debug assembly,
  unit tests and lint; ACR run `chn` digest
  `sha256:aa251e05bf1fafc30ed26a46ea3744e38fa5efe4f0c859caf69e6ff07e074fb3`;
  healthy Azure revision `wishtrace-api--shopifyfail1` at 100% traffic with PostgreSQL TLS true.
- Truth boundary: classifier and deployment are verified. No post-deployment credential, merchant
  attempt, decline report, Visa confirmation or order is claimed.
- Owner: WishTrace team / Codex

### 2026-08-03 06:08 PKT — Consume Shopify's structured completion receipt in memory

- Context: The next user-approved Quiplash 2 run executed on the UI-failure observer revision and
  still ended `UNKNOWN` after 82.7 seconds. The immutable ledger proves a fresh mandate, one Prava
  credential and one Pay click. Current Shopify Checkout One code shows that completion is resolved
  asynchronously through `SubmitForCompletion` and `PollForReceipt`; a processing receipt need not
  expose final failure copy within 60 seconds.
- Decision: Observe only allowlisted checkout-host fetch/XHR responses after the single Pay click.
  Parse at most 2 MB in browser memory, retain only `DECLINED`/`PROCESSING`, and never log or persist
  the response. Accept only explicit submit failure, failed payment receipt or a current payment
  failure code. Continue UI evidence in parallel and extend the receipt boundary to 120 seconds,
  below Android's existing 180-second read timeout.
- Consequence: The next run can report the merchant's own structured decline even when the rendered
  error is delayed or lives outside the top document. A processing, challenge, malformed response,
  HTTP failure or non-payment validation still stops safely as `UNKNOWN`; Pay is never retried.
- Evidence: commit `d93b93c`; backend pytest 185/185, Ruff and strict mypy; Android debug assembly,
  unit tests and lint; ACR run `chp` digest
  `sha256:2ffea3b56418fe16e1d19f896ffe71a30fd2f06553f43c5057338d937491e746`;
  healthy Azure revision `wishtrace-api--completion1` at 100% traffic with PostgreSQL TLS true.
- Truth boundary: deployment is verified; no post-deployment credential, merchant attempt, decline
  report, Visa confirmation or order is claimed.
- Owner: WishTrace team / Codex

### 2026-08-03 08:08 PKT — A settled decline may start a fresh explicit gift journey

- Context: Zaid's organizer-proof attempt correctly settled the one-time mandate as `CONSUMED`
  after the merchant declined it. The backend treated every consumed mandate as non-replaceable,
  while Android retained the previous `ReadyForRanking` state, so **Find another gift** could reopen
  the same recommendation and then fail to create a fresh Prava approval.
- Decision: Preserve the entire mandate and charge ledger. Permit replacement only when the latest
  mandate is `CONSUMED`, both mandate and latest charge carry `DECLINED`, and no merchant order ID
  exists. Clear Android's in-memory discovery and selected candidate before a user-requested fresh
  journey. Keep explicit gift selection and **Arm autopilot** before hosted approval.
- Consequence: A proven failed purchase no longer traps the occasion, while success and uncertainty
  remain fail-closed. The next journey retrieves new live candidates and can open a fresh Prava
  hosted approval without deleting or falsifying the completed sandbox evidence.
- Evidence: read-only TLS database inspection; 188 backend tests; Ruff; strict mypy; Android focused
  and full unit tests, assembly and lint; ACR run `chr` digest
  `sha256:23a78c8c41888a3ab60527483f47d482518c38be71094f328624cdb91ae5c8c1`;
  healthy `wishtrace-api--giftreset1` at 100% traffic; APK installed on `RMX3201`.
- Truth boundary: deployment and state reset behavior are verified. No new Prava session, credential,
  merchant submission or transaction result is claimed until the user explicitly runs the flow.
- Owner: WishTrace team / Codex

### 2026-08-03 08:20 PKT — Apply the multi-recipient schema gate before accepting Add Person

- Context: Android reached the final Gift DNA save action but showed “We couldn't save this.” The
  deployed API supported multiple recipients, while Supabase was still on `20260803_0014` and kept
  the earlier `uq_recipients_user_id` constraint.
- Decision: Apply the existing `20260803_0015` migration transactionally. Drop only the obsolete
  unique constraint; do not delete or rewrite any recipient, occasion or transaction history.
- Consequence: the same authenticated owner can persist another real person and occasion through
  the existing API. The prior Zaid data and completed sandbox evidence are unchanged.
- Evidence: Alembic current is `20260803_0015 (head)`; schema drift check is clean; seven context
  tests pass; the database probe reports PostgreSQL 17.6 over TLS.
- Truth boundary: migration behavior is verified, while a post-migration physical-phone save still
  awaits the user's real input.
- Owner: WishTrace team / Codex

### 2026-08-03 08:39 PKT — Give People its own collection state

- Context: a second recipient saved successfully after the schema migration but could not appear in
  People because that route consumed Home's one-recipient upcoming snapshot.
- Decision: load the owned recipient collection through a dedicated immutable People state and
  refresh it on sign-in, contact-photo update and person save/edit. Do not broaden Home's focused
  snapshot or fabricate an occasion summary for a recipient card.
- Consequence: all persisted people are visible immediately, while Home keeps its five-second
  next-occasion hierarchy. Recipient cards remain informational until a recipient-ID detail route is
  implemented rather than navigating every card to the wrong Home recipient.
- Evidence: focused ViewModel tests; full Android unit, assembly and lint gates; physical-device UI
  hierarchy and screenshot showing two distinct saved people.
- Truth boundary: recipient identity and preference cues are backend-backed; no product or payment
  state is implied by the list.
- Owner: WishTrace team / Codex

### 2026-08-03 08:47 PKT — Serialize logout and refresh context on session identity changes

- Context: after an in-process logout and Google re-sign-in, Home could retain “We lost the thread”
  even though Azure returned successful authentication and context responses. A process restart
  loaded the same real data, proving a client lifecycle race rather than lost persistence.
- Decision: complete logout and credential clearing before Welcome navigation; make bearer-token
  change the refresh trigger for Home and People; set sign-in working state synchronously before
  launching account discovery.
- Consequence: old-session navigation can no longer pull the app back into Home during logout, and
  a new verified session always invalidates stale context UI without requiring a restart.
- Evidence: 43 Android unit tests, debug assembly, lint, in-place physical-device installation and
  post-install Home content verification.
- Truth boundary: backend data was never missing or recreated; this is client session recovery.
- Owner: WishTrace team / Codex
