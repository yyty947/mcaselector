# ReplaceBlocks Development Notes

Date: 2026-06-04
Last updated: 2026-06-26

Scope: ReplaceBlocks reconnaissance plus implemented phases 1-5A. Java source now includes a rule builder, validation diagnostics, modern preview/dry-run with per-rule counts, exact source block-state matching, a Java 1.21.9 block-state catalog foundation, a property-aware catalog-backed builder UI, and explicit source modes. Gradle build logic and Minecraft world data were not modified by these development notes.

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
- Target can be a simple registry name, an SNBT block state, or a block plus tile entity SNBT.
- Multiple rules are supported in one ReplaceBlocks value.
- Legacy bare or quoted source block-name matching still uses Java regex matching via `String.matches(...)`.
- Builder inputs accept simple block IDs, source wrappers, or block state SNBT with `Name`.
- Builder inputs can search/select Java 1.21.9 catalog block IDs, generate `literal(...)` for simple source IDs, generate `props(...)` for source property dropdowns, and generate full-state SNBT for target property dropdowns.
- The NBT Changer dialog shows ReplaceBlocks validation messages and warnings after a short typing pause, so incomplete in-progress input does not flash errors on every character.
- The default NBT Changer dialog width keeps the ReplaceBlocks `Builder` button visible without horizontal scrolling.
- When opened without an existing value, the builder starts with real default inputs `minecraft:stone` and `minecraft:dirt`, so `Add rule` immediately creates a valid example rule.
- The NBT Changer dialog offers ReplaceBlocks preview/dry-run counts for modern 1.18+ formats, including per-rule rows and overlap warnings.
- `BlockStateCatalog.latestJava()` loads the generated Java 1.21.9 block-state catalog used by the builder dropdown UI.
- For modern versions, replacement iterates all 4096 blocks per section.
- Palette entries are added as needed and unused palette entries are cleaned up.
- Existing tile/block entities are removed when replacing a block with a non-tile target.
- New tile/block entities are added when the target includes tile entity SNBT.
- Section light arrays are removed after section mutation.
- Heightmaps are requested for recomputation after replacement.
- Air replacement has special handling that creates missing sections in the existing section range.

## Current pain points

- The UI now has a builder and preview, but advanced syntax remains powerful and can still be hard to discover for tile entities and complex SNBT.
- Advanced query values containing `=`, `,`, `;`, `:` or SNBT must be quoted at the `ChangeParser` level.
- UI validation now shows ReplaceBlocks-specific messages for common failures, but parser diagnostics are still UI-side and do not provide a structured parser API.
- Legacy source block handling is intentionally asymmetric: bare/quoted compatibility sources remain regexes, while explicit `literal(...)`, `regex(...)`, `props(...)`, and exact-state sources carry their own source modes.
- Source state matching is exact, not subset matching. A partial properties compound will not match a full palette state.
- Selected-property matching is explicit through `props(...)`; existing source SNBT remains exact matching.
- The builder now emits `props(...)` for catalog-backed source property rules, but it does not yet offer per-property enable/disable checkboxes or a dedicated source-mode selector.
- The catalog currently includes Java 1.21.9. More versions need additional generated resources and a selection strategy.
- Quoted custom target names appear fragile when followed by another rule or tile entity SNBT; this needs a focused test.
- New tile entity replacement appears to append a tile entity without first removing an existing one at the same coordinates; verify for duplicate block entities.
- Preview exists for modern 1.18+ paths, but unsupported older preview chunks are reported instead of estimated.
- Replacing air can expand sparse sections across the existing section range, which is powerful but high risk.
- Replacing blocks removes section light data and relies on Minecraft or later processing to rebuild lighting.
- Heightmap writeback for post-21w43a flat chunk format should be verified in tests; reconnaissance found modern scanners, but did not find a modern `setHeightMap` override.

## UI status and improvement recommendation

Implemented:

- `ChangeNBTDialog` keeps the existing raw ReplaceBlocks field and advanced query.
- `ReplaceBlocksRuleBuilderDialog` builds simple rules from `from` and `to` inputs.
- `ReplaceBlocksRuleBuilderDialog` uses `BlockStateCatalog.latestJava()` for searchable from/to block selectors and property dropdown rows.
- Builder inputs accept block IDs, unknown/modded resource locations, and block state SNBT.
- Empty builders prefill real `minecraft:stone` -> `minecraft:dirt` inputs, and `Add rule` generates a valid `literal(...)` source rule.
- `ChangeNBTDialog` has a `Preview` button for ReplaceBlocks dry-run counts, per-rule matched block rows, and overlap warnings.
- `ReplaceBlocksDiagnostics` surfaces common validation errors and regex warnings.
- `BlockStateCatalog` provides the UI data source for vanilla block IDs and properties.

Recommended next work:

- Add tile entity source safety controls before Y range, biome restrictions, or presets.
- Verify duplicate block-entity behavior on copied worlds before exposing target tile NBT editing.
- Keep per-rule preview rows and overlap warnings intact as tile filters are added.
- Add tile entity editing only after duplicate block-entity behavior is tested on copied worlds.
- Consider Y range controls after tile safety work, using per-rule preview rows to verify scope.

## Risks

- World data mutation is destructive. Always use copies.
- Air replacement can fill all missing sections in the detected range.
- Regex source matching can affect more blocks than the user expects.
- Source-state matching requires the full stored block-state compound; partial property SNBT intentionally does not match.
- `props(...)` can intentionally match more states than exact SNBT because unlisted properties are ignored.
- Tile entity replacement may duplicate block entities if a tile target is applied over an existing tile entity.
- Light arrays are removed and may require Minecraft to recalculate.
- Heightmap writeback for 1.18+ must be empirically checked.
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
- Phase 5A: per-rule preview counts, source-mode rows, and overlap warnings implemented. Next route is tile safety, Y range, biome restrictions, presets, and release hardening.

Detailed next-development plan: `docs/NEXT_DEVELOPMENT_REPLACE_BLOCKS.md`.
