# WishTrace — AGENTS.md

This file governs Codex, Claude Code and any other coding agent working in the WishTrace repository. It is not optional guidance.

## 1. Mission

Build one reliable, visually distinctive Android flow that turns recipient and occasion context into a grounded gift decision and a verified Prava transaction.

The working product matters more than architectural ambition.

## 2. Authority order

Before changing code, use this precedence:

1. Current official Prava documentation and observed API behavior.
2. Latest organizer announcement or support response.
3. Builder Handbook rules and hard deadline.
4. `PLAN.md`.
5. This file.
6. Relevant repository-local skill.
7. Existing code and comments.
8. Generated concepts and old notes.

When a higher source conflicts with a lower one, update the lower document and record the decision.

## 3. Required reading before a task

Always inspect:

1. `PLAN.md`
2. `TOOLKIT.md`
3. `docs/INTEGRATION_STATUS.md`
4. `docs/DECISION_LOG.md`
5. relevant `.agents/skills/**/SKILL.md`
6. repository tree
7. current git diff
8. build files and environment examples
9. tests around the target area

Never assume a file, command, endpoint, SDK method, model or merchant capability exists.

## 4. Core product invariant

Every meaningful feature must strengthen this loop:

```text
recipient + occasion context
→ real candidates
→ deterministic filtering
→ grounded model ranking
→ explicit user review
→ Prava transaction
→ verified result
→ personal message
```

Reject features that turn WishTrace into a generic chat assistant, marketplace browser or calendar clone.

## 5. Truthfulness rules

Never:

- invent products, prices, stock, variants, merchants or delivery dates;
- describe a directory listing as verified merchant support;
- present generated UI as a functioning screen;
- present a payment session as a completed order;
- present `pending` or unknown as success;
- label controlled data as live;
- fake a Prava response and call it real;
- hide manual steps in the demo;
- claim production support from sandbox behavior;
- claim a real card transaction without evidence;
- claim audio was sent when only local playback exists;
- invent user traction, interviews or transactions.

Controlled or seeded components are allowed only when clearly labeled in code, docs and narration.

## 6. Scope discipline

### The MVP needs

- one giver;
- one recipient;
- one occasion;
- interests, dislikes, budget and optional hints;
- one commerce path;
- a small candidate set;
- deterministic rejection;
- structured ranking;
- exact review screen;
- one Prava sandbox transaction;
- one final result;
- one personal text message;
- one memorable motion sequence.

### The MVP does not need

- group gifting;
- pooled money;
- arbitrary unattended spending;
- cross-merchant baskets;
- social feed;
- recipient account;
- refunds;
- subscriptions;
- every occasion type;
- more sponsor integrations;
- production payment unless the stable sandbox flow is complete.

When uncertain, remove scope.

## 7. Work protocol

For each task:

1. Restate the outcome in one sentence.
2. Inspect current code and relevant docs.
3. Identify the smallest vertical change.
4. State assumptions and external dependencies.
5. Implement in a reversible slice.
6. Run targeted tests.
7. Run the build or lint relevant to touched files.
8. Capture evidence of behavior.
9. Update status/decision docs if truth changed.
10. Report changed files, commands, evidence and remaining risk.

Do not make broad speculative refactors during the hackathon.

## 8. Git rules

- Keep changes small and reviewable.
- Do not overwrite user work.
- Do not reset, clean or delete files without explicit approval.
- Inspect the diff before and after changes.
- Avoid formatting unrelated files.
- Prefer one coherent commit per vertical slice.
- Never commit `.env`, keys, credentials, card data or personal information.

Suggested commit prefixes:

```text
feat(android):
feat(backend):
feat(prava):
feat(agent):
fix:
test:
docs:
```

## 9. Architecture contract

Expected boundary:

```text
Android client
    ↓ HTTPS
WishTrace backend
    ├── persistence
    ├── commerce adapter
    ├── deterministic validator
    ├── OpenAI orchestrator
    ├── Prava adapter
    └── transaction ledger/webhook handler
```

### Android owns

- presentation and motion;
- form input;
- navigation;
- local non-secret state;
- opening hosted approval/custom tab;
- deep-link/app-link return;
- recovery UI;
- accessibility.

### Backend owns

- all secret keys;
- model calls;
- merchant credentials;
- product normalization;
- hard constraint validation;
- Prava session creation;
- idempotency;
- webhook verification;
- authoritative transaction state;
- audit-safe logs.

