# OpenAI Orchestration

## Goal

Use OpenAI where language and relationship context matter, while keeping commerce facts and money controls deterministic.

## Pipeline

### Optional extraction

Input: user-provided note, screenshot description or link metadata.

Output:

```json
{
  "interests": ["gaming"],
  "positive_hints": ["mentioned wanting an Xbox game"],
  "dislikes": ["decorative clutter"],
  "confidence": "medium",
  "needs_user_confirmation": true
}
```

The user can edit extracted facts. Do not silently turn inference into permanent truth.

### Candidate ranking

Input:

- recipient context;
- occasion;
- valid candidate snapshots;
- evidence IDs;
- desired response schema.

Output shape:

```json
{
  "selected_candidate_id": "candidate_123",
  "alternative_candidate_ids": ["candidate_456"],
  "evidence": [
    {
      "candidate_id": "candidate_123",
      "evidence_ids": ["interest_gaming", "hint_7"],
      "reason": "Directly matches Sophie's gaming interest and stays within budget."
    }
  ],
  "uncertainty": "low"
}
```

## Prompt constraints

- Use only candidate IDs provided.
- Never create a product, merchant, price or delivery fact.
- Do not override hard rejections.
- Explain direct evidence separately from inference.
- Return a refusal/no-selection when evidence is weak.
- Keep user-facing reason short.

## Validation

After response:

1. Parse against schema.
2. Confirm every ID exists and is valid.
3. Confirm selected candidate was not rejected.
4. Confirm evidence IDs exist.
5. Remove unsupported claims.
6. Retry once if parse/ID validation fails.
7. Fall back to deterministic score or user selection.

## Model strategy

Do not freeze an exact model in planning. At kickoff, benchmark available models for:

- structured-output reliability;
- latency;
- cost/credits;
- multimodal access if hints use images;
- message quality.

Use the smallest model that reliably meets the task. Reserve a stronger model for final ranking only if it materially improves evidence quality.

## Personal message

Generate a draft from approved recipient context. Rules:

- no fabricated shared memories;
- avoid overly intimate wording unless context supports it;
- user must be able to edit;
- preserve sender voice when examples are provided;
- no model-generated message is sent without the user's visible review in the MVP.

## Reliability metrics

Track locally:

- schema-valid rate;
- candidate-ID-valid rate;
- average latency;
- fallback rate;
- user changes to selected gift;
- user edits to message.
