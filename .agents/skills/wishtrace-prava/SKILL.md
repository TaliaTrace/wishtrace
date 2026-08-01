---
name: wishtrace-prava
description: Implement and review Prava sessions, approval, transaction states, idempotency, security and evidence.
---
# WishTrace Prava Skill

Read current official docs, `docs/PRAVA_INTEGRATION.md`, `AGENTS.md` and integration status.

Workflow:

1. Verify exact official contract.
2. Prove smallest sandbox call manually.
3. Keep credentials backend-only.
4. Build purchase-intent snapshot and idempotency.
5. Model all states.
6. Handle hosted approval and return.
7. Reconcile authoritative state/webhook.
8. Capture IDs/evidence.
9. Test duplicate, cancel, decline, expiry and unknown.
10. Update status.

Never invent methods, silently retry money movement, call pending success or claim order completion without proof.
