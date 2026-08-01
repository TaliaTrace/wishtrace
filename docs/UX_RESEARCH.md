# UX Research and Synthesis

## Current research status

- Mobbin connector: connected.
- Search result on July 29, 2026: blocked by Mobbin paid-plan gate.
- Free-plan status confirmed by the user on July 30, 2026.
- Three MCP searches for onboarding flows and upcoming-event screens returned `INVALID_ARGUMENT` without previews or links.
- Three additional targeted searches on July 30 for value-first auth, upcoming-birthday home and sparse calendar screens also returned `INVALID_ARGUMENT`.
- Therefore no Mobbin screen is cited as inspected or used in this build.
- Exact searches are prepared in `prompts/MOBBIN_QUERIES.md` for later use.
- No callable Lazyweb MCP connector was available. Research used only Lazyweb's public web pages, so no paid/private flow is claimed.
- Public Lazyweb pages inspected on July 30: Onboarding gallery, Finch onboarding, Headspace onboarding, Days, Up Ahead, onboarding-quiz prevalence and onboarding-versus-signup length.
- The external Gemini Antigravity Compose, Android UI verification, UX audit, UX flow, UX/UI principles and visual-validator skill files were inspected as process guidance. They supplied no product facts or external integration behavior.

## July 30 implementation synthesis

The generated concepts are not a production reference. The implementation keeps only the underlying story—person, occasion, clues, outcome—and rejects their fake products, generated portraits, malformed logos, excessive lavender, dashboard chrome and unsupported merchant/payment details.

Current interface principles:

- progressive disclosure: one decision per route;
- recognition over recall: recipient clues stay visible during discovery;
- visible system status: factual stages instead of fake percentages;
- loss prevention: budget and “nothing is charged” boundaries appear before payment work exists;
- peak-end restraint: emotional warmth comes from copy, spacing and a single trace motif rather than confetti or decoration;
- controlled-data honesty: seeded fixtures are named in code/docs and never described as live; the consumer UI does not treat “demo” as a product mode.

### July 30 onboarding research correction

The earlier “maximum three screens” recommendation was too blunt for WishTrace. Lazyweb's public research reports a median onboarding length of 11 screens versus 6 for sign-up, while only 13.3% of tracked apps use an onboarding quiz. The useful lesson is not “make onboarding long”; it is to separate value education from account/setup, and use personalization questions only when answers materially change the experience.

Public Finch and Up Ahead captures were visually inspected for composition. The reusable patterns were:

- one expressive focal object or character per moment;
- a stable progress/CTA zone;
- bold, short copy with a single question or promise;
- supporting micro-elements that reward attention without becoming controls;
- immediate swipe/skip agency.

WishTrace applies those patterns as five distinct, quick moments:

```text
thoughtfulness
→ remembering dates
→ saving clues and exclusions
→ clues filtering choices
→ explicit user review
```

It does not copy Finch's mascot/streak mechanics, Days' heavier feature walkthrough, fictional products, or a quiz with no downstream effect. The pages are swipeable, skippable and non-blocking rather than a cinematic intro.

Lazyweb public sources:

- `https://www.lazyweb.com/inspiration/onboarding`
- `https://www.lazyweb.com/flow/finch/onboarding`
- `https://www.lazyweb.com/flow/headspace/onboarding`
- `https://www.lazyweb.com/company/days`
- `https://www.lazyweb.com/company/up-ahead`
- `https://www.lazyweb.com/research/what-percent-of-apps-show-an-onboarding-quiz`
- `https://www.lazyweb.com/research/onboarding-vs-signup-flow-length`

### July 30 shell refinement

Public research converted directly into implementation:

- Android recommends three to five equal-level destinations in a navigation bar. WishTrace keeps four and removes the center interruption.
- Android positions a FAB as the single highest-importance action. A global plus competed with `Find a gift` and required interpretation, so add actions moved beside People and Occasions.
- Material 3 Expressive favors shape, tonal containment and research-backed motion. WishTrace uses tonal surfaces and brief spring-like one-shot entrances, not looping decoration.
- Compose guidance favors interruptible animation and `AnimatedVisibility` when hidden content must leave accessibility semantics. Route changes use brief fades/scales; staggered content snaps when system animation is disabled.
- NN/g visual hierarchy guidance recommends only a few meaningful size levels and grouping related facts. The home screen now has one greeting, one occasion card and one clue card.
- NN/g minimalist and recognition-over-recall heuristics say every extra unit competes with relevant content and needed facts should remain visible. Auth copy was cut to one sentence; Sophie/date/countdown/interests/budget/action remain visible together.

