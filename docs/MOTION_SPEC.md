# Motion Specification

## Principle

Motion explains state. It does not decorate waiting.

## Signature sequence

### Beat 1: context

Interest and hint chips settle around the recipient card.

### Beat 2: trace

A thin path connects relevant context to three candidate cards.

### Beat 3: filtering

Invalid cards move back and desaturate with short labels:

- over budget;
- arrives late;
- conflicts with dislike.

### Beat 4: selection

The chosen card moves to the center and gains clear emphasis.

### Beat 5: completion

After authoritative payment success, the card folds or wraps, then resolves into a receipt.

## Timing budget

- Context: 400–600ms
- Trace: 500–800ms
- Filtering: 500–800ms
- Selection: 400–600ms
- Receipt resolution: 500–800ms

Total hero sequence: 3–5 seconds.

## Compose implementation

Prefer:

- `AnimatedContent`
- `AnimatedVisibility`
- `updateTransition`
- `Animatable`
- `graphicsLayer`
- Canvas path drawing
- spring for object movement
- tween for trace progress

Use shared-element transitions only if stable in the selected Compose version.

## Reduced motion

When animation scale is reduced or disabled:

- show the same state changes instantly;
- retain rejection labels;
- never hide information behind animation.

## Prohibited

- looping sparkle fields;
- fake thinking orbs;
- confetti on failure;
- long logo intro;
- motion that blocks purchase controls;
- Rive runtime added without a timeboxed proof.
