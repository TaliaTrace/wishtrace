---
name: wishtrace-openai
description: Implement grounded OpenAI extraction, ranking, message generation, validation and fallbacks.
---
# WishTrace OpenAI Skill

Read `docs/OPENAI_ORCHESTRATION.md` and domain models.

Rules:

- Candidate list comes from commerce adapter.
- Hard invalid candidates are removed before model call.
- Model returns only known IDs and evidence IDs.
- Structured output is mandatory.
- Validate and repair once.
- Fall back safely.
- No payment credentials or authoritative commerce facts in model control.
- Avoid logging private hints.

Output: schema, prompt version, model/access assumption, tests, validation rate, latency, fallback.
