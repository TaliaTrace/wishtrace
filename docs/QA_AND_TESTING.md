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
- Prava payment-result polling and report-status reconciliation;
- reconciliation;
- Android API client.

### End to end

- clean-install real-account flagship flow;
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
- unsupported merchant;
- prior product recedes when a different live eligible candidate exists;
- prior primary recommendation recedes even when it was never selected into a mandate;
- prior product remains eligible when live inventory has no valid alternative.

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
- polling resolves before app return;
- app return occurs before polling resolves;
- duplicate tap;
- duplicate status/report request;
- unknown status;
- refresh resolves unknown;
- setup conflict automatically refreshes the existing mandate;
- sandbox-only replacement requires the exact post-mint unknown mandate and a different product;
- production cannot replace an unknown charge;
- one explicit pre-credential mint retry reuses the active approval without a new session;
- mint retry is refused when the provider charge count reached the mandate limit;
- a second pre-credential mint failure permanently removes the retry;
- an exhausted mint offers only a fresh owner-approved sandbox-card path, never a third charge;
- post-mint unknown never qualifies as a mint retry;
- `Authorized` and `Creds_Generated` dashboard rows are never mapped to a merchant order or decline;
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
- empty and error states;
- exhausted-card terminal recovery and the persistent Home `View merchant proof` route.

## Demo preflight

- airplane mode off;
- battery charged;
- notification privacy enabled;
- test account has no unintended prior recipient or purchase intent;
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
