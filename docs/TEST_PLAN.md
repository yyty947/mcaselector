# ReplaceBlocks Test Plan

Date: 2026-06-04
Last updated: 2026-07-17

Safety rule: never test on a real world save. Always copy a small test world and keep an untouched backup.

## Phase 6 release gates

Phase 6 is complete only when every required gate below passes on the same candidate commit. A focused rerun may validate a fix while it is being developed, but it does not replace the final full gate.

Version and capability matrix:

| World format | Ordinary / legacy rules | `literal` / `regex` / state matching | Tile, Y, and biome conditions | Preview |
|---|---|---|---|---|
| Java 1.9 classic IDs | Existing exact legacy IDs and literal IDs | Unsupported source forms fail closed | Unsupported and must fail closed | Unsupported |
| Java 1.13-1.17 palettes | Preserved | Evaluated against available block-state compounds | Unsupported and must fail closed | Unsupported |
| Early flat 1.18 snapshots (21w37a path) | Supported | Supported | Supported with position, block entity, Y, and biome context | Supported |
| 1.18 release and newer (21w43a path) | Supported | Supported | Supported with position, block entity, Y, and biome context | Supported |

Required gates:

| ID | Gate | Required evidence |
|---|---|---|
| `AUTO-01` | Java compile and all JUnit tests | Successful command output and test count |
| `AUTO-02` | Translation completeness | No missing ReplaceBlocks translation keys |
| `PKG-01` | `build shadowJar` | Successful build output |
| `PKG-02` | Windows `jpackage` | Successful package output, or a recorded environment blocker that is resolved before PR |
| `UI-01` | Chinese Builder regression pass | Completed checklist, screenshot, and clean console |
| `UI-02` | English Builder regression pass | Completed checklist, screenshot, and clean console |
| `UI-CATALOG` | Focused catalogue-switch reset | Direct empty switch, Cancel preservation, Confirm full reset, and custom-preset advisory verified by the user |
| `WORLD-18` | Disposable Java 1.18.x world | Preview/execution comparison, world reload, and log check |
| `WORLD-21` | Disposable Java 1.21.x world | Preview/execution comparison, world reload, and log check |
| `WORLD-LATEST` | Disposable latest snapshot world | Preview non-mutation, conditional execution, Change/Force parity, and selection boundary |
| `DOC-01` | Internal docs alignment | No stale phase status or compatibility claims |

Release blockers:

- any preview path writes region, entities, or poi files
- preview counts differ from execution on a fresh copy without an explained overlap case
- unsupported old formats ignore a source condition and broaden the replacement
- duplicate or stale block entities remain after replacement
- modern heightmaps are absent, malformed, or written to the wrong NBT level
- Minecraft reports chunk, palette, block entity, lighting, or heightmap errors after load and reload
- Builder input, popup, property restoration, or preset actions log JavaFX exceptions
- required translations render as raw keys

Phase 6 execution record:

| Gate | Status | Commit / environment | Evidence or remaining action |
|---|---|---|---|
| Focused parser, Builder model, legacy safety, modern preview, light, and heightmap tests | Passed | Windows, Java 21, 2026-07-12 | Added semantic preset normalization, Change/Force parity, and one-chunk relight-ring regressions |
| `AUTO-01` | Passed | Windows 11, Adoptium Java 21.0.11, 2026-07-14 | 116 tests passed; clean `compileJava`, full `test`, `build`, and `shadowJar` succeeded |
| `AUTO-02` | Passed | Windows 11, Adoptium Java 21.0.11, 2026-07-14 | `run --args="--mode printMissingTranslations"` succeeded with no missing-key output |
| `PKG-01` | Passed | Windows 11, Adoptium Java 21.0.11, 2026-07-14 | Clean `build shadowJar` succeeded and reran all 116 tests |
| `PKG-02` | Passed | Windows 11, Azul Zulu 21.0.11 JDK FX | JDK FX `jpackage` and standalone startup passed; the Adoptium-only `jlink` limitation is environmental and does not apply to the configured JDK FX path |
| JavaFX startup smoke | Passed | Chinese locale, Java 21, 2026-07-10 | Main window rendered with menu, chunk grid, status bar, and no startup exception |
| `UI-01` / `UI-02` | Passed | User reports, 2026-07-12/14 | Boundary-only scrolling, blue hover/focus/selected states, explicit empty-arrow catalogs, preview, copied-world checks, and the fresh-Builder editor-focus then arrow path for From/To/Biome all passed |
| `WORLD-18` / `WORLD-21` file-level checks | Passed | Commit `35261278`, DataVersions 2860 and 4671, 2026-07-11 | Preview hashes, preview/execution counts, selection-only execution, bounded air, state round-trip, tile effects, duplicate coordinates, light invalidation, and heightmap shape passed on disposable copies |
| Real biome boundary | Passed | Disposable copies of user-provided 1.18 and 1.21 normal terrain, 2026-07-11 | Preview hashes unchanged; execution removed all selected-biome source matches while the control-biome counts stayed unchanged |
| `WORLD-18` / `WORLD-21` game checks | Passed | User game pass and log review, 2026-07-12/13 | Selected chunks and the adjacent one-chunk ring relit correctly after execution saved existing ring chunks with the version-specific relight flag cleared |
| `WORLD-LATEST` | Passed | 26.3 snapshot 3 disposable copies, 2026-07-12/13 | File checks passed and the user completed the copied-world game load/reload; source world remained read-only |
| B-class release hardening | Passed | Windows 11, Adoptium Java 21, 2026-07-17 | 134 tests passed; translation check produced no missing keys; `build shadowJar` succeeded with parser/diagnostic unification, catalogue compatibility, preset rollback, ReplaceBlocks-only region abort, and context-read call-count regressions |
| `UI-CATALOG` | Passed | User report, 2026-07-17 | Five-catalogue checks passed: empty switching was direct; Cancel preserved the old catalogue and all work; Confirm selected the new catalogue and fully reset the Builder; saved presets survived; exact out-of-catalogue custom-preset IDs warned without blocking; regex sources were not misclassified |

### Phase 6 copied-world evidence

All writes below used fresh copies under `%LOCALAPPDATA%\Temp\mca-phase6-worlds-35261278-run1`. The original `mcatest1.18` and `mcatest` worlds were read-only baselines. Each selection contained 81 chunks centered on the saved player position.

| Scenario | Java 1.18 / DataVersion 2860 | Java 1.21 / DataVersion 4671 |
|---|---|---|
| Preview non-mutation | 12 region/poi/entities hashes unchanged | 77 region/poi/entities hashes unchanged |
| Ordinary / multiple rules | 41,492 dirt matches; post-execution source 0 and target 41,492 | 99,063 stone + 41,253 dirt = 140,316; post-execution sources 0 and targets exactly 99,063 / 41,253 |
| Bounded air at Y=80 | 20,736 preview and target blocks; post-execution source 0 | 20,479 preview and target blocks; post-execution source 0 |
| Exact state / selected properties | 20,736 north exact-state and 20,736 south / waterlogged matches; post-execution sources 0 and targets 20,736 each | 20,202 north exact-state and 20,148 south / waterlogged matches; post-execution sources 0 and targets 20,202 / 20,148 |
| Overlap accounting | South-facing and waterlogged rules overlap on 20,736 positions | South-facing and waterlogged rules overlap on 20,148 positions |
| Tile add/remove/update | Fixture had no relevant block entities | Preview reported remove 2, add 11, update 2; post-execution totals were 3 / 16 / 5 with 0 duplicate coordinates |
| Light / heightmap integrity | Touched sections lost their light arrays; all four heightmaps existed with 37 longs and no malformed arrays | Same; bounded Y only invalidated the Y=80 section while ordinary/state rules invalidated their touched sections |
| Real biome boundary follow-up | `snowy_plains` source 50,332; `forest` control 895,747 unchanged | `cold_ocean` source 1,408; `beach` control 59,954 unchanged |

The stateful fixtures in this pass were deliberately generated in copied `.mca` files and then round-tripped through exact-state and `props(...)` rules. The later normal-terrain boundary pass used copies under `%LOCALAPPDATA%\Temp\mca-phase6-biome-final`; the original worlds were read-only. The user completed the final 1.21 adjacent-ring rendering rerun on a disposable copy.

The 2026-07-12 latest-snapshot pass used two copies under `%LOCALAPPDATA%\Temp\mca-phase6-26_3`. It exercised `biome(minecraft:river, y(90..105, literal(minecraft:stone)))`, compared Change with Force, and checked an adjacent unselected chunk. The 26.3 game log had no chunk, palette, lighting, or heightmap errors; its only ERROR was an unrelated missing Rockstar Vulkan-layer JSON file.

## Catalog data tests

The Phase 4A block-state catalog is data-only and does not mutate worlds.

Automated checks:

```powershell
.\gradlew.bat test --tests net.querz.mcaselector.version.mapping.blockstate.BlockStateCatalogTest
```

Expected coverage:

- Java 1.18.2, 1.20.6, 1.21.9, 1.21.11, and 26.2 catalogues load in DataVersion order; the newest is the Builder default.
- `minecraft:acacia_trapdoor` exists.
- `facing`, `half`, `open`, `powered`, and `waterlogged` are exposed with allowed values.
- default trapdoor properties match the Mojang report default state.
- `minecraft:yellow_wool` and `minecraft:blue_wool` exist as separate block IDs and expose no color property.

If `processResources` fails because a running JavaFX app locks an unrelated file under `build/resources/main`, close the running MCA Selector window and rerun the test.

## Property-aware builder UI

The Phase 4B builder UI is catalog-backed and should be tested without mutating world data first.

Manual checks:

- Open NBT Changer, choose the ReplaceBlocks field, and open the builder.
- Search/select `minecraft:stone` as the source and `minecraft:dirt` as the target; adding the rule should generate `literal(minecraft:stone)=minecraft:dirt`.
- Opening the builder with an empty ReplaceBlocks value should leave From/To inputs blank and should not immediately show an empty-rule validation error.
- Search/select `minecraft:acacia_trapdoor`; the builder should show `facing`, `half`, `open`, `powered`, and `waterlogged` dropdowns.
- Property dropdowns should default to `all`/`全部`. Leaving all source properties at `all` should generate a simple `literal(...)` source; selecting only `facing=north` should generate `props(...)` with only `facing`.
- Change several trapdoor properties and add the rule; the generated target text should use block-state SNBT with `Name` and the selected `Properties`, then parse successfully.
- Search/select `minecraft:blue_ice`; no property rows should appear, and the generated rule should still parse.
- Enter an unknown/modded ID such as `example:custom_block`; no property rows should appear, and manual entry should remain possible.
- Confirm the Builder displays the active Java/DataVersion catalogue and offers Java 1.18.2, 1.20.6, 1.21.9, 1.21.11, and 26.2, with the newest selected by default. Selection is manual; no world version or ID migration is inferred.
- With an empty Builder, switch catalogue and confirm it changes directly. With a non-empty Builder, Cancel must keep the old catalogue plus every draft/rule/selection/status/popup; Confirm must select the new catalogue and reset fields, properties, Extra NBT, Y/biome filters, rules, selections, result, validation, preset selection, and popups.
- Confirm a catalogue switch never deletes built-in or saved presets. Apply a versionless custom preset containing an exact ID outside the active catalogue: the rule must remain appended with a non-blocking advisory. A regex source must not be guessed as an exact catalogue ID.
- Enter `minecraft:future_block` as source and target. Both remain addable with warnings; the target warning must explicitly ask the user to verify world support.
- Paste or type block-state SNBT with `Name` directly in a from/to input; the builder should still accept it through existing validation.
- Copy the generated value into the advanced text workflow and confirm it remains accepted there.

## Source matching modes

Gate A and Phase 4C/4D are implemented. Re-run these checks if source syntax or matching semantics change.

Documentation checks:

- `docs/NEXT_DEVELOPMENT_REPLACE_BLOCKS.md` names every source mode that will exist.
- Legacy bare source strings remain documented as Java regex matching.
- Existing source SNBT remains documented as exact `CompoundTag` matching.
- Explicit regex source mode uses `regex(...)`.
- Literal block-ID matching uses `literal(...)`.
- Selected-property matching uses `props(...)`.
- Y range matching uses `y(min..max, source)`.
- Parser examples remain documented for future compatibility checks.

Automated checks:

- Legacy `minecraft:stone=minecraft:dirt` remains accepted.
- Explicit `regex(minecraft:.*_log)=minecraft:stone` is accepted and matches the same way as the equivalent legacy regex.
- Explicit `literal(minecraft:stone)=minecraft:dirt` is accepted.
- `literal(stone)=minecraft:dirt` normalizes to `minecraft:stone`.
- Existing exact source-state SNBT remains exact.
- Literal source mode treats regex metacharacters literally.
- Selected-properties mode matches `Name` exactly and only the selected property keys.
- Empty `props(...)` property selections are rejected with a targeted diagnostic.
- Invalid new-mode syntax produces a targeted diagnostic.
- Valid Y range wrappers are accepted and invalid Y ranges are rejected with a targeted diagnostic.
- Repeating the same normalized source selector keeps only the last target, matching the legacy `Map<String, ...>` behavior. Source type, tile mode, Y range, biome set, and source state all participate in source identity.
- Different selectors that can match the same block, such as a literal and a regex, remain separate ordered rules and continue to appear as overlap in Preview.
- Builder and custom-preset loading treat the same source with a different target as a duplicate instead of adding a second row.

Current focused commands:

```powershell
.\gradlew.bat test --tests net.querz.mcaselector.changer.fields.ReplaceBlocksFieldTest --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksDiagnosticsTest
```

Manual checks:

- In advanced text, confirm `literal(minecraft:stone)=minecraft:dirt` is accepted.
- In advanced text, confirm `regex(minecraft:.*_log)=minecraft:stone` is accepted and clearly intentional.
- In advanced text, confirm `props({Name:"minecraft:oak_stairs",Properties:{facing:"north"}})=minecraft:stone` is accepted.
- Confirm invalid `regex(*)=minecraft:stone` shows a targeted validation error.
- Confirm invalid `props({Name:"minecraft:stone"})=minecraft:dirt` shows a targeted validation error.
- Confirm invalid `y(10..0, literal(minecraft:stone))=minecraft:dirt` shows a targeted validation error.
- In the builder, a simple source block rule should generate `literal(...)`.
- In the builder, selecting source block properties should generate `props(...)`.

## UI interaction regression checks

Execution matrix:

