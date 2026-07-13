# ReplaceBlocks Development Notes

Date: 2026-06-04
Last updated: 2026-07-07

Scope: ReplaceBlocks reconnaissance plus implemented phases 1-5A, Phase 4E, Phase 4F-1, and Phase 4F-2. Java source now includes a rule builder, validation diagnostics, modern preview/dry-run with per-rule counts, exact source block-state matching, a Java 1.21.9 block-state catalog foundation, a property-aware catalog-backed builder UI, explicit source modes, tile/block entity source safety controls, source-side Y range restrictions, and source-side biome restrictions. Gradle build logic and Minecraft world data were not modified by these development notes.

## Project stack

- Language/runtime: Java 21.
- Build: Gradle, `application`, `java`, `org.openjfx.javafxplugin`, `shadowJar`, `org.beryx.runtime`.
- UI: JavaFX controls and Swing interop.
- Main dependencies: Querz NBT, Gson, fastutil, Log4j, SLF4J simple, commons-cli, progressbar, Groovy JSR223, RichTextFX, LZ4, ClassIndex, LevelDB.
- Tests: JUnit Jupiter.
- Main class: `net.querz.mcaselector.Main`.

## Build and run

From the repository root on Windows:

```powershell
.\gradlew.bat run
.\gradlew.bat test
.\gradlew.bat jar
.\gradlew.bat shadowJar
```

The Gradle application main class is `net.querz.mcaselector.Main`.

CLI mode includes `change`, with `--fields` and `--force` options. The exact world/selection flags should be checked in `ParamExecutor` before running, and any change command must be run only against a disposable copy of a world.

## Main startup path

- `Main.main(...)`
- initializes logging
- calls `VersionHandler.init()`
- runs `ParamExecutor`
- checks JavaFX availability
- loads config and translations
- launches `Window`

## ReplaceBlocks call chain

UI path:

- `DialogHelper.changeFields(...)`
- creates `ChangeNBTDialog`
- user edits field rows or the advanced query text
- dialog returns `ChangeNBTDialog.Result`
- confirmation dialog is shown
- `FieldChanger.changeNBTFields(...)`
- loads regions and queues `MCAFieldChangeProcessJob`
- each chunk receives `ChunkData.applyFieldChanges(fields, force)`
- for ReplaceBlocks, `ReplaceBlocksField.change(...)`
- `VersionHandler.getImpl(data, ChunkFilter.Blocks.class).replaceBlocks(...)`
- heightmaps are recomputed through `ChunkFilter.Heightmap`

CLI path:

- `ParamExecutor` mode `change`
- `parseFields(...)`
- `new ChangeParser(line.getOptionValue("fields")).parse()`
- `FieldChanger.changeNBTFields(fields, force, selection, progress, true)`
- same chunk-level field application path as UI

`ReplaceBlocksField.force(...)` currently delegates to `change(...)`, so `--force` has no special ReplaceBlocks behavior.

## Key files

