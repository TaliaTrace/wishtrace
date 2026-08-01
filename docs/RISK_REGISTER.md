# Risk Register

| Risk | Probability | Impact | Trigger | Mitigation | Fallback |
|---|---:|---:|---|---|---|
| merchant path fails | high | critical | no full path in 90 min | validate HyperX first, then one timed Turtle Beach attempt | narrow to one proven live SKU or show unavailable honestly |
| Prava API uncertainty | medium | critical | undocumented/failed request | use official docs, Birdie, IDs | reduce to smallest official session flow |
| payment status unclear | medium | critical | return says pending/unknown | poll official result and reconcile app return | show unknown honestly and use recorded proof |
| stored-value gift card restricted | medium | high | merchant/Prava fails | avoid commitment | physical gift SKU |
| OpenAI output invalid | medium | medium | schema/ID errors | structured output + validation | deterministic score/user selection |
| UI polish consumes schedule | high | high | transaction not done by hour 16 | gate UX work | cut onboarding/advanced motion |
| emulator/PC slow | medium | medium | build iteration lag | physical device, previews | reduce animation/assets |
| deep link return fails | medium | high | app cannot reconcile | test early | polling/manual refresh |
| production access unavailable | high | low for eligibility | no approval/card | sandbox-complete submission | narrate production as future |
| generated concepts mislead | medium | medium | screenshot shown as build | label assets | use real app captures |
| deadline confusion | low | critical | conflicting schedule text | follow hard-deadline section | internal target 2.5h early |
| secret leak | low | critical | key in client/repo | env vars, scan | rotate immediately |
| demo network failure | medium | high | live request unavailable | record a complete real run and retain correlation evidence | play truthful backup video; never switch to fixtures |
| overclaiming order completion | medium | critical | only session/payment exists | precise language | describe exact transaction result |

## Review cadence

- Every four hours during build.
- Immediately after any external integration failure.
- At feature freeze.
- Before recording and before submission.
