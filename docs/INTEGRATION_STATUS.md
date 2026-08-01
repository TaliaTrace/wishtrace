# Integration Status

Update this with observed facts, IDs and dates. Do not leave a successful spike only in chat history.

## Prava

- Environment: sandbox planned
- Path: SDK/API planned
- Dashboard/API key created:
- Authentication request verified:
- Session creation verified:
- Hosted approval verified:
- App return verified:
- Webhook verified:
- Authoritative success verified:
- Decline/cancel/unknown tested:
- Production access requested:
- Known blockers:
- Last verified:
- Evidence location:

## Commerce

- Primary merchant/path:
- Backup merchant/path:
- Mode: live / controlled / hybrid
- Search verified:
- Product detail verified:
- Price refresh verified:
- Availability verified:
- Delivery data verified:
- Quote/total verified:
- Checkout compatibility verified:
- Known blockers:
- Last verified:
- Evidence location:

## OpenAI

- Account/project used:
- Model selected:
- Structured extraction verified:
- Structured ranking verified:
- Multimodal verified:
- Message generation verified:
- Invalid-output fallback tested:
- Latency:
- Known blockers:
- Last verified:

## Android

- Application ID: `com.wishtrace.app`
- Build command: `cd android; .\gradlew.bat :app:assembleDebug` (verified 2026-07-30)
- Device/emulator: `Doomo_API_31` (API 31) launched headlessly and verified 2026-07-30
- Navigation verified: five-page onboarding → Google sign-in shell → local `Not now` session → Home/People/Occasions/Profile; recipient detail; two-step person/occasion editor; four-stage Discovery → source-needed Recommendation → Personal note. Verified by 2 connected Compose UI tests.
- Google auth client: Credential Manager `1.6.0` and Google ID `1.2.0` compiled; `WISHTRACE_GOOGLE_WEB_CLIENT_ID` resource injection compiled
- Google account validation: NOT VERIFIED; web client ID and backend token exchange are not configured
- API connection verified:
- Custom tab/hosted approval verified:
- App link verified:
- Process recreation tested:
- Accessibility checked: primary/back targets asserted at 48dp; semantic headings/labels and onboarding page semantics present; core contrast pairs are 5.95:1–14.91:1; onboarding captured at 130% text scale; motion snaps when animator duration is disabled. TalkBack/manual switch-access testing remains pending.
- Evidence location: `artifacts/screenshots/milestone-4/`, `artifacts/screenshots/milestone-3/`, `artifacts/screenshots/milestone-2/`, `android/evidence/`, `android/app/build/reports/androidTests/connected/debug/`
- Known blockers: backend and all external integrations are not implemented; API 37 is not installed, so API-36-compatible stable Core/Lifecycle versions are pinned. API 31 AVD cold launch has substantial OpenGL jank; physical-phone startup/motion profiling is pending.

## Demo

- Bronze flow repeat count:
- Primary video:
- Backup video:
- Five-second test:
- Clean-checkout build:
- Submission URL:
