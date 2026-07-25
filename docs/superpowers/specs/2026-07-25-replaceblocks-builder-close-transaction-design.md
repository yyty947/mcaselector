# ReplaceBlocks Builder Transactional Close Design

## Goal

Make Builder closing behave like a transactional editor: only the Builder OK
button applies a valid generated value to the Change NBT ReplaceBlocks field;
cancelled or discarded Builder work leaves that field exactly as it was when the
Builder opened.

## Close behavior

- Loading existing ReplaceBlocks text establishes a clean Builder baseline.
- Closing an unchanged Builder is silent, even when the baseline contains rules.
- Changing rules or draft inputs makes the Builder dirty.
- Confirming a non-empty catalogue switch clears the working Builder and leaves it
  dirty when that differs from the opening baseline.
- Builder Cancel and the title-bar close button use the same discard flow.
- A dirty close offers `Discard changes` and `Continue editing`.
- `Continue editing`, or closing the discard confirmation itself, keeps the
  Builder open.
- `Discard changes` closes the Builder without changing the outer ReplaceBlocks
  field.
- Builder OK continues to return the current valid non-empty generated value.
  Clearing the outer field remains an explicit action in Change NBT.

## Implementation boundary

The Builder records an immutable snapshot of its rule list and both draft inputs
after initial text restoration. Dirty state is computed by comparing current
content with that baseline; popup, validation, selection, and catalogue-only
state are excluded.

The actual Builder window receives a `WINDOW_CLOSE_REQUEST` event filter. This is
required because JavaFX 21's heavyweight Dialog forwards a title-bar close
through the underlying Stage; consuming only `DialogEvent.DIALOG_CLOSE_REQUEST`
does not consume that original window event. The Builder Cancel button calls the
same close decision directly. The OK button bypasses discard confirmation.

## Compatibility

This change does not modify ReplaceBlocks syntax, parsing, generated rule
semantics, catalogue contents, preview, execution, world data, or performance
optimizations. Presets stay versionless, and catalogue switching retains its
existing confirmation/reset behavior.

## Validation

Automated JavaFX tests cover clean baselines, dirty draft/rule/reset states,
title-bar close cancellation, confirmation-window close cancellation, explicit
discard, and Builder Cancel. Manual validation checks the translated button copy
and the same paths in the real dialog.
