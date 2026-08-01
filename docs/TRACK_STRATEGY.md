# Track Strategy

This file defines which prizes WishTrace actively targets, what proof each one needs, and when optional integrations may be added.

## Priority order

### Tier 1: build for these from the beginning

1. **Overall / Prava finalists**
2. **Best UX / Mac mini**
3. **Visa Intelligent Commerce**
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
| Overall / Prava | A coherent product that discovers, decides and completes a useful transaction | Working end-to-end demo, Prava result, logs or receipt, clear user problem | Stop adding features if the transaction is not reliable |
| Best UX / Mac mini | A memorable, understandable and trustworthy experience | Five-second comprehension, polished hero flow, real loading/error/success states, clear payment permission | No decorative polish that delays the working transaction |
| Visa | Explicit permission, spending controls, safe credential handling and transaction completion | Merchant, amount, authorization boundary, status, idempotency, no card data in client/model | Do not make unsupported Visa claims |
| OpenAI | Model use materially improves product selection or personalization | Grounded candidate ranking, structured output, evidence-linked reasoning, fallback behavior | Do not use model output for prices, stock or delivery facts |
| Localhost | A product that can continue after the hackathon | Sharp wedge, repeat-use loop, distribution story, 90-day roadmap, founder commitment | Do not inflate market claims without evidence |
| Linq | Messaging is a core product interface, with Prava powering the transaction | Sender can initiate or manage the gifting flow through iMessage/RCS/SMS; recipient delivery alone is insufficient | Drop immediately if access, reliability or setup threatens the main submission |

---

## Best UX / Mac mini strategy

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
→ Prava result becomes “gift secured”
→ personal message is attached
```

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