Sources inspected:

- Android Material 3 in Compose: `https://developer.android.com/develop/ui/compose/designsystems/material3`
- Android layouts and navigation patterns: `https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns`
- Android animation quick guide: `https://developer.android.com/develop/ui/compose/animation/quick-guide`
- Android Compose accessibility: `https://developer.android.com/develop/ui/compose/accessibility`
- NN/g visual design principles: `https://media.nngroup.com/media/articles/attachments/Principles_Visual_Design-Letter.pdf`
- NN/g usability heuristics: `https://www.nngroup.com/articles/ten-usability-heuristics/`

This document converts known mobile UX principles and the WishTrace concept work into specific decisions. It must be updated with real references when access becomes available.

## Product emotion

The user should feel:

1. “I remembered in time.”
2. “This choice actually fits them.”
3. “I know exactly what will be charged.”
4. “It is handled.”

The product should not feel like a shopping marketplace or a finance dashboard.

## Five-second comprehension target

A silent viewer should see:

- a person;
- an upcoming birthday;
- a selected gift;
- a clear on-time outcome.

They should describe it as “an app that remembers occasions and gets the right gift,” not “a wishlist,” “budget tracker” or “gift-card app.”

## Onboarding synthesis

### Research question

How do we explain a multi-step product without forcing users through a marketing carousel?

### Decision

Use five short, skippable value/trust moments followed by account choice and a real two-step setup flow:

1. Thoughtfulness feels different.
2. Keep the moments that matter.
3. Hold onto the little clues.
4. Let the clues come together.
5. You make the final call.

Each page has one focal composition, one headline, one sentence and a stable action/progress area.
Account choice follows on its own route and is required; there is no local-session or demo escape
hatch. Recipient and occasion questions live in setup because those answers actually change the
experience.

### Avoid

- long feature lists;
- redundant slides that repeat the same promise;
- quizzes whose answers do not change the product;
- generic AI language;
- asking for notifications before value is understood;
- forced account creation before the user sees the product;
- animated intro that delays action.

## Home synthesis

The home screen is an occasion control surface, not a full calendar.

Hierarchy:

1. next important occasion;
2. time remaining;
3. recipient and budget;
4. “Find a gift” primary action;
5. other people/occasions below.

Avoid calendar-grid dominance. A calendar may exist as a secondary view.

## Recipient profile synthesis

Use a relationship-memory layout, not a contact card.

Show:

- name and relationship;
- next occasion;
- interests;
- dislikes/exclusions;
- saved hints with source/date;
- budget and delivery boundaries;
- edit controls.

Evidence should look tangible: compact chips/cards, source labels and editable extraction.

## Discovery synthesis

The system must feel active without pretending to think like a human.

Use meaningful stages:

```text
Checking available gifts
Applying your $60 budget
Removing late arrivals
Matching Sophie's interests
```

Show a compact trace of what happened. Do not expose internal chain of thought. Show user-relevant facts and decisions.

## Recommendation synthesis

Use one strong recommendation and at most two alternatives.

Primary card contains:

- product image;
- title;
- merchant;
- exact price;
- delivery fact or “delivery not confirmed”;
- direct evidence;
- why it fits;
- budget status;
- primary purchase action.

Rejected candidates can briefly animate away with one factual reason. Do not shame the user or overstate confidence.

## Approval synthesis

This is the highest-trust screen.

Before opening Prava, show:

- recipient and occasion;
- merchant;
- product/variant;
- item and total amount;
- currency;
- delivery deadline if verified;
- what Prava will authorize;
- what will not happen automatically;
- edit/cancel option.

The primary button should be explicit, such as “Approve $50 purchase,” not “Continue.”

## Success synthesis

Use peak-end design.

The selected gift wraps or resolves into a clean receipt card. Show:

- “Gift secured” or the precise verified status;
- merchant and amount;
- receipt/reference;
- delivery information when authoritative;
- personal message status;
- next action, such as view details.

Avoid excessive confetti. One short warm motion is enough.

## Signature interaction

1. Recipient hints orbit or sit around the recipient card.
2. A trace connects relevant evidence to candidate gifts.
3. Invalid candidates recede with short labels.
4. The selected gift moves to the center.
5. The card folds/wraps.
6. After Prava confirmation, it unfolds into the receipt.

Duration: 3–5 seconds total. Every movement should explain a state transition.

## Content principles

