# Agentic Commerce Hackathon Context and Links

> Read this file before making architecture, product, UX, merchant, Prava, OpenAI, timeline, or submission decisions for WishTrace.
>
> Last consolidated: **July 30, 2026 (PKT)**

## 1. What this file is for

This is the external-context index for the **Prava + OpenAI Agentic Commerce Hackathon** and the **WishTrace** project.

Use it to:

- Understand the event and judging criteria.
- Know which sources are authoritative.
- Find official documentation quickly.
- Avoid inventing Prava, merchant, track, or deadline behavior.
- Keep WishTrace aligned with the submitted concept.
- Separate verified facts from assumptions that still need testing.

Do not rely on memory when a linked source or local reference can answer the question.

---

## 2. Source-of-truth order

When two sources conflict, use this order:

1. **Observed behavior in the current Prava sandbox or production environment**
2. **Latest official Prava docs**
3. **Latest organizer announcement in Prava Discord**
4. **Live Devfolio schedule, rules, and prize pages**
5. **Builder Handbook hard-rule and hard-deadline sections**
6. **This file, `PLAN.md`, and `AGENTS.md`**
7. Older notes, posts, generated concepts, and assumptions

Record consequential changes in `docs/DECISION_LOG.md` and current integration truth in `docs/INTEGRATION_STATUS.md`.

---

## 3. Event snapshot

- **Event:** Agentic Commerce Hackathon
- **Organizer / required payment layer:** Prava
- **Format:** Virtual
- **Team size:** 1–4 accepted builders
- **Build theme:** Software that can discover, decide, and complete or enable a commercial action
- **WishTrace status:** Accepted
- **WishTrace team name used in planning:** HARDCoders
- **Primary project:** WishTrace

### Official build window

The Builder Handbook gives the precise timing as:

- **Kickoff:** July 31, 2026 at 7:00 PM PT / August 1 at 7:30 AM IST
- **Hard deadline:** August 2, 2026 at 3:00 PM PT / August 3 at 3:30 AM IST

Approximate Pakistan conversion:

- **Kickoff:** August 1, 2026 at **7:00 AM PKT**
- **Hard deadline:** August 3, 2026 at **3:00 AM PKT**

The handbook contains a summary table that appears inconsistent with its later hard-deadline section. Use the **live Devfolio schedule** and the handbook’s explicit hard-deadline section. Submit early.

### Work-before-start rule

Planning, research, documentation, merchant investigation, and environment familiarization can happen before kickoff. Judged implementation must be completed during the official build window. Disclose anything that existed before the event.

---

## 4. What judges are looking for

The project must be a working end-to-end experience, not only a deck, mockup, or disconnected screens.

The main judging dimensions are:

- End-to-end functionality
- Creativity and novelty
- User value and market feasibility
- Meaningful and reliable Prava implementation
- Material use of partner technology
- Product experience and clarity
- Potential to continue after the hackathon

A strong product should show:

```text
clear user problem
→ meaningful discovery and decision
→ visible permissions and spending boundary
→ completed commercial action
→ verifiable outcome
```

What will not stand out:

- Generic chat wrappers
- A common RFH copied without a new insight
- Mocked payments presented as real transactions
- Prava or partner technology added only for eligibility
- A broad app with many unfinished paths

---

## 5. WishTrace product context

### One-line product definition

**WishTrace helps busy people remember important occasions and send thoughtful gifts on time. Users add loved ones, interests, dates, hints, and a budget. WishTrace finds a suitable gift, completes the approved purchase through Prava, and sends it with a personal text or audio message.**

### Primary user problem

People care about birthdays, anniversaries, and other occasions, but the intention to give a thoughtful gift becomes a multi-step task at the worst time. Calendar reminders remember dates but do not complete the intention.

### Narrow MVP

Build one excellent path:

```text
one recipient
→ one occasion
→ real recipient context and budget
→ real candidate gifts from one validated merchant path
→ deterministic budget / availability / timing checks
→ model-assisted ranking and explanation
→ user-visible Prava authorization
→ completed sandbox checkout or transaction evidence
→ receipt/result
→ personal text or audio message
```

One working recipient, occasion, merchant, and transaction is enough for the core demo.

### Fixed product principles

