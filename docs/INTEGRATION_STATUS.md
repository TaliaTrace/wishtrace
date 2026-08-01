# Integration Status

Update this with observed facts, IDs and dates. Do not leave a successful spike only in chat history.

## Prava

- Environment: sandbox configured in ignored local environment
- Path: hosted full-checkout API through the backend; official polling + report-status
- Dashboard/API key created: YES (user-provided environment; value never recorded here)
- Authentication request verified: PRE-KICKOFF ONLY; judged-window re-verification pending
- Session creation verified:
- Hosted approval verified:
- App return verified:
- Payment-result polling verified:
- Report-status verified:
- Real-merchant browser attempt verified:
- Authoritative success verified:
- Decline/cancel/unknown tested:
- Production access requested: NO — intentionally gated behind organizer-required sandbox evidence
- Known blockers: no judged-window session, tokenized card attempt or authoritative result yet
- Last verified: 2026-08-01 documentation/announcement review
- Evidence location:

Organizer truth boundary: production access requires the sandbox integration to work end to end in
the Android app and a tokenized test-card transaction to be attempted through browser automation
against a real merchant. The expected sandbox merchant failure is accepted; it is not an order.

## Commerce

- Primary merchant/path: HyperX US UCP lead; not yet proven
- Backup merchant/path: Turtle Beach USA UCP lead; not yet proven
- Mode: runtime must be live; no controlled fallback
- Search verified:
- Product detail verified:
- Price refresh verified:
- Availability verified:
- Delivery data verified:
- Quote/total verified:
- Checkout compatibility verified:
- Known blockers: advertised UCP capability is a lead, not proof of product lookup, quote or checkout
- Last verified: pre-kickoff directory/profile research only; judged-window live calls pending
- Evidence location:

## OpenAI

- Account/project used: Azure AI Foundry, configured in ignored environment
- Model selected: configured Azure deployment; name intentionally not duplicated in docs
- Structured extraction verified:
- Structured ranking verified:
- Multimodal verified:
- Message generation verified:
- Invalid-output fallback tested:
- Latency:
- Known blockers: structured ranking against live candidate IDs is not implemented
- Last verified: pre-kickoff Responses API smoke response only; judged-window orchestration pending

## Supabase PostgreSQL

- Path: session pooler on port 5432 with SQLAlchemy async psycopg 3 and `NullPool`
- Client TLS verified: YES via libpq `ssl_in_use`
- Server version observed: PostgreSQL 17.6
- Migration status: `20260801_0001 (head)`
- Migration content: foundation/Alembic version only; product tables not created yet
- Permanent local `.env` contains `sslmode=require`: USER CONFIRMATION PENDING; verification used a process-only secure override
- Last verified: 2026-08-01 during official window

## Android

- Application ID: `com.wishtrace.app`
- Build command: `cd android; .\gradlew.bat :app:assembleDebug` (verified 2026-08-01)
- Device/emulator: physical `RMX3201`, Android 11/API 30, serial redacted from public evidence; Google Play Services present and ADB authorized
- Navigation verified: five-page onboarding → Google sign-in shell → local `Not now` session → Home/People/Occasions/Profile; recipient detail; two-step person/occasion editor; four-stage Discovery → source-needed Recommendation → Personal note. Verified by 2 connected Compose UI tests.
- Google auth client: Credential Manager `1.6.0` and Google ID `1.2.0` compiled; `WISHTRACE_GOOGLE_WEB_CLIENT_ID` resource injection compiled
- Google account validation: NOT VERIFIED; configured client IDs still require real challenge/exchange on the physical phone
- API connection verified:
- Custom tab/hosted approval verified:
- App link verified:
- Process recreation tested:
- Accessibility checked: primary/back targets asserted at 48dp; semantic headings/labels and onboarding page semantics present; core contrast pairs are 5.95:1–14.91:1; onboarding captured at 130% text scale; motion snaps when animator duration is disabled. TalkBack/manual switch-access testing remains pending.
- Evidence location: `artifacts/screenshots/milestone-4/`, `artifacts/screenshots/milestone-3/`, `artifacts/screenshots/milestone-2/`, `android/evidence/`, `android/app/build/reports/androidTests/connected/debug/`
- Known blockers: runtime still wires pre-kickoff seeded repositories and a local auth bypass; these are forbidden in the judged path and must be removed as the backend endpoints land.

## Demo

- Bronze flow repeat count:
- Primary video:
- Backup video:
- Five-second test:
- Clean-checkout build:
- Submission URL:
