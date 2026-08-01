# Screen Map

## 0. Entry and session

Welcome leads to Google sign-in. There is no `Not now`, local session or runtime fixture path. A
newly authenticated user enters the real empty state and creates their recipient and occasion.

## 1. Onboarding value and trust

Purpose: create emotional pull, explain the product loop and establish user control before account/setup.

Implemented moments:

1. Thoughtfulness and right-time arrival.
2. Remembering birthdays and celebrations.
3. Saving interests, exclusions and small clues.
4. Clues converging while weaker choices recede.
5. Reviewing timing, budget and note before anything moves.

Interaction:

- horizontal swipe plus stable bottom CTA;
- `Skip` until the final page;
- five-dot semantic progress;
- one focal visual composition, one headline and one sentence per page;
- native, reduced-motion-safe explanatory motion.

Do not display a dashboard, quiz, permission prompt, fake product or payment claim inside onboarding.

## 2. Add recipient

Purpose: create emotional anchor quickly.

Fields:

- name;
- relationship;
- generated initials;
- next.

State:

- validation inline;
- keyboard-safe layout;
- photo capture is deferred until WishTrace has a server-backed media contract;
- visible `1 of 2` progress;
- safe back recovery.

## 3. Occasion and preferences

Fields:

- occasion type;
- local date;
- interests;
- dislikes/exclusions;
- budget and currency;
- optional note/hint;
- delivery deadline/address is collected later, only when a live quote needs it.

Primary action: `Save`.

Implemented as the second step of the setup/editor flow. The initial viewport prioritizes occasion, date and interests; exclusions, budget and optional clue follow by scroll while the Save action remains sticky.

## 4. Home

Purpose: show next action immediately.

Hero:

- Sophie;
- birthday in 12 days;
- budget;
- `Find a gift`.

Secondary:

- other recipients or add person;
- history only if implemented.

States:

- empty;
- loading;
- upcoming;
- no occasion soon.

## 5. Recipient detail

Purpose: review the context used for decisions.

Sections:

- next occasion;
- interests;
- dislikes;
- hints;
- budget;
- edit;
- find gift.

## 6. Discovery progress

Purpose: turn latency into understandable progress.

Stages:

- checking products;
- checking budget;
- checking availability/delivery;
- matching preferences.

Include cancel and retry when meaningful. Never show fake percentages.

## 7. Recommendation

Purpose: make the decision defensible.

Primary card:

- product;
- merchant;
- exact price;
- source freshness;
- why it fits;
- evidence;
- constraint status;
- `Review purchase`.

Alternatives: maximum two.

Rejected facts: maximum three concise rows.

## 8. Purchase review

Purpose: establish trust before money moves.

Show:

- recipient/occasion;
- product/variant;
- merchant;
- amount/currency;
- delivery fact;
- authorization summary;
- personal message status;
- edit/cancel;
- `Approve $X purchase`.

## 9. Prava handoff / return

The hosted Prava experience may own the approval UI. WishTrace must provide:

- pre-handoff state;
- app background/return resilience;
- processing state;
- reconciliation;
- cancel/decline/expired/unknown UI.

## 10. Personal message

This can occur before approval or after selection. Pick one sequence and keep it stable.

Default:

- generated draft;
- editable text;
- skip;
- record audio stretch.

## 11. Receipt / gift secured

Show only authoritative facts:

- exact final status;
- merchant and amount;
- receipt/reference;
- product;
- delivery status if known;
- message status;
- view details/home.

## Navigation recommendation

Use four equal primary destinations: Home, People, Occasions and Profile. Add actions are contextual text actions in People and Occasions. There is no global plus/FAB because `Find a gift` is the product's dominant action. Hide primary navigation throughout setup, discovery, recommendation, message, review, Prava and receipt routes.

## Implemented route evidence

As of the disclosed pre-kickoff UI milestone 4, Welcome, Sign-in, Home, People, Recipient detail,
Occasions, Profile, Add/Edit person, Add/Edit occasion, Discovery, Recommendation and Personal note
are navigable. Their runtime fixture repositories are temporary technical debt and must be replaced
before the judged path is accepted.

The final Discovery route starts a backend discovery and reaches Recommendation only with live,
validated candidates. An unavailable merchant returns a recoverable unavailable state; it never
routes to a fabricated recommendation. Personal note preserves user/generated origin metadata.