| ID | Scenario | Pass condition |
|---|---|---|
| `UI-01A` / `UI-02A` | Empty Builder and validation timing | Blank inputs, no eager error, correct helper/status colors |
| `UI-01B` / `UI-02B` | Block and biome completion | Tab and click both work; first popup, hover, caret, and token completion are stable |
| `UI-01C` / `UI-02C` | Stateful two-column layout | Equal widths, aligned property rows, source-only controls below properties |
| `UI-01D` / `UI-02D` | Rule add, select, edit, delete | `literal`, `props`, tile, Y, biome, and target state restore without losing editable controls |
| `UI-01E` / `UI-02E` | Built-in and custom presets | Fill, save draft, overwrite, load, and delete preserve parser-equivalent rules |
| `UI-01F` / `UI-02F` | Help and Preview | Correct enablement, non-mutating preview, per-rule/tile/overlap output |
| `UI-01G` / `UI-02G` | Resize and console | No overlap/clipping at default and resized layouts; no JavaFX exception |
| `UI-CATALOG` | Five-catalogue switch/reset | Empty switches directly; Cancel preserves all work; Confirm selects the new catalogue and fully resets; saved presets remain and exact out-of-catalogue preset IDs warn without blocking |
| `UI-01H` / `UI-02H` | Builder layout polish | Compact toolbar, full-width source restrictions, localized empty-state placeholders, content-driven table/result sizing, shared Add Rule button treatment, readable disabled preset actions, aligned dialog insets, bounded autocomplete popups, and wrapped monospace output |

Manual checks:

- In the NBT Changer field row, type `minecraft:stone=minecraft:dirt` directly into ReplaceBlocks. The field should stay visually neutral while typing and only show valid/invalid feedback after a short pause.
- With the default NBT Changer dialog size, the ReplaceBlocks `Builder` button should be visible without dragging the horizontal scrollbar.
- Open an empty builder and confirm the From/To fields are blank, the generated value is empty, and no `Add at least one rule` error is shown before user action.
- Before typing in an empty Builder From/To field, click the dropdown arrow. The complete A-Z block catalog should open; it must not open automatically when the Builder first appears.
- Open a fresh Builder, click once inside an empty From, To, or Biome editor, then click that field's arrow as the next action. On this first show the full catalog must stay attached directly above or below its field instead of jumping away from it; repeat all three fields after reopening the Builder.
- In a fresh empty Builder, confirm the preset and catalogue controls form a compact top toolbar; the closed catalogue control shows a short Java version while hovering it exposes the full DataVersion.
- Confirm Extra NBT, Min Y, Max Y, and Biome appear together in one full-width optional source-restrictions strip below From/To, without changing the From/To widths or the generated syntax.
- Confirm the Y-range labels remain fully visible as `最低 Y`/`最高 Y` (or the localized equivalent) before and after entering values; neither label should render as `...`.
- Open the Biome and From/To autocomplete lists near the Builder's right side. Their right edge must stay inside the Builder window; long entries may use the available width, and the popup must remain attached below or above the field. Ordinary property/preset dropdowns should retain their existing native width behavior.
- With no rules, confirm the rules area is compact and shows its localized empty-state prompt. Add one or more rules and confirm the table grows to use available space; delete them all and confirm it returns to the compact empty state.
- With no generated value, confirm the result area is compact. Add a rule with a long source/target expression and confirm the result wraps cleanly, remains readable in monospace, and grows only as needed.
- Confirm Add Rule uses the same button treatment as the surrounding controls. Disabled Fill/Save/Delete preset buttons must remain readable. Confirm the left edge of Help/Preview aligns with the upper content and the right edge of Cancel aligns with the result area. Resize the Builder and verify these states do not cause overlap or layout jumps.
- In the builder From/To fields, type `oak` or `sto`. The candidate list should open automatically, show every matching block ID in A-Z order, highlight the typed substring in blue, allow mouse scrolling, and collapse after Tab completion.
- Narrow the suggestion list from many matches to two or three and drag its scrollbar repeatedly. The popup must shrink to the actual result count, show no empty rows, and log no `VirtualFlow index exceeds maxCellCount` warning.
- With a Chinese IME active, select part of a completed block ID, enter two Latin letters, and confirm neither letter is swallowed and no `TextInputControl.replaceText` exception is logged.
- Repeat the same suggestion test with mouse-click completion. The chosen block ID should fill the editor, the matching property rows should appear when applicable, and the JavaFX console should not log `ListViewBehavior` or index errors.
- Moving the mouse over preset, block, Extra NBT, biome, and property choices should show a clearly visible light-blue hover. Keyboard focus/current selection should use a solid blue background with readable white text.
- The builder helper text below the generated value should be visible before manual From/To input, then hide once the user types non-empty text into either From/To field.
- For stateful From/To blocks with matching properties such as stairs, property dropdown rows should appear directly below both block inputs, align vertically across the two columns, and use equal left/right column widths. Source-only Extra NBT, Y range, and Biome controls should appear below the source property rows.
- In the builder From block area, leaving Min Y and Max Y empty should generate the same rule as before.
- Filling only Min Y should generate `y(<min>.., source)`, filling only Max Y should generate `y(..<max>, source)`, and filling both should generate `y(<min>..<max>, source)`.
- Invalid Y input such as letters or Min Y greater than Max Y should prevent adding the rule and show validation feedback.
- Leaving the Builder Biome field empty should generate the same rule as before. Filling `plains` should generate `biome(minecraft:plains, source)`, filling `plains;minecraft:forest` should generate a semicolon-separated multi-biome wrapper, an unknown `minecraft:` biome ID should prevent adding the rule and show validation feedback, and a syntactically valid non-`minecraft:` biome ID should remain manually typeable.
- In the Builder Biome field, typing `pla` or `for` should open a filtered biome list, highlight the typed substring, and support Tab completion and mouse-click completion without console errors.
- In the Builder Biome field, typing after a semicolon such as `minecraft:plains;for` should complete only the current biome token, preserving the earlier token.
- Clicking the empty Builder Biome dropdown arrow should show the complete A-Z biome list; leaving the field empty without clicking must not open it automatically.
- Choose each built-in Builder preset and click `Fill preset` / `填入预设`. The From/To inputs should be filled visibly, remain editable, and require the normal `Add rule` action before the generated ReplaceBlocks value changes.
- The Air preset should fill `minecraft:air` -> `minecraft:stone` and show a warning about sparse/missing sections.
- The Fluids, Logs/leaves, and Ores presets should fill visible source-mode regex text and ordinary target blocks, then generate valid ReplaceBlocks text after `Add rule`.
- The Containers preset should set Extra NBT to present, fill a visible container source regex, target `minecraft:air`, show a data-loss warning, and generate a `tile(regex(...))=minecraft:air` rule after `Add rule`.
- Adding a valid rule should clear both input columns and source-only conditions while keeping the generated rule. With no draft and no selected row, saving stores all rules in the table.
- Selecting one rule and clicking `Save preset` / `存为预设` should save only that rule. With no selected row, a valid current draft takes precedence over the rule table.
- In the rules table, Ctrl/Shift-click should keep JavaFX's native multiple-row selection. Drag from empty table space over two of three rules: the translucent rectangle should select exactly the two intersected rows. Ctrl-drag from empty space should add intersected rows to the existing selection. While the rectangle grows or shrinks, the table, column headers, surrounding controls, and dialog geometry must remain completely stationary.
- With two rules selected, `Save preset` / `存为预设` should store exactly those two rules in table order, leaving the unselected rule out. `Edit rule` / `载入编辑` should be disabled for that multi-selection; `Delete rule` / `删除规则` and the table's Delete key should remove every selected row.
- When the rules table has a selection, Esc should clear only the rule selection. In From/To, biome, and property controls, Ctrl+Enter should follow the same valid/invalid outcome as `Add rule` / `添加规则`; Delete must continue to edit text normally while an input owns focus.
- In every open From/To, biome, property, Extra NBT, and preset list, verify Up/Down, PageUp/PageDown, Enter, and Esc. On the first Builder opening, the first arrow key must produce an immediate visible navigation response rather than being absorbed by JavaFX's initial popup focus. For From/To and biome autocomplete, moving within the currently fully visible rows must leave the list stationary; crossing a boundary should reveal only the next row, while PageUp/PageDown should move by one actual visible page. Type and then clear a query, close an explicit empty catalog with Esc, and confirm the next closed Up/Down key does not write a block or biome; reopening without another explicit empty-arrow click must not retain the full catalog. Repeat the middle-of-text caret and selection test with the popup closed to confirm that ordinary editing remains native. Check the JavaFX console for exceptions after both keyboard and marquee interaction.
- Applying a custom preset should append all non-duplicate preset rules to the current rule table without clearing the existing rules or draft. The save success message should appear in the Builder validation/status area, not as a separate modal dialog.
- With no rules added yet, entering a valid From/To draft, including selected source/target properties, should enable `Save preset` / `存为预设`. Saving should validate that draft, store it as one ReplaceBlocks rule, and not require pressing `Add rule` first.
- With no rules added yet, incomplete or invalid From/To draft input should keep `Save preset` / `存为预设` disabled or produce the existing Builder validation message rather than saving.
- Saving again with an existing custom preset name should ask for overwrite confirmation. Built-in preset names should not be overwritable.
- Selecting a custom preset should enable `Delete preset` / `删除预设`; deletion should ask for confirmation and remove the preset from the dropdown.
- The Builder button bar should show `Help` and `Preview` together on the left. Preview should stay disabled until the Builder has a valid generated ReplaceBlocks value, then run the same non-mutating dry-run counts described below.

## Per-rule preview counts

Implemented as Phase 5A. Preserve these checks before future presets or release-hardening checks.