- Relationship and occasion first, not a generic wishlist or shopping search.
- Saved hints are one input, not the entire product.
- The model may rank only real candidate data supplied by an adapter.
- The model must not invent products, prices, variants, inventory, merchants, or delivery dates.
- Hard constraints are deterministic code, not model judgment.
- The payment step must feel central, safe, and understandable.
- Concept images are art direction, not implementation evidence.

### UX objective

The silent five-second story should be understandable:

```text
person + occasion + clues
→ options considered
→ bad options visibly rejected
→ thoughtful gift selected
→ spending permission understood
→ paid through Prava
→ personal message sent
```

The signature experience should make the selection logic visible, for example:

- Rejected: over budget
- Rejected: arrives after the occasion
- Strong match: recipient mentioned Xbox twice
- Selected: within budget and available in time

---

## 6. Target tracks and rewards

### Priority tracks

1. **Prava overall / strongest agentic-commerce product**
2. **Best UX Mac mini**, announced separately by Sushant
3. **Visa Intelligent Commerce**, through a meaningful Prava transaction
4. **OpenAI**, through meaningful API use or substantive Codex usage
5. **Localhost Most Startup-Ready Product**

### Lower priority unless scope changes

- Linq iMessage Agent
- Project NANDA adapter
- Senso discovery and trust

Do not bolt on an extra partner merely to enter another track. Track technology must materially improve the product.

---

## 7. Official event and organizer links

### Devfolio

- Main event page: https://agentic-commerce.devfolio.co/
- Overview: https://agentic-commerce.devfolio.co/overview
- Prizes: https://agentic-commerce.devfolio.co/prizes
- Lineup: https://agentic-commerce.devfolio.co/lineup
- Schedule: https://agentic-commerce.devfolio.co/schedule

### Official handbook

- Published Builder Handbook: https://docs.google.com/document/d/e/2PACX-1vRg9zmj3a5aWqUJQUaLDT4_SEUQGzt9lGn8aYVC898PTYOFIE3loLW_gCg0aEn334FogipRadhuNyju/pub
- Local HTML snapshot: `references/source/Agentic Commerce Hackathon Builder Handbook.html`
- Local text snapshot: `references/source/Agentic Commerce Hackathon Builder Handbook.txt`

### Organizer / social references

- Prava X account: https://x.com/pravapayments
- Prava Instagram: https://www.instagram.com/pravapayments
- Event announcement on X: https://x.com/sushantpandey_/status/2075910888578789727?s=20
- Event announcement on LinkedIn: https://www.linkedin.com/posts/sushantpandeycv_prava-openai-presents-the-agentic-commerce-share-7481679160168574976-L2Xj/
- Best UX Mac mini announcement: https://x.com/sushantpandey_/status/2081723688450457788?s=20

### Community and support

- Prava Discord invite: https://discord.gg/j6NzpSmuJ
- Birdie Telegram bot: https://t.me/pravapay_bot
- Hackathon support email from handbook: support+hackathon@prava.space
- General Prava support address linked in handbook: support@prava.space

Use Discord support first so Birdie and the Prava team can share context in the same thread.

---

## 8. Prava documentation and platform

### Core links

- Prava website: https://www.prava.space/
- Documentation home: https://docs.prava.space/
- Documentation index for agents: https://docs.prava.space/llms.txt
- Choosing an integration: https://docs.prava.space/choosing-your-integration
- Developer dashboard: https://dashboard.prava.space/
- Authentication and environments: https://docs.prava.space/authentication
- Quickstart / access checklist: https://docs.prava.space/quickstart

### Current integration choice for WishTrace

Default to **Prava SDK/API** because WishTrace is a native Android product that needs a controlled, branded payment flow and a sandbox path.

Use:

- Android app for the user experience
- Backend for secrets, Prava requests, OpenAI calls, merchant adapters, webhooks, and reconciliation
- Hosted or embedded approval only as supported by the current docs and tested environment

Do not expose Prava keys in the Android application.

### Meaningful Prava usage

Prava must enable the core commercial action. The correct story is not “we added a pay button.” It is:

```text
WishTrace discovers and selects a real gift
→ user understands merchant, amount, and permission
→ Prava authorizes the bounded purchase
→ checkout is completed
→ result and receipt are shown
```

Creating a payment session alone is not a completed order.

### Sandbox and production

- Build and stabilize the complete sandbox flow first.
- Temporary hackathon production access may be requested from August 1–8.
- Production access is reviewed and is not automatic.
- Contact Prava through Discord support before attempting escalation.
- Do not claim a live-card transaction unless it actually occurred and can be evidenced.

