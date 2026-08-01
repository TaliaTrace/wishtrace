# WishTrace — TOOLKIT.md

The goal is not to collect tools. The goal is to ship one trustworthy transaction with exceptional UX.

## Frozen default stack

### Android

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Coroutines and Flow
- ViewModel
- Retrofit/Ktor client, whichever is already proven
- Coil for product images
- Android Custom Tabs or official hosted-flow mechanism for Prava approval
- App links/deep links for return handling

### Backend

- FastAPI + Pydantic is the default
- Node/TypeScript is acceptable if the selected Prava/merchant SDK is materially better there
- SQLite for local hackathon persistence, or a proven hosted database only when already configured
- Typed adapter boundaries for OpenAI, merchant and Prava

### Intelligence

- Current official OpenAI API
- Structured output for extraction and ranking
- Model selected after credit/access and latency testing
- No model-name lock in planning documents

### Payments

- Prava SDK/API
- Sandbox until the whole flow is reliable
- Production request only after sandbox proof
- MCP/CLI not the default because the handbook describes them as production-only paths and recommends SDK/API for native embedded experiences

### Design

- AI image generation for art direction and launch visuals
- Compose previews for real UI
- Material 3 guidance for behavior and accessibility
- Mobbin when access is available
- UXPeak for focused critique, not passive viewing
- Awwwards for visual/motion inspiration only

### Quality

- Android Studio and Layout Inspector
- Physical Android phone
- Unit tests for validators and state machines
- API integration tests
- Screen recordings
- Secret scanning
- Clean-checkout build test

## Mobbin status

The Mobbin connector is attached, but a search on July 29, 2026 returned a paid-plan requirement. Therefore:

- do not claim that Mobbin references were inspected;
- keep exact queries in `prompts/MOBBIN_QUERIES.md`;
- use Mobbin manually or upgrade only if it is worth the cost;
- do not block the build on it.

## AI image generation workflow

Use image generation to answer:

- What is the emotional tone?
- What is the hero composition?
- How should person, occasion and gift relate visually?
- What could the signature motion look like?

Then translate the chosen direction into:

- design tokens;
- real typography;
- Compose components;
- real interaction states;
- real brand assets.

Do not ask the image model to design an entire production app in one poster. Generate one screen or moment at a time.

## UX research timebox

For each major screen:

1. 10 minutes defining the task and user fear.
2. Up to 20 minutes collecting patterns.
3. 10 minutes writing the WishTrace synthesis.
4. Implement.
5. Review screenshot and interaction.

Research without a documented decision is wasted time.

## Motion choice

1. Native Compose animations first.
2. Rive only if the signature transition requires a reusable state machine and can be proven within 90 minutes.
3. Lottie only for a self-contained success animation from an existing asset.
4. Never use Rive and Lottie together in the MVP.

## Merchant resources

Organizer-provided discovery sources:

- `https://composio.dev/mcp-gateway`
- `https://ucpchecker.com/`
- `https://mcpservers.org/topics/ecommerce-mcp`
- Prava merchant spreadsheet included in organizer announcements

These are leads, not compatibility guarantees. Follow `docs/MERCHANT_VALIDATION.md`.

## Tool freeze

By hour 4, freeze:

- backend language;
- OpenAI integration shape;
- Prava integration shape;
- merchant primary and backup;
- persistence choice.

By hour 8, freeze:

- navigation;
- design tokens;
- animation runtime;
- screen list.

After hour 8, add a tool only when a blocker and rollback are written in `docs/DECISION_LOG.md`.
