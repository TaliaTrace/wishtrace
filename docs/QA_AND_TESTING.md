# QA and Testing

## Test pyramid

### Unit

- money arithmetic;
- deterministic candidate rejection;
- schema validation;
- state transitions;
- idempotency behavior;
- date/deadline logic.

### Integration

- commerce adapter normalization;
- OpenAI structured output;
- Prava sandbox session;
- webhook verification;
- reconciliation;
- Android API client.

### End to end

- seeded flagship flow;
- cancel and retry;
- decline/failure;
- process death or app background during approval;
- repeat flow without duplicate purchase.

## Deterministic validator cases

- over budget;
- wrong currency handling;
- unavailable;
- unknown availability;
- required variant absent;
- excluded category;
- late delivery;
- missing delivery data;
- stale price;
- unsupported merchant.

## Model cases

- valid schema;
- invalid JSON/schema;
- invented candidate ID;
- selected rejected candidate;
- unsupported delivery claim;
- empty evidence;
- no suitable candidate;
- timeout;
- rate limit.

## Payment cases

- success;
- user cancel;
- decline;
- session expiry;
- network lost after approval;
- webhook before app return;
- app return before webhook;
- duplicate tap;
- duplicate webhook;
- unknown status;
- refresh resolves unknown;
- changed price requires new approval.

## Android UX cases

- keyboard does not hide action;
- back navigation is safe;
- rotation/process recreation where supported;
- status/navigation bars;
- 1.3x font scale;
- screen reader labels;
- 48dp targets;
- low network;
- product image failure;
- empty and error states.

## Demo preflight

- airplane mode off;
- battery charged;
- notification privacy enabled;
- seeded data reset;
- server health checked;
- test credentials ready;
- screen recording storage available;
- backup video local and cloud;
- transaction IDs captured;
- no personal information visible.

## Clean build proof

From a fresh checkout/documented environment:

1. install dependencies;
2. run backend tests;
3. run Android unit tests;
4. build APK;
5. launch backend;
6. complete demo.

Record exact commands in README.