- `src/main/java/net/querz/mcaselector/Main.java`: application and CLI entry.
- `src/main/java/net/querz/mcaselector/changer/FieldType.java`: registers `REPLACE_BLOCKS`.
- `src/main/java/net/querz/mcaselector/changer/Field.java`: field base class.
- `src/main/java/net/querz/mcaselector/changer/ChangeParser.java`: advanced `Field = value` parser.
- `src/main/java/net/querz/mcaselector/changer/fields/ReplaceBlocksField.java`: ReplaceBlocks input parsing and version-layer dispatch.
- `src/main/java/net/querz/mcaselector/version/ChunkFilter.java`: `Blocks`, `Heightmap`, `BlockReplaceSource`, `BlockReplaceData`, and preview contracts.
- `src/main/java/net/querz/mcaselector/version/VersionHandler.java`: DataVersion to implementation dispatch.
- `src/main/java/net/querz/mcaselector/io/job/FieldChanger.java`: region job orchestration and saving.
- `src/main/java/net/querz/mcaselector/io/job/ReplaceBlocksPreviewer.java`: ReplaceBlocks dry-run scanner.
- `src/main/java/net/querz/mcaselector/io/mca/ChunkData.java`: applies fields to chunk data.
- `src/main/java/net/querz/mcaselector/ui/dialog/ChangeNBTDialog.java`: current NBT Changer UI and best integration point.
- `src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderDialog.java`: rule builder dialog.
- `src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksDiagnostics.java`: non-mutating UI diagnostics.
- `src/main/java/net/querz/mcaselector/version/mapping/blockstate/BlockStateCatalog.java`: 1.21.9 block-state catalog loader used by the property-aware builder UI.
- `src/main/resources/mapping/block_states/java_1_21_9.json`: generated 1.21.9 block-state catalog containing block IDs, property names, allowed values, defaults, and state counts.
- `src/main/java/net/querz/mcaselector/version/java_1_18/ChunkFilter_21w43a.java`: modern 1.18+ `replaceBlocks` implementation for DataVersion 2844+.
- `src/main/java/net/querz/mcaselector/version/java_1_18/ChunkFilter_21w37a.java`: 1.18 snapshot implementation and helper behavior.
- `src/main/java/net/querz/mcaselector/version/java_1_13/ChunkFilter_17w47a.java`: inherited block state palette helpers.
- `src/main/java/net/querz/mcaselector/version/java_1_16/ChunkFilter_20w17a.java`: inherited compacted `block_states.data` bit packing behavior.

## Current capabilities

- Replace one or more source block names with target block names.
- Match source block state SNBT exactly against palette block-state compounds.
- Match explicit literal source IDs with `literal(...)`.
- Match explicit regex sources with `regex(...)`.
- Match selected source properties with `props(...)`.
- Match only source positions with existing block entities using `tile(...)`, or exclude those positions using `no_tile(...)`.
- Limit source matches by world block Y using `y(min..max, source)`. Either boundary may be omitted, but at least one integer boundary is required.
- Limit source matches by biome using `biome(<biome>[;<biome>...], source)`. Parser input may omit `minecraft:` for vanilla biomes, but serialized values use full IDs. Modern preview and execution evaluate the biome at each candidate block position; in 1.18+ chunk data, one biome value covers a 4x4x4 block cell.
- Target can be a simple registry name, an SNBT block state, or a block plus tile entity SNBT.
- Multiple rules are supported in one ReplaceBlocks value.
- Legacy bare or quoted source block-name matching still uses Java regex matching via `String.matches(...)`.
- Builder inputs accept simple block IDs, source wrappers, or block state SNBT with `Name`.
- Builder inputs can search/select Java 1.21.9 catalog block IDs, generate `literal(...)` for simple source IDs, generate `props(...)` for selected source property dropdowns, and omit properties whose dropdown is left at `all`.
- Builder source tile filters are labeled as `Extra NBT: any/present/absent`; the Builder Help dialog explains the choices and is the intended home for future Builder-specific help text.
- Builder source min/max Y fields default to empty; filling either field wraps the generated source with `y(...)`.
- Builder source biome field defaults to empty; filling it with one or more biome IDs separated by semicolons wraps the generated source with `biome(...)`. It uses the known vanilla biome registry for filtered suggestions, Tab completion, and mouse-click completion while still allowing manually typed custom IDs. The Builder Help dialog states the block-position-aware 4x4x4 biome-cell granularity.
- Builder built-in presets are editable starting points. They fill visible From/To and source condition controls for Air to stone, Fluids to air, Logs/leaves to air, Ores to stone, and Containers with Extra NBT to air, then rely on the normal Add rule path and existing generated ReplaceBlocks validation.
- Builder custom presets store parser-compatible ReplaceBlocks text in global config. Save precedence is the selected rule, otherwise the current valid draft, otherwise all table rules. Loading reparses and appends every non-duplicate preset rule without clearing current table rules or draft inputs.
- The NBT Changer dialog shows ReplaceBlocks validation messages and warnings after a short typing pause, so incomplete in-progress input does not flash errors on every character.
- The default NBT Changer dialog width keeps the ReplaceBlocks `Builder` button visible without horizontal scrolling.
- When opened without an existing value, the builder starts with blank From/To inputs and does not immediately show an empty-rule validation error.
- The builder helper text below the generated value is only a pre-input hint; it hides after the user manually types non-empty From/To text.
- The Builder offers ReplaceBlocks preview/dry-run counts for modern 1.18+ formats, including per-rule rows and overlap warnings. The Preview button sits in the Builder button bar beside Help and uses the generated Builder value.
- `BlockStateCatalog.latestJava()` loads the generated Java 1.21.9 block-state catalog used by the builder dropdown UI.
- For modern versions, replacement iterates all 4096 blocks per section.
- Palette entries are added as needed and unused palette entries are cleaned up.
- Existing tile/block entities are removed when replacing a block with a non-tile target.
- When the target includes tile entity SNBT, modern 1.18+ paths remove existing block entities at the replacement coordinates before adding the new tile.
- Preview estimates tile entity additions, removals, and updates.
- Preview and modern 1.18+ execution apply Y filtering through the same `BlockReplaceSource` predicate.
- Modern biome palette indices derive their packed bit width from the palette size and use the non-crossing 64-value storage shape. Multi-entry palettes with missing or malformed packed data fail closed instead of guessing a biome.
- Context-free pre-1.18 execution paths fail closed for tile, Y, and biome-restricted sources instead of silently dropping those conditions. Classic 1.9 execution also skips source modes that cannot be represented by its block-ID lookup.
- Section light arrays are removed after section mutation, and ReplaceBlocks writes the version-appropriate `isLightOn=0` / `LightPopulated=0` byte so Minecraft schedules relighting.
- Heightmaps are recomputed after replacement. Phase 6 fixed 1.17+ packed heightmap entry counts, early-flat nested `block_states` reads, single-palette section scans, and 21w43a+ root-level `Heightmaps` writeback.
- Air replacement has special handling that creates missing sections in the existing section range; Y-restricted air replacement now only completes sections whose section Y range intersects an air-matching Y-restricted source.

