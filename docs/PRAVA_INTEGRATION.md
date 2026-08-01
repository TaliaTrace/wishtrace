# Prava Integration Plan

## Chosen path

Use **Prava SDK/API** for the Android product through the backend. The Builder Handbook recommends SDK/API for native embedded experiences and says sandbox and production are available for this path.

MCP and CLI are not the default because the handbook describes them as production-only options and WishTrace needs a custom native payment UX.

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
9. Backend reconciles with Prava and/or verified webhook.
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
- Verify webhooks using current official method.
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

Do not claim an order if only a payment/session was completed. Describe the exact result.

## Production access

The handbook allows participants to request temporary production access from August 1 through August 8, subject to review. Request it only after the sandbox flow works reliably.

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