---

## 9. Merchant discovery resources

Organizer-shared resources:

1. Composio MCP Gateway: https://composio.dev/mcp-gateway
2. UCP Checker: https://ucpchecker.com/
3. E-commerce MCP directory: https://mcpservers.org/topics/ecommerce-mcp
4. Prava merchant spreadsheet: https://docs.google.com/spreadsheets/d/1Vwqybz1P9pNz3aQXc8Q4uVqa1p7vYTu_y3ySC7Xsunw/edit?gid=890707389#gid=890707389
5. Prava merchant directory: https://directory.prava.space/

### Critical rule

These are **discovery resources, not compatibility guarantees**.

Before committing to a merchant, validate:

- Product discovery works
- Variant data is usable
- Price and currency are accurate
- Inventory or availability can be checked
- Delivery or digital fulfillment is clear
- Checkout is compatible
- Prava authorization and completion work
- The final result can be verified

Keep one backup merchant or commerce flow.

### Community references, not official source of truth

- Community Prava repository: https://github.com/prajwalsuryawanshi/prava-payments
- Community SDK guide: https://prava-sdk.prajwalsuryawanshi.in/getting-started/

Use community material only for secondary hints. Official Prava docs and observed behavior override it.

### Gift-card warning

The generated WishTrace concepts use recognizable gift cards for visual clarity. Do not assume Microsoft, Xbox, Steam, Amazon, Google Play, PlayStation, Kindle, or any other gift card is actually purchasable through the selected merchant path. Verify before implementing or demonstrating it.

---

## 10. OpenAI references

### Official documentation

- OpenAI developer platform: https://platform.openai.com/
- API documentation: https://platform.openai.com/docs/overview
- API quickstart: https://platform.openai.com/docs/quickstart
- API reference: https://platform.openai.com/docs/api-reference
- Codex documentation: https://developers.openai.com/codex/

### Hackathon credits

- OpenAI hackathon credit form supplied in handbook: https://forms.gle/Cyi2wZnySCiL1daS8

The handbook states that only approved / RSVP’d participants should use the form and that the organizer reconciles the email list. Reconfirm current status in Discord before relying on a deadline or delivery date.

### WishTrace use of OpenAI

OpenAI should materially improve the decision, not merely generate marketing text.

Recommended model responsibilities:

- Convert recipient context into a structured preference profile
- Rank real candidate products
- Produce short, grounded reasons
- Generate a personalized text message
- Optionally assist with audio-message text or transcription

Deterministic code remains responsible for:

- Budget enforcement
- Merchant allowlist
- Currency
- Variant validity
- Availability
- Delivery cutoff
- Duplicate prevention
- Purchase permission
- Transaction state

### Key handling

- Keep API keys server-side.
- Use a project-scoped key or service account.
- Never commit secrets.
- The accepted hackathon identity and the funded API organization can be separate, but usage must remain truthful and attributable to the team.

---

## 11. Partner-track references

### Visa Intelligent Commerce

- https://www.visa.com/en-us/solutions/intelligent-commerce

WishTrace is relevant because it combines permissions, spending controls, trust, and a useful consumer purchase through Prava.

### Localhost

- https://www.localhosthq.com/

Startup-ready story:

- Repeated occasion-driven use
- Emotional consumer value
- Natural retention through birthdays and anniversaries
- Expansion from one-off gift approval to bounded occasion-based automation

### Linq

- Website: https://linqapp.com/
- Docs: https://docs.linqapp.com/

Do not sign up or integrate unless the latest organizer guidance permits it. It is not part of the current WishTrace core plan.

### Project NANDA

- Prava track details: https://nandatown.projectnanda.org/pravahack

Not part of the current WishTrace core plan.

### Senso

- Docs: https://docs.senso.ai/docs/introduction

Only use if verified merchant context becomes central enough to justify the added scope.

---

## 12. RFH reference

- Local RFH PDF: `references/source/RFH — Requests for Hacks.pdf`
- Notes: `references/RFH_NOTES.md`

WishTrace maps most directly to the shopping / styling RFH, narrowed to relationship-aware gifting:

```text
understand recipient and occasion intent
→ find real options
→ choose within hard constraints
→ complete the approved purchase through Prava
```

RFHs are inspiration, not specifications. WishTrace should preserve its relationship-first insight rather than becoming a generic shopping agent.