The Android client must never contain OpenAI or Prava secret keys.

## 10. Domain modeling

Use explicit types for:

- `Recipient`
- `Occasion`
- `Preference`
- `Hint`
- `Money`
- `ProductCandidate`
- `CandidateRejection`
- `RankedDecision`
- `PurchaseIntent`
- `PravaSession`
- `TransactionState`
- `PersonalMessage`

Money must use integer minor units or a decimal-safe type. Never use binary floating point for totals.

Dates and time:

- store UTC instants when an instant is intended;
- store local date and timezone for birthdays and delivery deadlines;
- do not silently convert a birthday into UTC midnight.

## 11. Commerce and merchant rules

A commerce adapter must return normalized candidates with at least:

- stable candidate ID;
- merchant ID/name;
- title;
- current price and currency;
- product URL or checkout reference;
- availability state;
- variant requirements;
- delivery estimate or explicit unknown;
- source timestamp;
- source mode: live, controlled, hybrid or simulated.

Before payment:

1. refresh product data;
2. compare current price to approved price;
3. verify merchant and variant;
4. verify availability;
5. verify delivery rule if claimed;
6. recalculate total;
7. require re-approval if material facts changed.

A merchant directory is a lead, not proof.

## 12. OpenAI rules

The model may:

- extract structured interests from user-provided hints;
- map hints to categories;
- rank supplied candidate IDs;
- explain evidence links;
- draft a personal message;
- express uncertainty or refuse when evidence is weak.

The model may not:

- invent a candidate;
- change price or currency;
- override budget, merchant, stock, variant or deadline rules;
- see card/payment credentials;
- create the final authoritative transaction state;
- claim a delivery promise not present in source data.

Use structured outputs. Validate every returned ID and enum. If validation fails, retry once with a repair instruction, then fall back to deterministic ranking or user choice.

Keep prompts and schemas versioned. Log model request IDs and latency where safe, but never log private hint content unnecessarily.

## 13. Deterministic decision order

Use this order:

1. Reject unsupported merchant or checkout path.
2. Reject unavailable product.
3. Reject missing required variant.
4. Reject over-budget total.
5. Reject late delivery when a reliable deadline exists.
6. Reject excluded categories or explicit dislikes.
7. Pass remaining candidates to model ranking.
8. Validate model output.
9. Show one recommendation and at most two alternatives.

The model decides preferences only after code enforces safety and factual constraints.

## 14. Prava rules

Use SDK/API by default for the native product unless current docs prove another path is better.

Required states:

```text
DRAFT
VALIDATING
READY_FOR_APPROVAL
SESSION_CREATING
AWAITING_USER
PROCESSING
SUCCEEDED
DECLINED
CANCELLED
EXPIRED
FAILED
UNKNOWN
```

Rules:

- All create/charge/order operations require idempotency.
- Disable repeated primary actions while a request is active.
- Do not silently retry a money-moving action.
- Reconcile return/deep-link state against the backend.
- Treat webhook/API authoritative state as source of truth.
- Show `UNKNOWN` honestly and provide refresh/recovery.
- Persist correlation IDs and Prava identifiers needed for support.
- Do not store card data.
- Verify webhook authenticity using the current official mechanism.
- Do not hardcode undocumented SDK methods from memory.

A completed demo requires more than session creation. Show the strongest authoritative result available and describe it precisely.

## 15. Android and Compose rules

- Use Material 3 foundations, not a generic third-party UI kit.
- Prefer stateless composables with immutable UI state.
- Use ViewModels and flows for screen state.
- Model loading, content, empty, error and recovery explicitly.
- Use one navigation route per meaningful screen.
- Handle process recreation where it affects payment return.
- Use Compose previews for isolated components.
- Test on a physical phone before submission.
- Avoid blocking animations and long cinematic onboarding.
- Avoid desktop patterns, tiny text and hover-dependent interactions.

Minimum accessibility:

- 48dp touch targets;
- semantic labels;
- contrast suitable for light mode;
- scalable text;
- no information conveyed only by color;
- reduced-motion-safe behavior where practical.

## 16. UX rules

The visual direction is warm, light, editorial and trustworthy. Avoid dark neon, generic purple AI gradients, glassmorphism overload, sparkles everywhere and marketplace clutter.

Every primary screen must answer:

1. What is happening?
2. Why does it matter?
3. What can the user do?
4. What will happen to their money?
5. How can they recover?

UX priority:

```text
clarity → trust → speed → emotion → novelty
```

Signature motion should communicate product logic:

```text
recipient clues converge
→ invalid gifts fall away
→ selected gift wraps
→ approval resolves into receipt
```

Use native Compose animation first. Add Rive only after a 90-minute proof and only if it replaces a harder native implementation.

## 17. AI-generated design rules

AI images are allowed for:

- mood and art direction;
- composition exploration;
- stakeholder alignment;
- launch graphics.

They are not allowed as:

- evidence of implementation;
- a substitute for interaction states;
- a source of exact logos or brand assets;
- a reason to copy impossible layout proportions;
- a reason to delay working code.

When translating a generated concept, create real design tokens and Compose components. Do not paste a screenshot as the app UI.

## 18. Testing requirements

Each vertical slice needs:

- happy path;
- invalid input;
- network failure;
- server failure;
- stale data where relevant;
- cancellation;
- duplicate action protection;
- retry/recovery;
- accessibility check;
- clean build.

Payment-specific tests:

- repeated tap uses same idempotency key;
- price changes before approval;
- hosted approval cancelled;
- deep link missing;
- process killed during approval;
- webhook arrives before app return;
- app return arrives before webhook;
- final state unknown;
- transaction declined;
- timeout and reconciliation.

## 19. Observability

Use structured logs with:

- request/correlation ID;
- purchase-intent ID;
- safe merchant identifier;
- Prava session/transaction ID when available;
- state transition;
- duration;
- error category.

Do not log:

- secrets;
- full card or credential data;
- unnecessary personal hints;
- addresses in public screenshots;
- raw model prompts containing private information unless locally redacted.

## 20. Tool selection

Use `TOOLKIT.md`. Do not research ten libraries for a solved problem.

A new dependency is permitted only when:

- a concrete blocker is documented;
- the dependency can be validated within 90 minutes;
- it replaces existing complexity;
- rollback is clear.

Mobbin MCP is connected but currently returns a paid-plan gate. Do not pretend Mobbin results were retrieved. Use the query file for later access and continue with Material patterns, existing visual references and direct testing.

## 20.1 Track and Linq rules

Primary track targets are Overall/Prava, Best UX/Mac mini, Visa, OpenAI and Localhost. Read `docs/TRACK_STRATEGY.md` before any work framed as prize optimization.

Linq rules:

- Treat Linq as optional late-stage work.
- Do not create Linq scaffolding while the core transaction is incomplete.
- Do not claim Linq-track readiness if Linq only sends a final notification.
- Messaging must be a core interface for initiating or managing the gifting transaction.
- Do not sign up independently when official instructions say to wait for Prava/Linq access guidance.
- Keep the Android + Prava flow fully functional without Linq.
- Remove or disable Linq cleanly if it introduces instability.

The Best UX / Mac mini target is explicit. UX work must improve comprehension, trust, recovery, emotional payoff or demo clarity. Decorative work that does not improve one of those outcomes is lower priority than transaction reliability.

## 21. Documentation updates

Update:

- `docs/INTEGRATION_STATUS.md` whenever an integration fact changes;
- `docs/DECISION_LOG.md` for consequential choices;
- `docs/PREEXISTING_WORK.md` before submission;
- README/run instructions after commands change;
- demo claims after flow behavior changes.

## 22. Completion report format

At the end of a task, report:

```text
Outcome:
Files changed:
Commands run:
Tests/evidence:
External assumptions:
Truth boundary (live/controlled/simulated):
Remaining risk:
Next smallest task:
```

Do not say “done” without evidence.

## 23. Escalation

Ask for clarification only when the missing decision cannot be resolved from files, current code, docs or a reversible default.

For Prava or partner issues:

1. record request IDs and exact state;
2. check handbook and docs;
3. ask Birdie in the Discord support channel;
4. keep context in the thread;
5. contact the Prava team before escalating to a partner.

## 24. Feature freeze

At 12 hours before internal submission:

- no new integrations;
- no architecture rewrites;
- no unproven animation runtime;
- no additional sponsor track;
- no data-model expansion unless fixing a blocker.

At 6 hours before internal submission:

- only bug fixes, copy, accessibility, demo and submission work.

At 2 hours before internal submission:

- no code changes unless the current build cannot be submitted.
