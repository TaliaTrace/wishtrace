# WishTrace — PLAN.md

> **Thoughtful gifts. On time.**

## Event facts

- Event: Prava Agentic Commerce Hackathon
- Official build opening: July 31, 2026 at 7:00 PM PT, which is August 1 at 7:00 AM PKT
- Hard submission deadline: August 2, 2026 at 3:00 PM PT, which is August 3 at 3:00 AM PKT
- Internal submission target: August 3 at 12:30 AM PKT
- Team size: 1–4 accepted participants
- Platform: native Android
- Primary Prava path: SDK/API, sandbox first

The internal target leaves roughly 2.5 hours for upload problems, video export, Devfolio issues and final verification.

---

## 1. Product definition

WishTrace helps busy people remember important occasions and send gifts that feel personal.

The user adds:

- someone they care about;
- relationship;
- birthday or other important date;
- interests and dislikes;
- budget;
- optional hints, screenshots, links or notes;
- delivery deadline or address when required.

Near the event, WishTrace:

1. retrieves real candidate products from one validated commerce path;
2. removes candidates that violate price, availability, variant, merchant or delivery rules;
3. uses OpenAI to rank the remaining products against recipient context;
4. shows the user the selected gift, supporting evidence, merchant, exact amount and deadline;
5. creates a Prava payment session and completes the approved sandbox transaction;
6. attaches a personal text message, with audio as a stretch feature;
7. shows a verified receipt or final transaction result.

### The core loop

```text
remember → discover → decide → authorize → purchase → send → prove
```

### The product is not

- a general shopping chatbot;
- a wishlist clone;
- a calendar with an AI label;
- a static recommendation page;
- a fake checkout demo;
- an autonomous spender with vague limits;
- a cross-merchant basket or gift-hamper system for the MVP.

---

## 2. The flagship demo

### Giver

Talia, a university student or young professional who is busy but cares about gifting well.

### Recipient

Sophie.

### Occasion

- Birthday in 12 days
- Budget: up to the amount supported by the validated test catalog
- Delivery must arrive before the birthday

### Context

- Interests: gaming, music and books
- Saved clue: Sophie mentioned wanting a specific experience or product category
- Dislike: avoid decorative clutter
- Optional preference: digital gift is acceptable only if the selected merchant and Prava flow support it

### Important implementation rule

The visual concept may show a Microsoft or Xbox gift card, but the real demo SKU must be selected only after merchant and Prava validation. Stored-value products or gift cards must not be assumed safe or supported. If a physical product is more reliable, use the physical product.

### Demo beats

1. Talia sees Sophie's birthday approaching.
2. She opens Sophie's profile and sees interests, clues, dislikes and budget.
3. WishTrace retrieves a small real catalog.
4. Candidates are visibly filtered:
   - over budget;
   - arrives too late;
   - contradicts a dislike;
   - weak evidence.
5. One candidate is selected with a short grounded explanation.
6. Talia adds or approves a personal note.
7. The exact merchant and amount are shown.
8. Prava approval and transaction occur.
9. WishTrace shows a final receipt/result and “gift secured” state.

The entire flow should be understandable in under 90 seconds. The first five seconds should communicate: **person + occasion + gift outcome**.

---

## 3. Judging strategy

The handbook prioritizes end-to-end functionality, novelty, user value, Prava implementation, partner implementation, product experience and continuation potential. WishTrace should answer each with one coherent build.

### Overall / Prava finalists

Proof required:

- live or clearly controlled real-product discovery;
- visible decision rather than generic text generation;
- completed Prava sandbox result;
- user value without a long explanation;
- honest boundaries and failure states.

### Best UX / Mac mini announcement

This is a major objective, but “best UX” means clarity, trust and memorable interaction, not decoration.

Winning signals:

- warm light visual identity, not dark neon;
- one obvious action per screen;
- minimal onboarding;
- no marketplace wall of products;
- visible reasons for rejection and selection;
- merchant, amount and permissions clear before payment;
- real loading, expired, declined, cancelled and unknown states;
- one signature transition from clues to selected gift to receipt;
- strong peak and ending.

### Visa Intelligent Commerce

Prava integrations are eligible for Visa consideration according to organizer clarification. Emphasize:

- explicit user authorization;
- bounded amount and merchant;
- no card data exposed to the model or client;
- deterministic controls;
- clear transaction status;
- idempotency and no double purchase.

### OpenAI

OpenAI must materially improve the experience, not merely produce a greeting.

Primary proof:

- structured ranking of grounded merchant candidates;
- evidence-linked explanation;
- optional multimodal clue extraction;
- personal-message drafting;
- reliability and fallback behavior.

Substantive Codex use during development can support the track, but the running product should still use OpenAI meaningfully when feasible.

### Localhost startup-ready

The wedge is not “AI gift recommendations.” It is:

> A private relationship and occasion memory that turns scattered context into a completed, explainable gift purchase.

Show:

- repeat use across a small circle of loved ones;
- capture through the Android share sheet as a future retention loop;
- merchant or affiliate revenue potential;
- premium planning/history potential;
- credible 90-day continuation plan.

### Track priority and optional Linq

Primary targets are:

1. Overall / Prava finalists
2. Best UX / Mac mini
3. Visa Intelligent Commerce
4. OpenAI
5. Localhost Most Startup-Ready Product

These targets all reinforce the same Android + OpenAI + Prava flow.

Linq is an optional late-stage extension. Do not start it until the Android hero flow, merchant path, grounded ranking, Prava sandbox transaction, evidence capture and demo path are stable. To be credible for the Linq track, messaging must be a core interface for initiating or managing the purchase flow; sending only a final recipient notification is not enough.

NANDA and Senso remain out of scope unless the core submission is already complete and the plan is explicitly changed. See `docs/TRACK_STRATEGY.md`.

---

## 4. Fixed invariants and flexible choices

### Fixed

- Native Android experience.
- One end-to-end gifting flow.
- Real product data or a clearly disclosed controlled catalog of real products.
- OpenAI ranks product IDs, not invented products.
- Deterministic hard constraints.
- Prava SDK/API sandbox transaction.
- Backend-only secrets.
- Truthful final state and transaction evidence.
- Text message included.
- Generated imagery is never presented as a working app.

### Flexible until tested

- Kotlin module structure.
- FastAPI versus Node backend.
- Exact OpenAI model.
- Exact merchant and SKU.
- UCP, merchant API, MCP-backed catalog adapter or controlled real-product feed.
- Database choice.
- Audio implementation.
- Rive versus native Compose motion.
- Number of onboarding screens.

No flexible choice should remain open after it begins blocking implementation.

---

## 5. Scope ladder

### Gold

- Polished Compose app with 8–10 coherent screens
- Add recipient and occasion
- Optional share-sheet hint capture
- Live validated merchant search
- Structured OpenAI ranking with visible evidence
- Deterministic rules and rejected candidates
- Personal text and audio
- Prava sandbox purchase and receipt
- Retry, cancel, expired and unknown states
- Signature motion sequence
- Production-access request after sandbox reliability
- Recorded user feedback or quick interviews

### Silver

- Polished Compose hero flow
- One recipient and occasion
- Validated catalog or merchant results
- OpenAI ranking and message
- Deterministic constraint checks
- Completed Prava sandbox flow
- Strong approval and success UX

### Bronze floor

- Seeded recipient and occasion
- Small controlled catalog of real products from one merchant
- OpenAI selects only from the catalog
- Completed Prava sandbox payment flow
- Text note
- Verifiable receipt/result

A stable Bronze submission beats a Gold plan with a broken transaction.

---

## 6. Screen plan

### Mandatory

1. Value onboarding or direct start
2. Add recipient
3. Add occasion, interests, dislikes and budget
4. Home with upcoming occasion
5. Recipient detail
6. Discovery progress
7. Recommendation and evidence
8. Prava review/approval handoff
9. Personal message
10. Receipt / gift secured

### Optional

- Share-sheet capture
- Audio message
- Transaction history
- Settings
- Multiple recipients

Onboarding is cut first if it threatens the core flow. A demo can begin from a seeded home screen.

---

## 7. Technical flow

```text
Android Compose client
  → WishTrace backend
      → recipient/occasion persistence
      → commerce adapter
          → validated merchant/catalog
      → deterministic product validator
      → OpenAI orchestration
      → Prava SDK/API adapter
      → transaction ledger and webhook handling
  ← typed UI state and evidence
```

### Discovery order

1. Get candidate products from commerce adapter.
2. Normalize money, merchant, availability, delivery and variants.
3. Deterministically reject invalid candidates.
4. Send valid candidate IDs and attributes to OpenAI.
5. Receive structured ranked IDs and evidence references.
6. Validate that every returned ID exists.
7. Refresh price and availability before purchase.
8. Create Prava session for the exact current transaction.
9. Complete and verify authoritative result.

The model never receives card data and never decides whether a hard constraint may be ignored.

---

## 8. Merchant strategy

Merchant compatibility is the main external risk.

Use the organizer-provided resources for discovery:

- Composio MCP Gateway
- UCP Checker
- E-commerce MCP server directory
- Prava merchant spreadsheet

A directory listing is not proof of successful checkout. Validate the complete flow early and keep a backup.

### Preferred order

1. Merchant with a tested Prava-compatible path and simple product/checkout flow.
2. Merchant API or UCP flow that can be validated end to end.
3. Controlled catalog of real products paired with a real Prava sandbox payment flow, with the boundary disclosed.
4. Final fallback: one known fixed SKU and exact amount if the merchant integration is unstable.

Do not attempt a multi-store basket.

---

## 9. 44-hour execution plan

### Phase 0 — before official build window

Allowed planning only:

- review docs and merchant options;
- prepare this kit;
- collect UX direction;
- confirm accounts and access;
- prepare blank repository and environment checklist;
- do not claim judged implementation as hackathon-built before kickoff.

### Hours 0–2: prove the path

- Confirm official build window has opened.
- Create repository baseline and disclosure commit.
- Test Prava sandbox authentication.
- Manually create the smallest payment/session request.
- Select primary and backup merchant path.
- Record all results in `docs/INTEGRATION_STATUS.md`.

**Gate:** no high-fidelity UI until the payment and merchant path are understood.

### Hours 2–6: backend skeleton

- Domain models.
- Recipient and occasion endpoints.
- Commerce adapter interface.
- Candidate normalizer and validator.
- OpenAI structured ranking spike.
- Prava adapter interface and idempotency store.
- Health and diagnostic endpoints.

### Hours 6–12: Android skeleton

- Compose theme and navigation.
- Seeded home, recipient and discovery screens.
- Network client and typed states.
- Recommendation screen.
- Prava handoff/deep-link return handling.
- Receipt screen.

### Hours 12–18: integrate the full vertical slice

- Connect real/controlled candidate data.
- Run validator and model ranking.
- Create purchase intent.
- Open Prava approval.
- Handle success, cancel, failure and unknown return states.
- Verify receipt/result.

**Gate:** the entire Bronze flow runs on a physical device or stable emulator.

### Hours 18–25: UX pass

- Implement final hierarchy and content.
- Add meaningful loading stages.
- Add rejected-candidate reasons.
- Clarify exact merchant, amount and deadline.
- Add signature motion with native Compose first.
- Record a silent five-second clip and test comprehension.

### Hours 25–31: reliability

- Test duplicate taps and retries.
- Test stale price.
- Test model malformed output.
- Test Prava cancel/decline/timeout/unknown.
- Test process death around return path.
- Add logs and correlation IDs.
- Scan repository for secrets.

### Hours 31–36: startup and track proof

- Add pre-existing-work disclosure.
- Capture transaction evidence.
- Add one or two quick user interviews or feedback notes if possible.
- Prepare OpenAI and Visa implementation explanation.
- Request temporary production access only if sandbox is reliable and time remains.

### Hours 36–40: demo and submission assets

- Freeze features.
- Record primary and backup demos.
- Write README and run commands.
- Create architecture diagram and screenshots.
- Draft Devfolio submission.

### Hours 40–42: independent review

- Run the judge checklist.
- Ask a fresh person to watch without audio.
- Verify build from clean checkout.
- Verify every submission claim.

### Hours 42–44: submit early

- Upload before the internal deadline.
- Confirm links and video access.
- Keep a local copy of APK, video and transaction evidence.
- No feature work after successful upload unless fixing a blocker.

---

## 10. Definition of done

The MVP is done only when:

- a fresh install opens;
- the flagship flow can be completed without developer intervention;
- candidate products are real or their controlled boundary is disclosed;
- the model output is grounded in candidate IDs;
- deterministic constraints are enforced;
- the user sees exact merchant and amount;
- Prava sandbox result is authoritative and visible;
- a receipt or clear final status is shown;
- duplicate purchase is prevented;
- no secrets appear in client, repository, screenshots or logs;
- a 60–90 second demo exists;
- a backup demo exists;
- pre-hackathon work is disclosed;
- submission links work from another browser.

---

## 11. Stop rules

Stop adding features when any of these is true:

- merchant checkout is not proven;
- Prava session is not proven;
- the core flow cannot run twice reliably;
- the demo exceeds 90 seconds because of complexity;
- a new dependency would require more than 90 minutes to validate;
- there are fewer than eight hours before internal submission;
- the change does not improve a judging criterion.

The emergency priority order is:

```text
working transaction
→ truthful proof
→ coherent demo
→ trust UX
→ visual polish
→ extra features
```