---

## 13. Discord organizer facts to remember

These points came from organizer announcements and the supplied Discord dump. Reconfirm any time-sensitive detail before making a final claim.

- The idea submitted during application may change.
- Only work built during the official window counts for judging.
- Planning and documentation before kickoff are allowed.
- Prava is required; MCP setup itself is not mandatory.
- SDK/API is the recommended route for many native hackathon projects.
- Build the sandbox path first.
- Production is optional, reviewed, and temporary.
- Birdie has hackathon and documentation context.
- Ask implementation questions in the Discord support channel so a human can join if needed.
- Merchant directories are not guarantees.
- API key deletion / rotation / multiple-key issues received an organizer patch.
- The best UX project has a separately announced Mac mini prize.
- Prava use makes submissions relevant for Visa-track consideration, but track quality still matters.

### Specific Discord references from supplied dump

These require membership in the Prava Discord server:

- OpenAI-credit discussion reference: https://discord.com/channels/1505768637313716285/1525459999781027910/1530870930274783315
- Office-hours reference: https://discord.com/channels/1505768637313716285/1525459674885787730/1530598676253114548

---

## 14. UX and product-research references

These are supporting research resources, not hackathon authorities.

- Mobbin: https://mobbin.com/
- Awwwards: https://www.awwwards.com/
- UXPeak: use the team’s selected UX psychology and critique videos
- Material Design 3: https://m3.material.io/
- Android Compose guidance: https://developer.android.com/develop/ui/compose

### Mobbin status

The Mobbin connector is connected, but direct MCP searches may require a paid Mobbin plan. Use the exact searches in `prompts/MOBBIN_QUERIES.md` once access works.

Use Mobbin for interaction patterns, not visual copying. Use generated concepts for art direction, not exact UI specifications.

---

## 15. Required local context files

Read these in order from the repository root:

1. `HACKATHON_CONTEXT_AND_LINKS.md`
2. `PLAN.md`
3. `AGENTS.md`
4. `CODEX_BOOTSTRAP.md`
5. `EXECUTION_BOARD.md`
6. `docs/PRODUCT_SPEC.md`
7. `docs/INTEGRATION_STATUS.md`
8. `docs/PRAVA_INTEGRATION.md`
9. `docs/MERCHANT_VALIDATION.md`
10. `docs/OPENAI_ORCHESTRATION.md`
11. `docs/SCREEN_MAP.md`
12. `docs/UX_RESEARCH.md`
13. `docs/DESIGN_SYSTEM.md`
14. `docs/QA_AND_TESTING.md`
15. `docs/DEMO_AND_SUBMISSION.md`

### Original source snapshots

- `references/source/Agentic Commerce Hackathon Builder Handbook.html`
- `references/source/Agentic Commerce Hackathon Builder Handbook.txt`
- `references/source/RFH — Requests for Hacks.pdf`
- `references/source/Discord dump excerpt.txt`

### Summaries

- `references/HANDBOOK_NOTES.md`
- `references/RFH_NOTES.md`
- `references/DISCORD_NOTES.md`

### Generated visual concepts

- `assets/concepts/wishtrace_showcase_concept.png`
- `assets/concepts/wishtrace_onboarding_ai_concept.png`
- `assets/concepts/wishtrace_onboarding_wireframe.png`

All generated concepts are non-production references and must never be presented as a working prototype.

---

## 16. Technical defaults

These defaults are chosen for the hackathon, but may change if a verified integration forces a better option.

### Android

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Coroutines and Flow
- Coil for images
- Chrome Custom Tabs or supported embedded flow for approval

### Backend

Choose the path that can be made reliable fastest:

- FastAPI, or
- Node.js / TypeScript

The backend owns:

- OpenAI requests
- Prava requests
- Merchant adapters
- API keys
- Webhooks
- Idempotency
- Transaction reconciliation
- Receipt and evidence records

### Required transaction states

At minimum:

```text
DRAFT
CANDIDATES_DISCOVERED
CANDIDATE_SELECTED
AWAITING_AUTHORIZATION
AUTHORIZED
CHECKOUT_IN_PROGRESS
COMPLETED
FAILED
CANCELLED
UNKNOWN_REQUIRES_RECONCILIATION
```

Never convert a timeout or unknown status into a fake success.

---

## 17. Merchant and integration validation gates

Do not polish the full UI until these gates are proven.