## Current pain points

- The UI now has a builder and Builder-local preview, but advanced syntax remains powerful and can still be hard to discover for tile entities and complex SNBT.
- Advanced query values containing `=`, `,`, `;`, `:` or SNBT must be quoted at the `ChangeParser` level.
- UI validation now shows ReplaceBlocks-specific messages for common failures, but parser diagnostics are still UI-side and do not provide a structured parser API.
- Legacy source block handling is intentionally asymmetric: bare/quoted compatibility sources remain regexes, while explicit `literal(...)`, `regex(...)`, `props(...)`, and exact-state sources carry their own source modes.
- Source state matching is exact, not subset matching. A partial properties compound will not match a full palette state.
- Selected-property matching is explicit through `props(...)`; existing source SNBT remains exact matching.
- The builder now emits `props(...)` for catalog-backed source property rules, but it does not yet offer per-property enable/disable checkboxes or a dedicated source-mode selector.
- The catalog currently includes Java 1.21.9. More versions need additional generated resources and a selection strategy.
- Quoted custom target names appear fragile when followed by another rule or tile entity SNBT; this needs a focused test.
- Modern 1.18+ target tile replacement removes existing block entities at the same coordinates before adding the replacement tile. Phase 6 also made the 1.13 and 1.17 palette paths remove all existing entries at the target coordinate before adding replacement tile SNBT. DataVersion 4671 copied-world files passed remove 2 / add 11 / update 2 checks with zero duplicate block-entity coordinates; Minecraft load/reload inspection is still required.
- Preview exists for modern 1.18+ paths, but unsupported older preview chunks are reported instead of estimated.
- Replacing air can expand sparse sections across the existing section range, which is powerful but high risk.
- Y-restricted air replacement reduces this risk by not completing sections outside the requested Y range. DataVersion 2860 and 4671 copied-world files matched and wrote exactly 20,736 and 20,479 Y=80 air blocks, with no remaining source matches; Minecraft rendering/reload validation is still required.
- Replacing blocks removes section light data and marks the chunk lighting incomplete. Clearing `isLightOn` fixed selected 1.21 chunks, but the user found stale light in the immediately adjacent ring. Selection-only execution now expands only the processing/save scope by one square chunk ring and clears the relight flag there; block replacement still uses the original selection. Final in-game boundary confirmation remains required.
- Automated Phase 6 tests verify early-flat and post-21w43a heightmap scan, packing, and writeback shape. File-level copied-world checks found all four heightmaps present at 37 longs with no malformed arrays after ordinary, state, and bounded-air execution. Minecraft surface behavior and logs remain manual gates.

