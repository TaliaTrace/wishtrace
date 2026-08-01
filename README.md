# WishTrace Hackathon Kit

This folder is the operating system for building **WishTrace** during the Prava Agentic Commerce Hackathon.

WishTrace helps busy people remember important occasions and send thoughtful gifts on time. A user adds loved ones, dates, interests, hints and a budget. Near an occasion, WishTrace discovers real products, checks hard constraints, selects a defensible gift, completes the approved purchase through Prava and attaches a personal message.

## Start here

Read in this order:

1. `PLAN.md` — what is being built and in what order.
2. `AGENTS.md` — mandatory rules for Codex and any coding agent.
3. `EXECUTION_BOARD.md` — the live 44-hour board.
4. `docs/INTEGRATION_STATUS.md` — current truth about Prava, merchant and OpenAI integrations.
5. The relevant local skill under `.agents/skills/` before a focused task.

## First Codex instruction

Open `CODEX_BOOTSTRAP.md` and give it to Codex from the repository root. Do not ask Codex to “build the entire app.” The bootstrap forces it to inspect the repository, prove the integrations early and work in small verified slices.

## Current fixed decisions

- Native Android with Jetpack Compose.
- A backend owns secrets, OpenAI orchestration, merchant access and Prava integration.
- Prava SDK/API is the default because the handbook recommends it for native embedded experiences and it supports sandbox testing.
- One recipient, one occasion, one validated merchant path and one completed transaction are enough for the MVP.
- The model ranks only real candidate products supplied by the commerce adapter. It never invents products, prices, availability or delivery promises.
- Hard constraints are enforced by deterministic code.
- Primary targets are the open Finalists award, Visa Intelligent Commerce and Best UX; OpenAI and Localhost strengthen the same narrow flow.
- UX is a major prize objective, but it cannot delay proof of a working end-to-end transaction.
- Linq is optional and may be added only after the main Android + Prava demo is stable; recipient-only notification does not count as a strong Linq implementation.
- Generated UI images are art direction, not proof of a working product and not a pixel-perfect source of truth.

## Package map

```text
PLAN.md                     Hackathon strategy, scope and timeline
AGENTS.md                   Repository rules for coding agents
TOOLKIT.md                  Frozen tools and fallback choices
EXECUTION_BOARD.md          Live task board and stop rules
CODEX_BOOTSTRAP.md          First prompt and task sequence for Codex

docs/
  PRODUCT_SPEC.md           Product behavior and acceptance criteria
  UX_RESEARCH.md            UX synthesis, research plan and five-second tests
  DESIGN_SYSTEM.md          Visual, content and accessibility system
  SCREEN_MAP.md             Screen-by-screen requirements and states
  MOTION_SPEC.md            Signature interaction and motion budget
  ARCHITECTURE.md           Client, backend, adapters and data contracts
  OPENAI_ORCHESTRATION.md   Grounded model workflow and schemas
  PRAVA_INTEGRATION.md      Payment flow, state machine and evidence
  MERCHANT_VALIDATION.md    Merchant selection and fallback matrix
  QA_AND_TESTING.md         Functional, UX, security and demo tests
  DEMO_AND_SUBMISSION.md    Script, evidence, submission and backup plan
  STARTUP_STORY.md          Localhost-ready wedge and continuation story
  TRACK_STRATEGY.md         Prize targets, proof matrix and optional Linq gate
  RISK_REGISTER.md          Risks, triggers and mitigations
  DECISION_LOG.md           Consequential decisions only
  INTEGRATION_STATUS.md     Current verified integration state
  PREEXISTING_WORK.md       Required disclosure template

prompts/
  IMAGE_GENERATION.md       Art-direction prompts for each hero screen
  MOBBIN_QUERIES.md         Exact searches to run when access works
  CODEX_TASKS.md            Safe task prompts for implementation
  JUDGE_QA.md               Questions judges may ask and proof required

.agents/skills/             Repository-local specialist instructions
references/                 Handbook, RFH and Discord notes
assets/concepts/            Generated concepts, explicitly non-production
```

## Source hierarchy

When documents conflict, follow this order:

1. Latest official Prava docs and observed API behavior.
2. Latest Prava team announcement in Discord.
3. Builder Handbook hard rules and deadline section.
4. Devfolio live rules and schedule.
5. `PLAN.md` and `AGENTS.md`.
6. Old notes and generated concepts.

Update `docs/DECISION_LOG.md` whenever a higher-priority source forces a change.

## Definition of success

The submission is successful when a judge can watch one short flow and verify:

```text
occasion + recipient context
→ real candidates discovered
→ one choice explained
→ spending boundary understood
→ Prava sandbox transaction completed
→ receipt/result shown
→ personal message attached
```

A narrower working flow beats a broad unfinished app. Runtime routes never substitute seeded,
controlled or simulated content when an integration is unavailable; they show a truthful empty,
unavailable or recovery state instead.

## Backend

The FastAPI service lives in `backend/`. It requires Python 3.12 and `uv`, loads secrets from the
ignored repository-root `.env`, rejects PostgreSQL connections without `sslmode=require`, and
publishes `/health` plus the public UCP platform profile. Authenticated recipient, discovery,
grounded-ranking and purchase-ledger routes are implemented. The locked Playwright image is deployed
to Azure Container Apps, and the staging runtime exposes the exact allowlisted Jackbox $5 path for
the organizer-required Prava sandbox attempt. This does not assert production stored-value support.

```powershell
cd backend
uv sync --all-groups
$env:PYTHONPATH = (Get-Location).Path
uv run pytest
uv run ruff check app tests migrations scripts
uv run mypy app scripts
uv run alembic upgrade head
uv run alembic check
uv run python -m scripts.run_server
```

Copy variable names from `templates/.env.example`; never commit the populated `.env`.

## Android client

The native client lives in `android/`. Runtime authentication, recipient, occasion and Home state
come from the WishTrace backend; seeded repositories and the local authentication escape hatch
have been removed. Compose previews and tests may use isolated fixtures. Runtime discovery, ranking,
exact quote, Prava Custom Tab handoff, backend reconciliation and personal-message persistence call
the public HTTPS backend; no Android route substitutes preview data when a provider is unavailable.

Requirements:

- Android Studio Quail 2 or compatible;
- JDK 17 or newer;
- Android SDK 36;
- an Android emulator or device with API 23 or newer.

Windows commands:

```powershell
cd android
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Debug APK:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

Rendered prototype evidence is in `android/evidence/`. Connected guardrail tests cover onboarding
to required Google authentication. Real OAuth has also been exercised on the Play-enabled physical
phone against the public Azure backend; no Google token or bearer token is captured.

This implementation began before the official build window at the user's explicit direction and is recorded in `docs/PREEXISTING_WORK.md`. It must not be represented as judged-window work.
