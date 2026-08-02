# Prava Integration Plan

## Chosen path

Use **Prava SDK/API** for the Android product through the backend. The Builder Handbook recommends SDK/API for native embedded experiences and says sandbox and production are available for this path.

MCP and CLI are not the default because the handbook describes them as production-only options and WishTrace needs a custom native payment UX.

Current official SDK/API contract verified on 2026-08-01:

- `POST /v1/sessions` creates a 15-minute, single-use hosted `full_checkout` session;
- `GET /v1/sessions/{sessionId}/payment-result` returns `pending`, `awaiting_result`,
  `completed`, or `failed`;
- single-use token, dynamic CVV and expiry appear only at `awaiting_result` and remain backend-memory
  only;
- `POST /v1/sessions/{sessionId}/report-status` must receive `APPROVED` or `DECLINED` after
  the merchant attempt;
- the Browser Harness is a checkout executor after credentials exist; it does not mint the one-time
  card. Organizer support confirmed on 2026-08-03 that Prava's Browser Harness is unavailable in
  sandbox and teams must build their own automation. WishTrace therefore uses its own exact-product
  Playwright actor and calls report-status itself.

Do not invent that missing invocation. A support request is pending for the exact hackathon sandbox
contract; implement only the documented session/poll/report boundary until it is answered.

## Meaningful role

Prava is not a button after recommendation. It enables WishTrace to move from a selected gift to an authorized and verified commercial result without exposing card details to the model or app.

## Sequence

```text
1. User reviews exact product, merchant, amount and deadline.
2. Backend refreshes product facts.
3. Backend validates budget and constraints.
4. Backend creates/updates immutable purchase-intent snapshot.
5. Backend creates Prava session using official contract and idempotency.
6. Android opens hosted approval flow.
7. User approves, cancels or fails.
8. Android returns through configured link.
9. Backend polls Prava's official payment-result endpoint and reconciles the callback/app return.
10. Android polls/refreshes purchase intent.
11. UI shows authoritative final state and evidence.
```

## State machine

```text
DRAFT
  → VALIDATING
  → QUOTED
  → READY_FOR_APPROVAL
  → SESSION_CREATING
  → AWAITING_USER
  → CREDENTIALS_READY
  → CHECKOUT_IN_PROGRESS
  → ORDER_VERIFIED
  → SUCCEEDED

Terminal alternatives:
DECLINED | CANCELLED | EXPIRED | FAILED

Nonterminal uncertainty:
UNKNOWN → RECONCILING → authoritative state or manual support
```

## Required purchase snapshot

- purchase-intent ID;
- recipient and occasion IDs;
- candidate ID and source snapshot;
- merchant;
- exact amount/currency;
- product/variant;
- budget boundary;
- delivery claim and source;
- idempotency key;
- created/updated timestamps;
- Prava session/transaction IDs;
- current status.

## Security

- Keys only on backend.
- No card data stored.
- Store only SHA-256 hashes of idempotency keys and canonical requests.
- Require the returned merchant origin and total to match the explicit review before a one-time
  credential can enter checkout memory.
- Validate redirect targets.
- Poll only the documented payment-result endpoint; do not invent a webhook contract.
- Report the merchant attempt as `APPROVED` or `DECLINED` through the documented report-status endpoint.
- Keep single-use card token, dynamic CVV and expiry in backend memory only; never log, persist or send them to Android.
- Redact sensitive logs.
- Use HTTPS.
- Limit production access to the approved window and use case.

## Completion evidence

Capture:

- session ID;
- transaction ID if supplied;
- authoritative status;
- response/correlation ID;
- timestamp;
- screenshot of hosted approval/result where allowed;
- app receipt/result screen;
- logs showing no duplicate operation.

Do not claim an order if only a payment/session was completed. A sandbox tokenized-card attempt
against a real merchant is valid organizer evidence even when the merchant checkout fails, but the
result must say authorization/attempt rather than “Gift secured.”

## Observed sandbox boundary — 2026-08-03

The active-mandate charge and one standard hosted session both failed inside Prava before a usable
one-time credential was returned. The standard purchase intent is terminal `FAILED`, with no
merchant outcome or order. Reopening its existing hosted URL did not create another session.