## UI status and improvement recommendation

Implemented:

- `ChangeNBTDialog` keeps the existing raw ReplaceBlocks field and advanced query.
- `ReplaceBlocksRuleBuilderDialog` builds simple rules from `from` and `to` inputs.
- `ReplaceBlocksRuleBuilderDialog` uses `BlockStateCatalog.latestJava()` for searchable from/to block selectors and property dropdown rows.
- `ReplaceBlocksRuleBuilderDialog` exposes an additive source tile selector that generates `tile(...)` or `no_tile(...)`.
- `ReplaceBlocksRuleBuilderDialog` exposes source min/max Y inputs that generate `y(min..max, source)` when either field is filled.
- Builder inputs accept block IDs, unknown/modded resource locations, and block state SNBT.
- Empty builders start with blank From/To inputs and no automatic empty-query popup. An explicit click on an empty From/To or Biome arrow fills and opens the complete sorted catalog; ordinary empty focus/text changes keep suggestions closed. Property dropdowns default to `all`/`全部`; selecting specific source properties generates `props(...)`, while leaving every property at `all` generates a simple `literal(...)` source.
- `ReplaceBlocksRuleBuilderDialog` has a `Preview` button for ReplaceBlocks dry-run counts, per-rule matched block rows, and overlap warnings.
- `ReplaceBlocksDiagnostics` surfaces common validation errors and regex warnings.
- `BlockStateCatalog` provides the UI data source for vanilla block IDs and properties.

## Builder JavaFX input lessons

The current From/To block inputs use editable JavaFX `ComboBox` controls backed by a filtered block catalog. This area took several UI iterations, so future polish should keep these constraints in mind:

- Do not prefill or prompt the From/To fields with example block IDs. It makes the editor feel like it already contains user text and made selection/typing behavior harder to reason about.
- Keep initial validation quiet. An empty new builder should not show `Add at least one rule` until the user tries to add/confirm an invalid rule.
- Empty block queries should not expose the full block catalog automatically. An explicit empty-arrow click may show it for that popup only; closing without completion must clear the transient candidates so closed-key navigation cannot write a value.
- Use a filtered backing list/predicate rather than replacing the ComboBox items list on every keystroke.
- Do not synchronously clear selection, clear value, or refilter items from inside the ComboBox popup mouse-selection event path. That produced JavaFX `ListViewBehavior` `IndexOutOfBoundsException` errors when users clicked suggestions, while Tab completion could still appear fine.
- Mouse-click completion and Tab completion both need explicit manual tests. They can travel different JavaFX event paths even though they look like the same feature to the user.
- Candidate hover/focus/selected styles should stay visible in dark theme and should match the main menu hover tone closely enough that dropdowns feel interactive.
- Property dropdown cells use graphic `Text` nodes; set both CSS `-fx-text-fill` on list cells and explicit `Text#setFill`/`.text {-fx-fill: ...}` styles, otherwise hover/focus can leave some options rendered black on the dark popup.
- Keep custom presets as serialized ReplaceBlocks text, not hidden builder state. This preserves advanced text round-tripping and keeps presets independent of future UI layout changes.
- Normalize custom preset rules through `ReplaceBlocksField` before storing or comparing them. Raw SNBT ordering and whitespace are not rule identity, and equivalent rules must not be appended twice.
- Clicking empty space in the rule table clears its selection so the documented all-rules preset fallback remains reachable. Closing a nonempty Builder through the window close control requires discard confirmation.

