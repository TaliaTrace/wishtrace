# Architecture

## System

```text
Android app
  ├── Compose UI
  ├── ViewModels / UI state
  ├── typed API client
  ├── custom tab / hosted approval handoff
  └── app-link reconciliation
        ↓
WishTrace backend
  ├── recipient/occasion service
  ├── commerce adapter
  ├── product normalizer
  ├── deterministic validator
  ├── OpenAI orchestrator
  ├── purchase-intent service
  ├── Prava adapter
  ├── webhook/reconciliation service
  └── persistence / audit-safe logs
```

## Backend module boundaries

```text
app/
  api/
  domain/
  services/
  adapters/
    commerce/
    openai/
    prava/
  persistence/
  tests/
```

Names may adapt to existing repository conventions.

## Suggested API

```text
POST /recipients
GET  /recipients/{id}
POST /occasions
GET  /home
POST /discoveries
GET  /discoveries/{id}
POST /purchase-intents
POST /purchase-intents/{id}/message
POST /purchase-intents/{id}/prava-session
GET  /purchase-intents/{id}
POST /integrations/prava/webhook
GET  /health
```

Reduce endpoints when a simpler contract is already present.

## State ownership

- Recipient/occasion: backend authoritative after save.
- Discovery candidates: backend authoritative snapshot with source timestamp.
- UI navigation: Android.
- Purchase intent and transaction: backend authoritative.
- Hosted approval: Prava.
- Final status: reconciled backend state.

## Idempotency

Use a stable key for each purchase intent and money-moving operation.

Example scope:

```text
wishtrace:{purchase_intent_id}:{operation}:{version}
```

Persist the key and response mapping. Do not generate a new key on every retry tap.

## Error taxonomy

- `VALIDATION_ERROR`
- `COMMERCE_UNAVAILABLE`
- `PRODUCT_STALE`
- `MODEL_INVALID_OUTPUT`
- `PRAVA_SESSION_FAILED`
- `PRAVA_DECLINED`
- `PRAVA_CANCELLED`
- `PRAVA_EXPIRED`
- `TRANSACTION_UNKNOWN`
- `INTERNAL_ERROR`

Map to human messages and recovery actions.

## Deployment

Choose the fastest proven backend hosting. Requirements:

- HTTPS;
- secret environment variables;
- stable callback/webhook URL;
- logs accessible during demo;
- rollback or redeploy within minutes.

Do not introduce infrastructure complexity solely for architecture points.