A later read-only card audit superseded the assumption that the failed default enrollment had
remained deleted: Prava listed both default card `7789` and non-default card `7912` as active.
WishTrace previously selected that stale default automatically. The recovery now omits `card_id`
whenever multiple active cards exist, forcing an explicit hosted choice without exposing card data.
One fresh user-authorized setup selecting `7912` is permitted; if its charge still fails before
credentials, retries stop and the result remains a provider-stage failure—not a merchant decline.

That phone setup reached the hosted security step but returned `FIDO_START_FAILED`, before mandate
creation. A bounded operator diagnostic reached Visa OTP and its Secure Payment Confirmation popup
but returned `AUTH_FAILED`; no credential, charge or merchant request occurred. Operator-driven
hosted verification is now stopped. Android exposes a fresh approval only after an explicit user tap,
and the user performs the real phone/passkey ceremony.

A subsequent user-controlled attempt superseded that blocker: hosted passkey approval completed,
the app return was handled, and Prava minted a one-time credential against the active `$10` mandate.
The Jackbox actor stopped before clicking Pay because Shopify's displayed total changed after its
initial pre-tax paint. The token was never exposed or persisted and no merchant outcome is claimed.

The recovery preserves the one-approval contract. The newest provider mandate is matched only when
its creation time belongs to the current local setup, quote totals are held stable for three seconds,
tax-inclusive total must fit the approved cap, and Android starts one deterministic sandbox proof
automatically after activation. A known pre-mint quote failure leaves the authorization active; any
post-mint uncertainty locks instead of creating another credential.

The next automatic run reached the real post-mint boundary: Prava returned a one-time credential and
the Jackbox actor clicked Pay once at the stable `$9.99` total. The page did not expose a verified
order or an explicit recognized decline within the observation window, so WishTrace recorded
`UNKNOWN`, sent no Prava report and authorized no retry. Provider `active` describes the remaining
authorization, not the merchant outcome; refresh can no longer overwrite that unresolved charge.

A later user-authorized Quiplash 2 mandate returned `FETCH_AGENTIC_CREDS_ERROR` before credentials.
The mandate remains active with zero authoritative charges. WishTrace refreshed Jackbox's live quote
before minting, but no payment credential or Pay submission reached the merchant. WishTrace permits
one explicit invocation retry under that same approval only after a read-only provider check confirms
the mandate is still active and `total_charges` is below its approved maximum. This creates no new
session or passkey prompt and never runs automatically. A second pre-credential failure is terminal;
any post-mint uncertainty remains permanently ineligible for this recovery.

The explicit Quiplash retry returned the same `FETCH_AGENTIC_CREDS_ERROR`. The immutable ledger now
contains two pre-credential failures, zero successful charges, no provider transaction reference and
no merchant payment submission. Android therefore exposes only `Done`. The request matches Prava's
documented `POST /v1/mandates/{id}/charge` body (`amount` plus `reference`); another merchant cannot
repair this pre-merchant provider failure, and a third invocation is not authorized.

Hosted mode remains intentional for native Android. Prava's embedded SDK is a browser JavaScript
package that mounts a secure iframe; it is not a native Android SDK. WishTrace therefore uses a
Custom Tab so Prava and the browser retain the secure origin and WebAuthn/passkey ceremony. The app
owns the branded preparation, return, reconciliation and result screens.

## Production access

Do not apply prematurely. The organizer's 2026-08-01 announcement requires an end-to-end sandbox
integration inside the app, including a tokenized test-card transaction attempted through browser
automation against a real merchant. The expected merchant failure is accepted as a working sandbox
flow. Apply only after that evidence, a meaningful Prava role, and the non-mock Android journey exist.

Organizer Discord also indicated that participants without a compatible card could build in sandbox and contact the team for help with a compatible Visa card for production testing. Treat that as support intent, not a guarantee. Keep the sandbox submission complete on its own.

## Support request data

When asking Birdie/Prava, include:

- account email only in private/support context;
- environment;
- endpoint/SDK version;
- session, transaction and response IDs;
- timestamp and timezone;
- expected versus actual behavior;
- redacted request shape;
- screenshots;
- whether retry used same idempotency key.
