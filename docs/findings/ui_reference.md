# UI Reference For ReplaceBlocks Builder

Date: 2026-06-04
Last updated: 2026-07-07

Scope: targeted JavaFX UI reference and current ReplaceBlocks UI behavior.

## Best integration point

Current entry: `src/main/java/net/querz/mcaselector/ui/dialog/ChangeNBTDialog.java`.

Reason:

- It already owns the NBT Changer field list.
- It already creates every non-headless `FieldType`, including `ReplaceBlocks`.
- It already syncs field-row input with the advanced `changeQuery` string.
- It already returns `ChangeNBTDialog.Result(fields, force, selectionOnly, scriptField)` consumed by `DialogHelper`.
- The current implementation keeps the builder and preview entry in this dialog while leaving real mutation in `FieldChanger`.

## Existing UI files worth referencing

### `ChangeNBTDialog.java`

Useful patterns:

- `Dialog<Result>` with `setResultConverter(...)`.
- Uses `FieldType.uiValues()` to create all field rows.
- `FieldCell` owns a label and `TextField`.
- Per-field validation is done by calling `value.parseNewValue(newValue)`.
- Valid input toggles a `valid` pseudo-class on the text field.
- `changeQuery` is a generated advanced text representation of all changed fields.
- `changeQuery.setOnAction(...)` parses advanced text and updates the field rows.
- The main tab already has the right shape: scrollable builder area plus an advanced query text field.

Current use:

- The ReplaceBlocks row keeps the raw field text input.
- A `Builder` button opens `ReplaceBlocksRuleBuilderDialog`.
- Builder has a button-bar `Preview` button beside Help. It runs a ReplaceBlocks dry-run for the current generated Builder value.
- ReplaceBlocks field-row diagnostics are delayed until typing pauses briefly; advanced-query parse errors are surfaced near the relevant input.
- The default NBT Changer dialog size keeps the ReplaceBlocks `Builder` button visible without horizontal scrolling.

### `FilterChunksDialog.java`

Useful patterns:

- Has a visual builder (`GroupFilterBox`) plus an advanced query text field (`filterQuery`).
- Builder updates the query string when valid.
- Query string can be parsed back into the visual builder on Enter.
- Disables OK when the visual model is invalid.
- Layout is close to `ChangeNBTDialog`: scrollable main editor, separator, raw query, tabs, and bottom options.

Recommended use:

- Model ReplaceBlocks after this two-way sync pattern, but start one-way for MVP: builder generates text; advanced text remains editable.

### `FilterBox.java`

Useful patterns:

- Uses small icon labels for add/delete controls.
- Uses tooltips from `UIFactory.tooltip(...)`.
- Uses `GridPane` for compact controls.
- Encapsulates add/delete behavior in methods and calls an update listener.

Recommended use:

- A ReplaceBlocks rule row can use compact add/delete controls similar to filter rows.
- Avoid touching drag/drop for MVP.

### `OverlayEditorDialog.java`

Useful patterns:

- Maintains a list of model objects and a parallel `VBox` of row components.
- Add operation appends to both model and UI.
- Delete operation removes by index from both model and UI.
- Uses a scroll pane containing a `VBox` list.

Recommended use:

- Use a simple `List<ReplaceRule>` plus `VBox rulesList`.
- Add and delete rules exactly update both the list and the generated ReplaceBlocks string.

### `OverlayBox.java`

Useful patterns:

- A row component with inputs on the left/center and action controls on the right.
- Uses `ComboBox`, `TextField`, `CheckBox`, `Label` with image icons, and tooltips.
- Uses pseudo-classes for invalid/selected state.

Recommended use:

- A ReplaceBlocks rule row can be an `HBox`, `GridPane`, or `BorderPane` with:
  - from block field
  - to block field
  - delete icon/button
  - validation style

### `EditArrayDialog.java`

Useful patterns:

- `TableView` with editable cells.
- Icon-only edit button.
- Selection-aware button enable/disable.

