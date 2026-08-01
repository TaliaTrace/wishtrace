# Track Strategy

This file defines which prizes WishTrace actively targets, what proof each one needs, and when optional integrations may be added.

## Live prize facts and priority order

Verified from the public Devfolio prize page on 2026-08-01:

- Open Finalists: $10,000 pool;
- Best UX: $800;
- Best Visa Intelligent Commerce Implementation: $5,000;
- OpenAI Winners & Finalists: $9,000, separate from participation credits;
- Localhost Most Startup-Ready Product: $5,000.

Source: <https://agentic-commerce.devfolio.co/prizes>

### Tier 1: build for these from the beginning

1. **Open Finalists**
2. **Visa Intelligent Commerce**
3. **Best UX**
4. **OpenAI**
5. **Localhost Most Startup-Ready Product**

These five targets all strengthen the same core product. None requires a second product surface.

### Tier 2: optional only after the core is stable

6. **Linq iMessage Agent**

Linq is a late-stage extension, not part of the critical path. It may be attempted only after the Android flow, grounded gift selection, Prava sandbox purchase, evidence capture, and demo recording path are stable.

### Tier 3: do not pursue during this build unless the plan is explicitly changed

- Project NANDA
- Senso

Do not add sponsor technology merely for eligibility.

---

## Track proof matrix

| Target | What judges must see | Required evidence | Stop condition |
|---|---|---|---|
| Open Finalists | A coherent product that discovers, decides and attempts a useful transaction | Real-account end-to-end Android demo, live candidate evidence, grounded ranking, authoritative Prava result and truthful failure boundary | Stop adding features if the core run cannot repeat |
| Visa | Explicit consent, bounded merchant/amount, safe token handling and authoritative transaction status | Review snapshot, idempotency proof, backend-only single-use credential handling, real-merchant browser attempt and reconciled result | Never imply Visa rails/card usage unless the observed Prava result proves it |
| Best UX | A memorable, understandable and trustworthy experience | Five-second comprehension, polished hero flow, real loading/error/result states, clear permission, accessible physical-device capture | No decorative polish that delays the working transaction |
| OpenAI | Model use materially improves product selection or personalization | Grounded candidate ranking, structured output, evidence-linked reasoning, fallback behavior | Do not use model output for prices, stock or delivery facts |
| Localhost | A product that can continue after the hackathon | Sharp wedge, repeat-use loop, distribution story, 90-day roadmap, founder commitment | Do not inflate market claims without evidence |
| Linq | Messaging is a core product interface, with Prava powering the transaction | Sender can initiate or manage the gifting flow through iMessage/RCS/SMS; recipient delivery alone is insufficient | Drop immediately if access, reliability or setup threatens the main submission |

---

## Visa strategy

Visa is a primary implementation target, but the product should earn it through commerce controls:

1. The user sees the exact merchant, product/variant, refreshed amount and currency.
2. One explicit approval creates one idempotent operation; repeated taps reuse it.
3. Prava's single-use token, dynamic CVV and expiry remain backend-memory-only.
4. Deterministic code enforces budget, availability and variant constraints before the model ranks.
5. Android displays `UNKNOWN`, cancellation, decline and failed merchant attempts precisely.
6. Evidence includes the organizer-required real-merchant browser attempt, even when sandbox checkout fails as expected.

The submission copy describes only observed payment network/card facts. “Visa intelligent commerce”
is the prize category, not permission to claim an unobserved Visa transaction.

## Best UX strategy

The UX prize is a primary target, not an afterthought.

Winning behavior:

- The first five seconds communicate **person + occasion + gift outcome**.
- The user sees why candidates were rejected or selected.
- Payment approval clearly shows merchant, amount, limits and next action.
- The app has real loading, cancellation, decline, expiry and unknown states.
- The success moment feels emotionally complete, not like a generic receipt.
- Motion is brief, purposeful and reproducible on the demo device.
- The demo shows one polished journey rather than many unfinished screens.

The signature sequence should be:

```text
recipient clues
→ invalid gifts visibly removed
→ selected gift becomes the approval card
→ verified merchant order becomes “Gift secured”
→ personal message is attached
```

If sandbox produces authorization without a verified merchant order, the emotional payoff remains
strong but uses precise wording such as “Approval complete — merchant attempt recorded.”

---

## Linq gate

Do not start Linq until all of the following are true:

- Android hero flow is stable.
- Merchant path is validated.
- OpenAI ranking works against grounded candidates.
- Prava sandbox purchase completes reliably.
- Payment result is reconciled and shown truthfully.
- Main demo can be completed without Linq.
- Submission copy and evidence are already mostly prepared.
- Official Linq access instructions have been received from the Prava/Linq team.

### Strong Linq version

Linq counts only when messaging is a core interface. A strong extension is:

1. The sender messages WishTrace.
2. WishTrace collects recipient, occasion, interests and budget.
3. WishTrace returns grounded options and asks for approval.
4. Prava completes the approved purchase.
5. The recipient receives the personal message and gift or redemption link.

### Weak Linq version

Using Linq only to send a final notification to the recipient is useful product functionality, but it is unlikely to be a strong Linq-track implementation by itself.

---

## Build-time rule

Track breadth never outranks submission reliability. The order is:

```text
working Prava transaction
→ trustworthy UX
→ OpenAI proof
→ demo and evidence
→ startup story
→ optional Linq
```
