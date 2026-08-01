# Codex Bootstrap

Paste the following from the WishTrace repository root.

```text
You are the lead implementation agent for WishTrace during a timed hackathon.

First read, in order:
1. PLAN.md
2. AGENTS.md
3. TOOLKIT.md
4. EXECUTION_BOARD.md
5. docs/INTEGRATION_STATUS.md
6. docs/DECISION_LOG.md
7. .agents/skills/wishtrace-product/SKILL.md
8. .agents/skills/wishtrace-prava/SKILL.md
9. the repository tree, build files, environment examples and current git diff

Do not implement the whole application yet.

Your first objective is to produce a verified technical reconnaissance report and the smallest executable vertical skeleton.

Tasks:
A. Report the current repository state, commands that appear valid, missing dependencies and risky assumptions.
B. Identify the official Prava SDK/API path currently available in the repository or docs. Do not invent methods.
C. Identify the primary commerce adapter candidate and one fallback. Treat directories as leads, not proof.
D. Propose the smallest domain contracts for recipient, occasion, product candidate, ranked decision, purchase intent and transaction state.
E. Create only the minimum project scaffolding needed to run the Android and backend health paths.
F. Add no secrets and no broad feature code.
G. Run relevant build/tests and report evidence.
H. Update docs/INTEGRATION_STATUS.md and docs/DECISION_LOG.md with verified facts only.

Follow AGENTS.md strictly. End with:
Outcome
Files changed
Commands run
Tests/evidence
External assumptions
Truth boundary
Remaining risk
Next smallest task
```

## Second task after reconnaissance

```text
Implement one backend vertical slice:
seeded recipient + occasion → commerce adapter returns normalized candidates → deterministic validator rejects invalid candidates → OpenAI adapter interface returns a validated ranked decision.

Use a fake adapter only behind an explicitly named controlled test implementation. Do not call it live. Add unit tests for over-budget, unavailable, excluded-category and malformed-model-output cases. Do not touch Prava yet unless the official contract has already been verified.
```

## Third task

```text
Implement the Prava purchase-intent state machine and the smallest official sandbox session path. Add idempotency, exact amount/merchant review data, cancellation, failure and unknown states. Keep secrets on the backend. Do not call session creation a completed transaction. Update integration status with request IDs and evidence.
```

## Fourth task

```text
Implement the Android hero flow using real typed state from the backend: home → recipient → discovery → recommendation → approval handoff → receipt. Use the design system and screen map. Include loading, error, retry and cancellation. Avoid building settings or secondary screens.
```

## Track work gate

Before adding any sponsor integration, read `docs/TRACK_STRATEGY.md`. Do not implement Linq until the core Prava sandbox transaction and main demo are stable.