Checks:

- Preview still reports aggregate scanned chunks, affected chunks, affected sections, matched blocks, tile estimates, and warnings.
- Preview also reports one row per rule.
- Per-rule rows show source mode, generated source text, target text, and matched block count.
- Aggregate matched blocks count each block position once when any rule matches.
- Per-rule counts count each rule match separately, so the per-rule sum may be higher than the aggregate when source rules overlap.
- Overlapping source rules are visible through an overlap warning.
- Preview remains non-mutating and does not change region modified times.
- Builder Preview cancellation uses a task-local token, checks it at chunk boundaries, waits for the preview worker to stop, and must not clear unrelated `JobHandler` queues or show a partial result.
- Unsupported older preview chunks are still reported instead of silently counted.
- Light-array and heightmap warnings use only actually affected sections/chunks. In selection-only mode, preview reports up to how many adjacent chunks outside the selection may receive relight-flag updates and states that block replacement remains inside the selection.

Automated coverage:

- `ReplaceBlocksPreviewCountsTest` builds in-memory modern sections and verifies aggregate matched blocks, per-rule counts, overlap count, Y range preview counts, synthetic air-section Y filtering, actual-match-only light invalidation counts, and Y range execution filtering without touching world files.
- `ReplaceBlocksPreviewerCancellationTest` verifies cancellation stops the scan before the next chunk; `CancellableProgressDialogCancellationTest` verifies the task-local scope does not select global cancellation and the token is cross-thread visible.
- `ReplaceBlocksFieldTest` and the pre-1.18 palette safety tests verify that valid but unmatched or unsupported fail-closed rules leave the entire chunk NBT unchanged, including light tags, heightmaps, palette storage, and tile/block-entity lists.
- `FieldChangerTest`, `RegionFieldChangeTest`, and `FieldChangerIntegrationTest` verify the primary-save barrier, failed/cancelled publication rules, exact eight-neighbor target generation, region-only loading/saving, cross-region `(31,31)` relighting, zero-match byte/mtime stability, and untouched POI/entities sentinels.

## Test world preparation

- Create one small creative-mode Java 1.18.x world and one Java 1.21.x world.
- Keep each untouched baseline outside this repository and never open it with MCA Selector.
- Before every test case, copy the entire world directory to a fresh case directory. Keep `region`, `entities`, `poi`, `level.dat`, and datapack files together.
- Record the Minecraft version, world DataVersion, candidate commit, selected chunks, rule text, and copy path in the execution table.
- Before preview, record modified times, sizes, and SHA-256 hashes for the selected region/entities/poi files. Record them again after preview; every value must be unchanged.
- Execute the replacement only on a second fresh copy, never on the preview copy.
- After execution, load the copy in Minecraft, visit the changed chunks, save, exit, reopen, and inspect the latest game log.

Required fixture layout near spawn:

| Fixture | Contents |
|---|---|
| A | Counted stone, dirt, deepslate, and glass groups for ordinary and multiple rules |
| B | Logs, stairs, slabs, trapdoors, and waterlogged variants with visible orientation markers |
| C | Chest and barrel with named items, furnace state, and sign text for block entity checks |
| D | Marked Y layers plus a tiny sparse/empty section area for bounded air replacement |
| E | Marker blocks spanning a stored biome boundary, with positions recorded on both sides |
| F | A small exposed surface containing solid, leaves, water, and air for light/heightmap checks |

Use exact block counts that are easy to verify (for example 16 or 64 blocks per group) and record the expected count before running preview.

Copied-world execution matrix (run each ID on separate fresh copies for both required versions):

| ID | Rule family | Expected evidence |
|---|---|---|
| `WORLD-*-01` | Ordinary, multiple rules, and selection-only | Exact counted groups change; outside blocks/chunks remain unchanged |
| `WORLD-*-02` | Literal, regex, exact state, props, and waterlogged state | Only intended IDs/states change; generated text round-trips |
| `WORLD-*-03` | `tile` / `no_tile` and tile targets | Preview add/remove/update counts match; no duplicate coordinates; item/sign data outcome is intentional |
| `WORLD-*-04` | Bounded Y and sparse air | Only requested layers change; no section is created outside the intersecting Y range |
| `WORLD-*-05` | Biome boundary and combined biome + Y + tile | Stored biome-cell boundary is respected and preview equals execution |
| `WORLD-*-06` | Overlapping rules | Aggregate and per-rule counts follow documented first-match/order behavior |
| `WORLD-*-07` | Light and heightmap-sensitive surface | Light arrays are invalidated, heightmaps remain valid, Minecraft relights and reloads cleanly |
| `WORLD-*-08` | UI/CLI parity | Equivalent rules on two fresh copies produce equivalent selected chunk data |

## Test block types

Normal blocks:

- stone
- dirt
- deepslate
- glass

Block states:

- oak_log with different `axis`
- stairs with `facing` and `half`
- waterlogged block if available
- slab with `type`