Use lay language in user-facing copy.

Prefer:

- “Finding gifts that fit Sophie”
- “Over your $60 budget”
- “Arrives after her birthday”
- “Approve $50 purchase”

Avoid:

- “Agentic orchestration”
- “AI confidence score”
- “Autonomous execution”
- “Inference pipeline”

## Accessibility

- 48dp targets.
- High contrast on warm light backgrounds.
- Text scales without clipping.
- Motion does not carry the only meaning.
- Reject/select states use icon and text, not color only.
- Product images have descriptions where meaningful.
- Screen reader order follows visual hierarchy.

## UX review ritual

For every primary screen:

1. Capture screenshot at normal phone size.
2. Blur/squint test: is hierarchy still obvious?
3. Five-second test: what did a fresh viewer think it does?
4. Thumb test: can primary action be reached comfortably?
5. Trust test: is money/permission unambiguous?
6. Failure test: can user recover without developer help?
7. Remove one nonessential element.

## 2026-07-30 rendered prototype review

- Mobbin free-plan MCP calls returned `INVALID_ARGUMENT`; no Mobbin screen or flow was retrieved or cited.
- The first API 31 capture exposed duplicated status-bar insets on onboarding and home. The redundant padding was removed and both screens were recaptured.
- Primary hierarchy is person → occasion urgency → one action. Profile copy separates direct evidence, exclusions and factual limits from future preference inference.
- Discovery shows four named stages, no percentage, visible cancel/retry/back recovery and a specific “what has not happened” boundary.
- Core text/background contrast pairs measure from 5.95:1 to 14.91:1.
- The onboarding action remains usable at 130% system text scale; content becomes taller and remains scrollable.
- A connected Compose test verifies all four routes and 48dp minimum height for primary and back actions on API 31.
- Fresh-human five-second testing, TalkBack, switch access and a physical-phone thumb test are still pending.

## 2026-07-30 refined shell review

- Removed all visible `DEMO` badges and the `Explore demo` entry; internal fixtures remain controlled and documented.
- Removed the center plus/FAB after hierarchy review. Four primary destinations now have equal visual weight.
- Reduced Welcome and Sign-in to one supporting sentence each; removed auth configuration prose from the normal path.
- Rebuilt recipient detail around identity, occasion, interest/exclusion chips and one clue; timezone and arrival details are progressive disclosure.
- Added one-shot staggered entrances and route transitions using native Compose. Motion is decorative reinforcement only and becomes instant when Android animator duration is zero.
- Refined screenshots are in `artifacts/screenshots/milestone-2/`.

## 2026-07-30 onboarding and context visual gate

- Five API 31 onboarding captures were reviewed at rendered phone size after implementation, not inferred from Compose previews.
- The first richer pass exposed modifier-order shrinkage on three supporting assets and an overlapping decorative heart. The assets were resized/reordered and the redundant heart removed before recapture.
- Final compositions use a focal 3D object plus multiple purposeful elements: calendar/person/heart tiles, countdown dates, saved-clue note, interest/exclusion chips, selected/rejected candidates and review fact tiles.
- The two setup screens were also captured. Step two uses progressive disclosure: occasion/date/interests are visible first; exclusions, budget and clue appear with one scroll while Save remains reachable.
- All final screenshots are in `artifacts/screenshots/milestone-3/`.
- Automated evidence is clean; fresh-human five-second testing, TalkBack, switch access, physical-phone thumb reach and physical-device motion profiling remain pending and must not be claimed.

## 2026-07-30 decision-flow visual gate

- Replaced the old verbose progress list and truth-boundary paragraphs with one visual trace, four compact labeled stages and a single `No purchase has started` status.
- The first rendered pass placed the stage rail under the sticky action. The trace scene was tightened while retaining all clue elements; final captures show Products, Budget, Delivery and Match together.
- Discovery motion maps to product logic: four known context clues converge, the progress ring resolves and every stage changes via icon, label and color. No percentage or thinking-orb metaphor is used.
- The source-needed decision screen uses a layered empty product card and search/shield cues, then surfaces budget, timing and clue readiness. It does not fill the empty card with an invented product.
- The personal note screen uses the message-heart focal asset, visible recipient/date context, a large editable field and a stable save/skip region. Empty Save is disabled.
- Final rendered evidence is in `artifacts/screenshots/milestone-4/`.
- Actual sourced recommendation visuals remain unverified until a real or recorded product fixture exists. The model-driven components are implemented but are not claimed as rendered commerce evidence.