Recommended use:

- If the rule list outgrows simple rows, a `TableView` with From and To columns is a natural phase-2 UI.
- For MVP, a `VBox` of rows is probably simpler and closer to the existing field UI.

### `UIFactory.java`

Useful patterns:

- `UIFactory.label(...)`
- `UIFactory.button(...)`
- `UIFactory.tooltip(...)`
- `UIFactory.checkbox(...)`
- `UIFactory.radio(...)`
- `UIFactory.tab(...)`

Recommended use:

- Use existing factory methods for translated labels, buttons, tabs, and tooltips.
- New text should go through `Translation`, not hard-coded UI strings, when implementing.

## Current ReplaceBlocks UI

Implemented controls:

- Existing advanced ReplaceBlocks text input remains available.
- `Builder` dialog has searchable, editable `from` and `to` block selectors with auto-opening A-Z filtered suggestions, Tab/click completion, and no empty-query full-list popup.
- Builder helper text below the generated value is shown before manual From/To input and hidden after the user types non-empty text into either field.
- `Builder` dialog has add/load-for-edit/delete controls and a rule table.
- The generated ReplaceBlocks string is shown and returned to the existing field row.
- Generated values are validated through `ReplaceBlocksField.parseNewValue(...)`.
- Empty builders start with blank From/To inputs and do not show an empty-rule validation error before user action.
- The Builder `Preview` button runs dry-run counts without saving region data.
- Preview output includes aggregate counts, one row per rule with source mode/source text/target text/matched blocks, and an overlap warning when multiple rules match the same original position.
- Error text distinguishes common invalid values and source regex warnings.
- Java 1.21.9 catalog data is wired into the builder for property dropdowns.
- The source tile/block entity selector is presented as `Extra NBT: any/present/absent`, and the Builder has a Help button that opens a separate explanation dialog.
- The source side has optional Min Y / Max Y inputs. Empty fields mean no Y restriction; filling either field wraps the generated source with `y(...)`.
- The source side has an optional searchable Biome input. Empty means no biome restriction; one or more biome IDs separated by semicolons wrap the generated source with `biome(...)`. The dropdown suggests known vanilla biome IDs, completes the current semicolon-separated token with Tab or mouse click, and keeps the empty-query list empty. Help text states that matching is block-position aware at the modern 4x4x4 biome-cell granularity.
- The Builder has a Preset row. Built-in presets fill visible editable From/To and source condition controls for common starting points instead of adding hidden behavior; air and container/data-block presets show warning text.
- The Builder can save the current generated ReplaceBlocks value as a custom global preset. Custom presets load through the same parser path into the rule table and can be overwritten or deleted.

Builder-supported rule syntax:

- simple source block names
- simple target block names
- catalog-backed block selection with property dropdowns; each property has an `all`/`全部` option
- source block state SNBT for exact block-state matching
- target block state SNBT
- source tile/block entity eligibility (`Extra NBT: any`, `tile(...)`, `no_tile(...)`)
- source Y range filtering (`y(min..max, source)`)
- source biome filtering (`biome(<biome>[;<biome>...], source)`)
- no rich target tile entity builder yet
- advanced users can still type tile SNBT, quoted custom values, and complex values manually in the raw field

Possible generated value:

```text
literal(minecraft:stone)=minecraft:dirt, literal(minecraft:oak_log)=minecraft:birch_log
```

Y-restricted generated value:

```text
y(-64..64, literal(minecraft:stone))=minecraft:dirt
```

Exact source-state example:

```text
{Name:"minecraft:oak_stairs",Properties:{facing:"north",half:"bottom",shape:"straight",waterlogged:"false"}}={Name:"minecraft:oak_stairs",Properties:{facing:"south",half:"bottom",shape:"straight",waterlogged:"false"}}
```

## Suggested implementation placement

Option A: nested component in `ChangeNBTDialog`

- Fastest for MVP.
- Can directly access the existing field list and field-row behavior.
- Less reusable.

