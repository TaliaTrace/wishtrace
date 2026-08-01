# Codex Task Prompts

## Merchant spike

```text
Read AGENTS.md, TOOLKIT.md, docs/MERCHANT_VALIDATION.md and docs/INTEGRATION_STATUS.md. Investigate the existing commerce integration only. Do not design UI. Prove search/product/price/availability/quote/checkout facts with the smallest official request. Treat directories as leads. Add one adapter interface and one primary implementation only after proof. Record live/controlled boundary, commands, IDs, failures and fallback trigger.
```

## Validator

```text
Implement deterministic product validation using money-safe types. Cover merchant, availability, required variant, budget, delivery deadline and explicit exclusions. Return structured rejection codes and user-facing factual labels. Add unit tests for every rule. The model must never override these results.
```

## OpenAI ranking

```text
Implement a provider-neutral OpenAI ranking adapter that accepts only validated candidate snapshots and recipient evidence IDs. Use structured output. Validate IDs and evidence references. Retry malformed output once, then fall back safely. Add tests with a fake provider. Do not hardcode a model until access/latency is measured.
```

## Prava state machine

```text
Using current official Prava docs, implement purchase-intent and transaction state transitions with idempotency. Include awaiting user, processing, success, decline, cancel, expiry, failure and unknown/reconciliation. Keep keys backend-only. Do not call session creation a completed transaction. Add tests for duplicate calls and out-of-order webhook/app-return events.
```

## Android hero flow

```text
Implement Home → Recipient → Discovery → Recommendation → Purchase Review → Prava Handoff/Return → Receipt in Jetpack Compose. Follow docs/DESIGN_SYSTEM.md and docs/SCREEN_MAP.md. Use immutable UI state and explicit loading/error/recovery. Do not add settings, social features or bottom navigation until the full flow works.
```

## UX review

```text
Read wishtrace-ux skill. Capture the current primary screens, then review purpose, hierarchy, trust, accessibility, five-second comprehension and failure recovery. Produce an ordered list of fixes by judging impact. Implement only the top three and re-capture screenshots.
```

## Demo freeze

```text
Read wishtrace-demo and wishtrace-qa skills. Run the complete flow twice, verify all claims, scan for secrets, test one failure path, build from clean checkout and produce the final 75-second script plus backup plan. Do not add features.
```