Recommended next work:

- Biome restriction granularity is block-position aware at the modern chunk 4x4x4 biome-cell level. Do not change this to chunk/selection-wide matching without updating parser tests, preview expectations, execution tests, and docs.
- Keep rich target tile NBT editing out of the builder until the remaining Minecraft tile load/reload gate has passed.
- Preserve duplicate block-entity coordinate checks in future copied-world release runs.

## Risks

- World data mutation is destructive. Always use copies.
- Air replacement can fill all missing sections in the detected range.
- Regex source matching can affect more blocks than the user expects.
- Source-state matching requires the full stored block-state compound; partial property SNBT intentionally does not match.
- `props(...)` can intentionally match more states than exact SNBT because unlisted properties are ignored.
- Tile-target duplicate cleanup is covered by automated tests and DataVersion 4671 file-level copied-world checks; Minecraft load/reload remains required.
- Y range parser, diagnostics, preview counts, modern execution, and bounded-air copied-world files pass; Minecraft rendering/reload remains required before release.
- Light arrays are removed and the chunk relight flag is cleared with a ByteTag; Minecraft is expected to recalculate on load.
- Heightmap writeback shape passes file-level DataVersion 2860/4671 checks; Minecraft surface rendering and logs remain to be checked.
- Parser compatibility is important because CLI and UI share `ChangeParser` and `ReplaceBlocksField`.
- Builder, preview, UI field, and advanced query must continue to round-trip through the ReplaceBlocks text format.
- Catalog data must remain a UI/help source until a later phase explicitly changes matching semantics.
- Do not overload existing source SNBT as selected-property matching; exact state matching must remain exact. Use `props(...)` for selected-property matching.
- Per-rule preview is now implemented and is the baseline for safely adding more rule conditions.

## Test plan summary

Detailed plan: `docs/TEST_PLAN.md`.

Minimum validation before UI work:

- Create disposable 1.18+ and 1.21+ test worlds.
- Use obvious blocks, stateful blocks, tile entity blocks, and air gaps.
- Verify normal block replacement.
- Verify block state replacement.
- Verify `BlockStateCatalog` loads `minecraft:acacia_trapdoor`, exposes `facing/half/open/powered/waterlogged`, and treats colored wool as separate IDs with no properties.
- Verify tile entity replacement and absence of duplicate block entities.
- Verify air replacement scope.
- Verify heightmaps and light behavior by loading in Minecraft and checking logs.

## Phased development route

Detailed roadmap: `docs/ROADMAP.md`.

- Phase 0: validate current behavior and document reproducible examples.
- Phase 1: rule builder UI implemented.
- Phase 2: validation and error messages implemented.
- Phase 3: preview/dry-run counts implemented for modern 1.18+.
- Phase 4: exact source block-state matching implemented; Phase 4A 1.21.9 block-state catalog implemented; Phase 4B property-aware builder UI implemented; Gate A source matching design completed; Phase 4C/4D explicit source modes implemented.
- Phase 4E: tile/block entity source filters, clearer Extra NBT Builder labels/help, preview add/remove/update estimates, modern duplicate tile cleanup, and user-reported copied-world in-game validation implemented.
- Phase 5A: per-rule preview counts, source-mode rows, and overlap warnings implemented.
- Phase 4F-1: Y range restrictions implemented with `y(min..max, source)`, Builder min/max Y controls, parser/diagnostic tests, preview tests, and modern 1.18+ execution tests.
- Phase 4F-2: biome restrictions implemented with `biome(<biome>[;<biome>...], source)`, Builder source biome input, parser/diagnostic tests, preview tests, and modern 1.18+ execution tests.
- Phase 4G: built-in presets are visible input fillers with air/container warnings; custom presets use selected-rule/draft/all-rules save precedence and append on load. Next route is the focused Phase 6 rerun.

Detailed next-development plan: `docs/NEXT_DEVELOPMENT_REPLACE_BLOCKS.md`.
