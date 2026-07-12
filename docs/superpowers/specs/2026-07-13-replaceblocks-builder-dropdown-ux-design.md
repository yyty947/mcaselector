# ReplaceBlocks Builder Dropdown UX Design

Date: 2026-07-13
Status: Approved for implementation
Scope: ReplaceBlocks Builder dropdowns only

## Goal

Make Builder dropdowns easier to navigate and visually read while preserving the existing autocomplete, rule generation, and advanced text workflows.

## Shared Dropdown Behavior

Use one Builder-local behavior layer for preset, From, To, property, Extra NBT, and biome ComboBoxes. Do not replace JavaFX ComboBox with a custom control and do not implement separate navigation behavior for each field.

Editable block and biome ComboBoxes retain their existing filtered autocomplete, mouse completion, Tab/Enter completion, and biome semicolon-token completion. Property, Extra NBT, and preset ComboBoxes retain their existing value models.

## Boundary-Aware Keyboard Scrolling

- `Up` and `Down` move the highlighted candidate by one item.
- The popup remains stationary while the new highlight is already fully visible.
- The popup scrolls by the minimum required amount only when the highlight crosses the current top or bottom visible boundary.
- `PageUp` and `PageDown` move by the current visible-page size and reveal the destination without repeatedly repositioning already-visible rows.
- Navigation changes only the Builder's independent popup highlight. It must not write a candidate into an editable ComboBox, change its value or JavaFX selection/focus model, or rebuild the filtered list.
- Tab, Enter, or a mouse click remains the explicit completion action.

## Dropdown Visual States

Every Builder dropdown popup receives a dedicated Builder style class so the styling does not leak into unrelated MCA Selector dialogs.

- Normal rows keep the existing dark background and light text.
- Mouse hover uses a clearly visible translucent/light blue background.
- Keyboard focus and the current selected item use a solid `#4aa3ff` blue background, white text, and a clear blue edge or inset border.
- Focused text must remain readable, including the blue substring highlight used by From, To, and biome autocomplete rows.
- Hover and focus must not resize rows or shift popup layout.

The styling applies to presets, From, To, property values, Extra NBT, and biome dropdowns.

## Explicit Empty-Query Expansion

From, To, and biome inputs remain blank when the Builder opens and do not open automatically.

When the user explicitly clicks an empty editable ComboBox's arrow button:

- From and To show the complete A-Z block catalog.
- Biome shows the complete A-Z biome catalog.
- The full list is shown only for this explicit expansion action; empty typing/focus events do not automatically open it.
- Once the user types, the popup immediately returns to the existing filtered mode.
- Clearing typed text closes the automatically filtered popup; another explicit arrow click is required to show the full catalog again.

Unknown or modded IDs remain manually enterable.

## Error Handling and Compatibility

- Preserve the popup-Scene key capture that prevents native JavaFX selection from rewriting editor text.
- Do not synchronously replace popup items from inside native mouse-selection callbacks.
- An unavailable popup skin or temporarily missing visible-cell information must fail safely: navigation may conservatively reveal the target, but must not commit a value or throw.
- Do not change ReplaceBlocks parsing, generated syntax, preview, execution, presets, or world data.

## Automated Tests

- Highlight movement inside the visible range does not request scrolling.
- Crossing the top or bottom visible boundary requests the minimum reveal operation.
- PageUp/PageDown calculate the correct destination and reveal it only when necessary.
- Popup navigation preserves editable text, ComboBox value, and selection state.
- Empty explicit expansion populates the full block or biome catalog.
- Empty typing/focus does not automatically open the full catalog.
- Builder popup style hooks are attached to all ComboBox categories.

## Manual UI Acceptance

The user performs the final JavaFX validation after restarting the development build:

- Navigate within a fully visible five-row window and confirm the list stays still until the highlight attempts to cross its top or bottom edge.
- Verify PageUp/PageDown move and scroll by one visible page.
- Verify hover is visibly light blue and keyboard focus/selection is solid blue with readable white text in every Builder dropdown category.
- Click empty From, To, and biome arrow buttons and confirm full sorted candidate lists appear without causing automatic initial popups.
- Type and clear queries, close an empty full catalog with Esc, verify another explicit arrow click is required to reopen it, complete by Tab/Enter/mouse, and confirm no list collapse or JavaFX console exception.

## Non-Goals

This change does not restyle non-Builder dropdowns, add new block or biome data, change popup row height, alter Builder layout, or modify ReplaceBlocks parser/execution behavior.
