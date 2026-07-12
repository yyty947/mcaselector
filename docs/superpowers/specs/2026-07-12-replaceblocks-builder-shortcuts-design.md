# ReplaceBlocks Builder Shortcuts Design

Date: 2026-07-12
Status: Implemented
Scope: ReplaceBlocks Builder only

## Goal

Improve keyboard navigation for the Builder's existing controls and reduce the number of mouse trips for the two most common rule operations. The design must preserve normal text editing behavior and avoid introducing a large or difficult-to-discover shortcut system.

## Recommended Shortcut Set

### List navigation

When an editable block, biome, preset, or property ComboBox popup is open and owns focus:

| Key | Behavior |
|---|---|
| `Up` / `Down` | Move the highlighted item by one entry. |
| `PageUp` / `PageDown` | Move the highlighted item by one visible page. |
| `Enter` | Accept the highlighted item when a popup is open. |
| `Esc` | Close the popup without changing the current value. |

The controls keep their normal JavaFX behavior when no popup is open. Text fields must continue to accept cursor movement, selection, typing, Tab completion, and mouse completion without being redirected to the end of the value.

### Rule operations

| Key | Active context | Behavior |
|---|---|---|
| `Delete` | Rules table has focus and one rule is selected | Delete the selected rule using the existing delete action. |
| `Ctrl+Enter` | A Builder input or property control has focus | Invoke the existing Add rule action using the current draft. Existing validation and error messages remain authoritative. |
| `Esc` | Rules table has focus | Clear the table selection if a row is selected. |

`Delete` must not delete text when any text input or editable ComboBox editor has focus. `Ctrl+Enter` must not bypass validation or create a rule from incomplete input.

### Rule table multi-selection

The rules table keeps native Ctrl/Shift row selection and additionally supports marquee selection from its empty area. Hold the left mouse button in empty table space and drag over rows to select every row intersecting the translucent rectangle. A normal marquee replaces the current selection; Ctrl-marquee adds its intersecting rows.

Saving a custom preset serializes every selected row, in table order. A valid current draft is considered only when no rows are selected, and all table rules are used only when neither a selection nor a draft exists. Editing remains a single-row operation; the edit button is disabled for zero or multiple selected rows. Delete removes every selected rule.

## Deliberately Excluded Shortcuts

- `Ctrl+A` is excluded because it must retain standard text-field select-all behavior. Selecting all table rows would also create ambiguous save-preset semantics.
- `Ctrl+S` is excluded because saving a preset may require a name prompt and overwrite confirmation; the visible button is clearer.
- `Ctrl+P` is excluded because Preview is not the primary repeated action and may conflict with application-level conventions.
- Undo/redo is excluded because it would require history across draft inputs, rule rows, property selections, and preset changes.

## Focus Routing

Shortcut handling should be local to the focused control or Builder region:

1. ComboBox popup navigation is handled by the popup/list control and must not replace its item list synchronously during mouse or keyboard selection.
2. Text input controls retain native cursor, selection, typing, Tab, and mouse-completion behavior.
3. The rules table handles `Delete` and selection-clearing `Esc` only while it owns focus.
4. Builder input controls handle `Ctrl+Enter` by invoking the same method as the Add rule button.
5. The dialog's normal `Enter`, `Esc`, `OK`, and `Cancel` behavior must remain unchanged outside these explicit contexts.

No global key filter should reinterpret ordinary keys across the entire dialog.

## Validation and Error Handling

- `Ctrl+Enter` uses the existing draft parser and diagnostics.
- Invalid, incomplete, duplicate, or empty drafts show the same diagnostic as clicking Add rule.
- `Delete` reuses the existing selected-row delete action and updates generated output and button state through the existing listeners.
- Popup navigation must not produce JavaFX selection/index exceptions.
- Shortcut handling must not change preset save precedence or rule-table selection semantics.

## Testing Scope

### Automated tests

- Test the pure shortcut-context decision logic if a helper is introduced.
- Verify invalid drafts cannot be added through the keyboard path.
- Verify `Ctrl+Enter` and the Add rule button produce equivalent generated rules for valid drafts.
- Verify Delete is enabled only for selected rule-table rows and does not apply to text input controls.
- Verify a marquee selects every visible row it intersects, and Ctrl-marquee adds to an existing selection.

### Manual UI regression

In both English and Chinese locales:

- Open From/To, biome, property, and preset lists; verify Up/Down, PageUp/PageDown, Enter, Esc, Tab, and mouse completion.
- Place the caret in the middle of an editable input, select text, and type; confirm the caret and selection are preserved.
- Focus the rules table, select rows with Ctrl/Shift and marquee, delete selected rules, press Esc to clear selection, and verify saving a preset still follows the documented precedence.
- Focus each Builder input, use Ctrl+Enter for valid and invalid drafts, and verify the same result as Add rule.
- Confirm Delete does not remove text while an input or editable ComboBox editor has focus.
- Confirm no JavaFX exceptions appear in the console.

## Non-Goals

This change does not add a shortcut customization UI, change CLI syntax, modify the main MCA Selector shortcuts, add undo/redo, or implement shortcuts for the five deferred language resources (`cs_CZ`, `hu_HU`, `sv_SE`, `tr_TR`, `uk_UA`).
