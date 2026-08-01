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
