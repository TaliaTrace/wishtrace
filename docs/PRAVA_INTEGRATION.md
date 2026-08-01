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
- the documented Browser Harness reconciles a Shopify checkout and returns a verified result, but
  the public SDK/API reference currently exposes no callable Browser Harness endpoint or method.

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
  → READY_FOR_APPROVAL
  → SESSION_CREATING
  → AWAITING_USER
  → PROCESSING
  → SUCCEEDED

Terminal alternatives:
DECLINED | CANCELLED | EXPIRED | FAILED

Nonterminal uncertainty:
UNKNOWN → RECONCILING → terminal state or manual support
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