### Gate A: Prava sandbox

- API key works
- Correct environment selected
- Session or authorization can be created
- Approval can be completed
- Final status can be retrieved
- Webhook or polling path works
- Failed and cancelled states are understood

### Gate B: merchant path

- Search or catalog data can be retrieved
- Product IDs remain stable
- Price and currency are available
- Variant or denomination can be selected
- Checkout is genuinely compatible
- Order or purchase result can be shown

### Gate C: OpenAI ranking

- Model receives only real candidates
- Structured output validates
- Hard constraints are applied outside the model
- Invalid output has a deterministic fallback
- Every reason can be traced to recipient context or candidate data

### Gate D: Android handoff

- App can request discovery
- App can show candidate reasoning
- App can start Prava approval
- App can recover after returning from browser / hosted flow
- App can display final receipt or failure

---

## 18. Submission evidence checklist

The final submission should contain:

- Working demo
- Short demo video
- Repository or judge-access link
- Clear user and problem statement
- Clear Prava integration explanation
- Completed transaction evidence
- OpenAI / partner implementation evidence where claimed
- Pre-existing work disclosure
- What worked, what failed, and what was learned
- Backup recording and seeded demo data

Keep evidence such as:

- Prava transaction or session ID
- Merchant order or completion ID when available
- Amount, currency, and merchant
- Timestamps
- Backend logs with secrets redacted
- Webhook event or status-poll result
- Receipt/result screen

Judges may request repository access, logs, transaction evidence, or a live demo.

---

## 19. Pre-existing work disclosure

Before kickoff, the team may have:

- Product concept and name
- UX research
- Generated art-direction images
- Architecture and flow planning
- `PLAN.md`, `AGENTS.md`, toolkit, prompts, and reference pack
- Merchant and documentation research

The implementation created during the hackathon must be disclosed clearly. Do not imply that generated concepts are a live app or that a sandbox session is a completed order.

Use `docs/PREEXISTING_WORK.md` as the formal disclosure record.

---

## 20. Questions that must be answered early

1. Which merchant path completes a genuine purchase most reliably?
2. Is the product physical, digital, or both for the MVP?
3. Can a recognizable gift card be bought through the chosen path, or must the demo use another product?
4. What exact Prava object represents authorization and completion?
5. How is the app notified after approval or checkout?
6. What evidence proves completion?
7. Does the chosen flow require user approval for the exact gift, or can a bounded permission be demonstrated safely?
8. Which delivery claims can be verified instead of assumed?
9. Is production access worth pursuing after sandbox success?
10. Which one interaction will make the UX prize case obvious?

---

## 21. Support-request template

When blocked, post a focused request in the Prava Discord support channel:

```text
Project: WishTrace
Integration: SDK/API, sandbox
Goal: [one sentence]
Expected: [expected result]
Observed: [actual result]
Endpoint / step: [exact endpoint or UI step]
Request or transaction ID: [redacted-safe ID]
Timestamp and timezone: [time]
Error: [exact message]
Already tried: [short list]
Screenshot / log: [attach with secrets removed]
```

Do not post API keys, card details, personal addresses, or private recipient data.

---

## 22. Instructions for a coding agent

Before changing code or plans:

1. Read this file, `PLAN.md`, and `AGENTS.md`.
2. Inspect `docs/INTEGRATION_STATUS.md`.
3. Check the relevant official docs link.
4. Distinguish verified behavior from an assumption.
5. Prefer the smallest vertical slice that proves the commercial flow.
6. Do not silently change the product into a generic shopping app.
7. Do not claim a transaction succeeded without evidence.
8. Do not spend hours polishing an unverified merchant path.
9. Update the decision log when a consequential choice changes.
10. Keep the demo path stable once Gold scope is working.

---

## 23. Final context summary

WishTrace is not a reminder app, generic wishlist, or recommendation chatbot. It is an occasion-aware gifting workflow that turns a user’s intention into a completed, bounded, explainable purchase.

The winning version is narrow:

```text
Sophie’s birthday is approaching
→ WishTrace knows her interests, clues, and budget
→ real gifts are discovered
→ invalid choices are rejected visibly
→ one gift is selected with a defensible reason
→ the user sees exactly what may be spent
→ Prava completes the approved transaction
→ the result and personal message are shown
```

A real, verifiable transaction and a clear five-second UX story matter more than feature count.
