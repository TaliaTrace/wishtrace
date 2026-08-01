# Product Specification

## User outcome

A busy person can turn a remembered occasion and recipient context into a purchased, personal gift without repeatedly searching, comparing and returning later to check out.

## Primary user

A student or young professional who buys gifts for a small circle of close people, has limited time and budget, and wants gifts to feel personal rather than generic.

## Jobs to be done

- Remember an important date.
- Preserve useful context about a loved one.
- Avoid buying something unsuitable.
- Stay within budget.
- Order in time.
- Complete payment safely.
- Add a personal touch.

## Core user stories

### Recipient setup

As a user, I can add a person, relationship, interests, dislikes and optional hints so WishTrace has useful context.

Acceptance:

- required fields are clear;
- date and timezone behavior are correct;
- budget currency is explicit;
- dislikes/exclusions can be edited;
- setup can be completed quickly.

### Occasion awareness

As a user, I can see which occasion needs attention first.

Acceptance:

- next occasion and days remaining are obvious;
- urgency is not exaggerated;
- primary action opens the gifting flow;
- empty state explains how to add a person.

### Gift discovery

As a user, I can ask WishTrace to find suitable gifts using my recipient context and constraints.

Acceptance:

- candidate source is known;
- progress shows meaningful stages;
- hard constraints are evaluated;
- failure is recoverable;
- results do not contain invented products.

### Decision

As a user, I can understand why one gift was selected and why alternatives were rejected.

Acceptance:

- direct evidence and inference are visually distinct;
- exact price and merchant are shown;
- delivery timing is shown only when supported by source data;
- one recommendation is primary;
- no more than two alternatives are shown;
- user can choose an alternative or edit constraints.

### Purchase

As a user, I can approve the exact transaction through Prava and see whether it completed.

Acceptance:

- exact merchant, amount and currency are shown;
- no card details are handled by the model or Android app;
- duplicate taps do not create duplicate purchases;
- cancel, decline, expiry, failure and unknown states are supported;
- authoritative result is reconciled;
- receipt/reference is retained.

### Personal message

As a user, I can edit a suggested text note before the gift is marked ready.

Acceptance:

- generated text is editable;
- no fabricated personal claims;
- message can be skipped;
- audio is optional and must be described truthfully.

## Data model summary

### Recipient

- ID
- display name
- relationship
- interests
- dislikes/exclusions
- optional hints
- optional delivery information

### Occasion

- ID
- recipient ID
- type
- local date
- timezone
- budget in minor units and currency
- required arrival date when known
- status

### Product candidate

- stable ID
- source mode
- merchant
- title
- price/currency
- product/checkout reference
- availability
- variant requirements
- delivery data or unknown
- fetched timestamp

### Ranked decision

- selected candidate ID
- ordered alternative IDs
- evidence references
- short explanation
- uncertainty
- model/schema version

### Purchase intent

- ID
- occasion and recipient IDs
- exact candidate snapshot
- approved amount/merchant
- idempotency key
- current state
- Prava identifiers
- timestamps

## Nonfunctional requirements

- No secrets in client.
- Money-safe arithmetic.
- Clear source boundary.
- Idempotent transaction creation.
- Resilient return/reconciliation.
- Accessible light-mode interface.
- Demo can run from seeded data.
- Critical flow works with predictable latency or visible progress.

## Privacy

Hints may contain private relationship context. For the MVP:

- collect only what is needed;
- avoid sending irrelevant full conversations to the model;
- redact or summarize before logging;
- do not expose hints in public demo screenshots beyond seeded fictional data;
- allow hint editing/deletion in future product scope, though full account privacy controls may be outside the MVP.