Tile/block entities:

- chest with an item
- barrel with an item
- furnace with burn/cook fields if needed
- sign with text

Air:

- naturally empty vertical space
- missing/sparse sections if possible
- caves or void-like areas in copied chunks

## Ordinary block replacement

Example:

```text
stone=dirt
```

Checks:

- stone becomes dirt in selected/copied chunks
- unrelated blocks remain unchanged
- palette no longer contains unused stone when no stone remains in the section
- world loads without errors

## Multiple simple rules

Example:

```text
stone=dirt, deepslate=stone
```

Checks:

- both rules apply
- generated advanced string is accepted
- order does not produce unexpected repeated replacement within the same pass

## Block state replacement

Example target shape:

```text
stone={Name:"minecraft:oak_log",Properties:{axis:"y"}}
```

Checks:

- resulting block has the expected state in game
- `block_states.palette` includes the full state compound
- no invalid state crashes the world

## Source block state matching

Example exact source-state rule:

```text
{Name:"minecraft:oak_stairs",Properties:{facing:"north",half:"bottom",shape:"straight",waterlogged:"false"}}={Name:"minecraft:oak_stairs",Properties:{facing:"south",half:"bottom",shape:"straight",waterlogged:"false"}}
```

Checks:

- only stairs with the exact stored source state are changed
- stairs with another `facing`, `half`, `shape`, or `waterlogged` value remain unchanged
- a partial source SNBT such as `{Name:"minecraft:oak_stairs",Properties:{facing:"north"}}` does not match full palette states
- the same rule can be entered through the raw ReplaceBlocks field
- the builder can add the rule when from/to inputs contain block state SNBT
- the generated advanced query round-trips through `ChangeParser`

Colored block example:

```text
minecraft:yellow_wool=minecraft:blue_wool
```

Modern Minecraft wool colors are separate block IDs, so this should work as ordinary block-name replacement rather than property matching.

## Tile entity replacement

Phase 4E automated coverage:

```powershell
.\gradlew.bat test --tests net.querz.mcaselector.changer.fields.ReplaceBlocksFieldTest --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksDiagnosticsTest --tests net.querz.mcaselector.version.java_1_18.ReplaceBlocksPreviewCountsTest
```

Syntax covered:

- `tile(literal(minecraft:chest))=minecraft:stone` matches only source positions with existing block entities.
- `no_tile(literal(minecraft:chest))=minecraft:stone` excludes source positions with existing block entities.
- Preview counts tile entity additions, removals, and updates separately.
- Modern 1.18+ target tile replacement removes existing block entities at the same coordinates before adding replacement tile SNBT.

Example target shape:

```text
stone=minecraft:barrel;{id:"minecraft:barrel"}
```

Checks:

- target block appears
- `block_entities` contains an entry with correct x/y/z
- Minecraft loads the tile entity without removing the chunk
- replacing an existing chest with barrel does not leave duplicate block entities at the same x/y/z
- replacing a tile entity block with a non-tile block removes the old block entity

Copied-world validation:

- Verify tile-to-tile replacement on copied worlds.
- Verify tile-to-non-tile replacement on copied worlds.
- Verify non-tile-to-tile replacement on copied worlds.
- Compare preview add/remove/update counts with a real run on a fresh copied world.
- User reported Phase 4E in-game copied-world validation complete on 2026-06-28.
- Keep rich target tile NBT editing out of scope until it has its own copied-world checklist.

## Air replacement

Example:

```text
air=glass
```

Run only on a tiny copied selection.

Checks:

- only expected selected chunks are affected
- missing sections inside detected section range are created as expected
- replacement does not unexpectedly fill outside the intended vertical range
- file size growth is understood and acceptable
- world loads successfully

## Y range replacement

Phase 4F-1 automated coverage:

```powershell
.\gradlew.bat test --tests net.querz.mcaselector.changer.fields.ReplaceBlocksFieldTest --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksDiagnosticsTest --tests net.querz.mcaselector.version.java_1_18.ReplaceBlocksPreviewCountsTest
```

Syntax covered:

- `y(-64..64, literal(minecraft:stone))=minecraft:dirt` limits the source match to inclusive world Y values.
- `y(64.., literal(minecraft:stone))=minecraft:dirt` has only a minimum Y.
- `y(..0, literal(minecraft:stone))=minecraft:dirt` has only a maximum Y.
- `y(0..15, tile(literal(minecraft:chest)))=minecraft:stone` combines Y filtering with source tile eligibility.
- Invalid ranges such as `y(.., ...)`, `y(10..0, ...)`, and `y(foo..10, ...)` are rejected.

Phase 4F-1 manual copied-world checks:

- Repeat air replacement on a tiny copied selection with a narrow Y range.
- Verify missing sections are not created outside the requested Y range.
- Compare preview counts with execution on a fresh copied world.
- Verify a normal block replacement across two visible heights, for example replacing stone with glass only at one marked Y layer.
- Verify a tile-filtered Y rule on containers if a copied test world has containers at different heights.