Option B: new component under `ui/component`

- Cleaner if the builder grows.
- Suggested name: `ReplaceBlocksRuleBuilder`.
- It should expose generated text and validity as properties/listeners.

Current implementation: a small dialog class, `ReplaceBlocksRuleBuilderDialog`, opened from `ChangeNBTDialog`.

## Block-state catalog usage

Available data source:

- `src/main/java/net/querz/mcaselector/version/mapping/blockstate/BlockStateCatalog.java`
- `src/main/resources/mapping/block_states/java_1_21_9.json`

Useful API:

- `BlockStateCatalog.latestJava()`: loads the current Java catalog.
- `blockNames()`: returns normalized `minecraft:` block IDs.
- `properties(blockName)`: returns property names and allowed values.
- `defaultProperties(blockName)`: returns the default state values from Mojang reports.
- `containsBlock(...)`, `hasProperty(...)`, and `isValidPropertyValue(...)`: basic validation helpers.

The catalog was generated from Mojang 1.21.9 server `reports/blocks.json` and stores only UI-useful data: block IDs, property values, default properties, and state counts. It is independent from `BlockRegistry`; `BlockRegistry` remains a lightweight ID validator and does not provide per-block property schema.

Current 4B usage:

- The catalog populates the block-name search list.
- Known vanilla blocks render one property row per catalog property.
- Property dropdowns start at `all`/`全部`; catalog default properties remain available as data, but they are not the current initial UI selections.
- Unknown/modded IDs remain manual entries without property rows.
- The builder generates existing ReplaceBlocks text from selected catalog values.
- Simple source IDs serialize as `literal(...)`.
- Source Min Y / Max Y fields serialize by wrapping the source expression in `y(min..max, source)`.
- Source Biome field serializes by wrapping the source expression in `biome(<biome>[;<biome>...], source)`.
- Catalog-backed source property rules serialize as `props(...)` when at least one property is not `all`; leaving every source property at `all` serializes as `literal(...)`.
- Target property dropdowns omit properties left at `all`, and a target with every property at `all` serializes as the simple target block name.
- Existing source SNBT remains exact matching; selected-property matching is explicit through `props(...)`.

## Constraints for future implementation

- Do not alter existing simple ReplaceBlocks syntax.
- Keep the advanced text path working.
- Treat the generated string as compatibility output.
- Do not reinterpret generated source SNBT as selected-property matching; use `props(...)` for selected-property matching.
- Dedicated source-mode UI controls and per-property subset checkboxes are still pending polish.
- Preserve per-rule preview counts before layering on more conditions.
- Source tile entity eligibility is implemented and documented in the Builder Help dialog; keep future Builder help content in that dialog instead of adding more permanent helper text to the main form.
- Rich target tile NBT editing is still pending.
- Biome restrictions and presets are implemented; release hardening is still pending.

## Builder UI implementation notes

- The From/To block selector is an editable JavaFX `ComboBox`, so keyboard completion and mouse-click completion are not equivalent internally. Test both paths before shipping input changes.
- The Biome input is also an editable JavaFX `ComboBox`, but completion replaces only the current semicolon-separated token.
- Built-in presets should continue to fill the existing visible controls and use the normal Add rule path. Do not let future built-ins bypass generated text validation or hide source mode, Extra NBT, Y range, or biome conditions.
- Custom presets should remain full generated ReplaceBlocks text stored in global config, not serialized UI widget state. This keeps advanced syntax round-trippable and prevents future Builder layout changes from invalidating saved presets.
- Avoid changing the ComboBox value, selection, or item list synchronously while JavaFX is handling popup mouse selection. This previously caused `ListViewBehavior` index errors when users clicked a suggestion.
- Keep the empty-query suggestion list empty. Showing the whole block catalog before the user typed made the first popup visually noisy and could place it above the input.
- Keep hover/focus/selected styling on suggestion and property dropdown cells; otherwise the dark popup looks inert even when it is interactive.
