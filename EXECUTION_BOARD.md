# WishTrace Execution Board

Update this file during the hackathon. Do not manage the project from memory.

## Current gates

| Gate | Evidence required | Status | Owner | Last checked |
|---|---|---|---|---|
| Official-window baseline frozen | secret scan + preexisting commit | PASS — `283f5be` | Codex | 2026-08-01 14:29 PKT |
| Supabase TLS + migration | client TLS true + Alembic head | PASS — TLS, PostgreSQL 17.6, `20260801_0001` | Codex | 2026-08-01 |
| Backend foundation | pytest + Ruff + mypy + health/UCP tests | PASS LOCALLY — public deploy pending | Codex | 2026-08-01 |
| Prava auth works | smallest official sandbox request | PRE-KICKOFF ONLY — REVERIFY | Codex/user | 2026-08-01 |
| Prava transaction path understood | session + authoritative status | NOT STARTED | | |
| Primary merchant validated | search/product/quote/checkout facts | NOT STARTED | | |
| Backup merchant validated | documented fallback | NOT STARTED | | |
| OpenAI structured output works | valid live candidate IDs returned | PRE-KICKOFF SMOKE ONLY | Codex/user | 2026-08-01 |
| Android Compose foundation builds | debug APK + unit tests + lint | PASS BASELINE — runtime fixtures remain | Codex | 2026-08-01 |
| Android onboarding + home build | routed screens + state handling + build evidence | PASS — UI MILESTONE 2 | Codex | 2026-07-30 17:36 PKT |
| Android recipient context build | editable recipient + occasion + validation + tests | PASS — UI MILESTONE 3 | Codex | 2026-07-30 19:02 PKT |
| Android grounded decision build | discovery + recommendation + message + recovery | PASS — UI MILESTONE 4 | Codex | 2026-07-30 19:34 PKT |
| Android device UX evidence | API 31 route tests + screenshots + text scale + contrast | PASS — UI MILESTONE 3 | Codex | 2026-07-30 19:02 PKT |
| Android return flow works | approval → app → reconciled state | NOT STARTED | | |
| Bronze flow repeats twice | full device demo | NOT STARTED | | |
| Five-second UX understood | fresh viewer explanation | NOT STARTED | | |
| Clean build works | fresh checkout command log | NOT STARTED | | |
| Submission uploaded | links verified externally | NOT STARTED | | |

## Now

1. Finish and deploy the secure FastAPI foundation.
2. Implement real Google challenge/exchange and Supabase-owned recipient/occasion persistence.
3. Replace every Android runtime seed/local-session path with authenticated network state.
4. Prove one live merchant path before ranking or payment UI claims.
5. Preserve the pre-kickoff prototype disclosure and never use fixtures as judged runtime fallback.

## Next

1. Reverify Prava sandbox auth during the official window.
2. Prove HyperX US catalog/search/lookup; try Turtle Beach only if the 90-minute gate fails.
3. Rank only live eligible candidates with Azure OpenAI.
4. Run hosted Prava approval and the organizer-required real-merchant browser attempt.
5. Repeat the physical-phone flow twice, then apply for production access.

## Milestone evidence

### 2026-08-01 — Judged-window security and backend foundation

- Kickoff: local clock verified after the official opening.
- Baseline: initialized Git after a tracked-file secret scan and committed all disclosed pre-kickoff work as `283f5be`.
- Backend: FastAPI/Pydantic v2, async SQLAlchemy + psycopg 3, Alembic, correlation/error middleware, audit-safe request logging, `/health`, and catalog-only `/.well-known/ucp`.
- Database: application-to-pooler TLS verified through libpq `ssl_in_use`; observed PostgreSQL 17.6; Alembic upgraded to `20260801_0001 (head)` using `NullPool`.
- Backend quality: pytest 7 passed; Ruff passed; strict mypy passed. These commands must be rerun after the final foundation diff.
- Android baseline: debug assembly, unit tests and lint passed during the official window. Physical RMX3201/API 30 with Google Play Services is ADB-authorized for real OAuth testing.
- Track evidence: live Devfolio page verified the Open Finalists, Visa and Best UX prizes; the proof strategy now uses one shared real transaction path.
- Truth boundary: the backend foundation and database migration are live. Public Azure deployment, real auth, runtime persistence, merchant calls, grounded ranking and judged-window Prava calls are not yet complete. Current Android runtime fixtures remain pre-kickoff technical debt and are not acceptable demo evidence.

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