## Biome replacement

Phase 4F-2 automated coverage:

```powershell
.\gradlew.bat test --tests net.querz.mcaselector.changer.fields.ReplaceBlocksFieldTest --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksDiagnosticsTest --tests net.querz.mcaselector.version.java_1_18.ReplaceBlocksPreviewCountsTest
```

Syntax covered:

- `biome(minecraft:plains, literal(minecraft:stone))=minecraft:dirt` limits the source match to positions whose stored biome is plains.
- `biome(plains;minecraft:forest, literal(minecraft:stone))=minecraft:dirt` accepts short vanilla IDs and multiple biome IDs separated by semicolons; serialized values use full IDs.
- `biome(minecraft:plains, y(0..15, tile(literal(minecraft:chest))))=minecraft:stone` combines biome filtering with Y range and tile source eligibility.
- Invalid biome wrappers such as `biome(, ...)`, unknown biome IDs, missing commas, or missing wrapped sources are rejected.

Granularity:

- Modern 1.18+ preview and execution are block-position aware: each candidate block position is checked against the biome value stored for that position.
- In modern chunk data, one biome value covers a 4x4x4 block cell. Automated tests verify a synthetic boundary where only one 4x4x4 cell matches.
- Automated fixtures cover 3-bit and 5-bit non-crossing packed biome palettes; the bit width comes from palette size, not the number of longs.
- Multi-entry biome palettes with missing, incorrectly sized, or out-of-range packed data must fail closed for biome-restricted rules. Singleton palettes remain valid without packed data.
- Missing or incomplete sections with no trustworthy biome must not satisfy `biome(plains, air)` merely because section completion serializes a plains default. If an unrestricted air rule completes the section, later biome-restricted rules in the same operation must still see the original biome as unknown.
- An incomplete section with an existing trustworthy singleton biome palette remains eligible for a matching biome-restricted air rule.

Phase 4F-2 manual copied-world checks:

- Pick a tiny copied selection crossing a visible biome boundary and replace a harmless marker block only in one biome.
- Confirm preview counts match execution on a fresh copy of the same selection.
- Verify blocks immediately across the biome boundary are not changed when their stored biome does not match.
- Repeat one combined rule with Y range plus biome condition if the boundary area has blocks at different heights.

## Preview / dry-run

Checks:

- Preview runs on a selected copied area without writing region files
- Preview does not change region file modified times
- Preview reports scanned chunks, affected chunks, affected sections, and matched blocks
- Preview counts source-state rules consistently with a later real run on another copy
- Preview shows warnings for air replacement, tile entity targets, light data, and heightmap recomputation
- Older unsupported preview chunks are reported as unsupported instead of silently counted

## Light validation

Checks:

- inspect whether changed sections had `BlockLight` / `SkyLight` removed
- inspect whether the changed chunk has `isLightOn=0` (or the equivalent legacy `LightPopulated=0`) with the correct numeric tag type
- run a valid rule whose source is absent and verify the complete chunk NBT remains byte-for-byte semantically unchanged, including light tags and heightmaps
- verify only successfully saved, actually changed chunks seed adjacent relighting; changed centers are excluded from the second-stage ring, missing neighbor chunks/files remain missing, and neighbor POI/entities files are untouched
- load world in Minecraft and wait for relighting
- check client/server logs for lighting or chunk errors
- revisit the area after save/reload

## Heightmap validation

Checks:

- after replacement, verify surface behavior in game:
  - mob spawning / motion blocking where practical
  - top surface rendering and precipitation behavior
  - no obvious invisible collision or stale surface artifacts
- inspect chunk NBT before/after for `Heightmaps`
- specifically verify 1.18+ and 1.21+ flat chunk format writeback

## Version coverage

Minimum:

- 1.18+ world using post-21w43a flat chunk structure
- 1.21+ world

Legacy safety coverage:

- Automated classic 1.9 execution confirms literal IDs still work and contextual or unsupported source modes fail closed.
- Automated context-overload coverage protects 1.13-1.17 palette paths from silently dropping tile, Y, or biome conditions.
- A pre-1.18 copied world remains optional because new conditional execution and preview are documented as modern-only; if one is used, it is a compatibility smoke test, not a substitute for `WORLD-18` or `WORLD-21`.

## World integrity checks

Before run:

- copy world directory
- record modified time and size of region files
- note selected chunks

After run:

- open copied world in Minecraft
- visit changed chunks
- check game logs for chunk, block entity, heightmap, or light errors
- save and reload the world
- verify no unexpected chunks were changed
- compare NBT for a small sample before/after

## CLI/UI parity

For every stable example:

- run once through the existing UI on a copied world
- run once through CLI `change` mode on another copy when command flags are confirmed
- compare resulting chunks for equivalent behavior

Do not run CLI mutation commands until the exact world and selection options have been rechecked in `ParamExecutor`.
