# ReplaceBlocks Test Plan

Date: 2026-06-04
Last updated: 2026-07-07

Safety rule: never test on a real world save. Always copy a small test world and keep an untouched backup.

## Catalog data tests

The Phase 4A block-state catalog is data-only and does not mutate worlds.

Automated checks:

```powershell
.\gradlew.bat test --tests net.querz.mcaselector.version.mapping.blockstate.BlockStateCatalogTest
```

Expected coverage:

- Java 1.21.9 catalog loads.
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

Manual checks:

- In the NBT Changer field row, type `minecraft:stone=minecraft:dirt` directly into ReplaceBlocks. The field should stay visually neutral while typing and only show valid/invalid feedback after a short pause.
- With the default NBT Changer dialog size, the ReplaceBlocks `Builder` button should be visible without dragging the horizontal scrollbar.
- Open an empty builder and confirm the From/To fields are blank, the generated value is empty, and no `Add at least one rule` error is shown before user action.
- Before typing in an empty builder From/To field, click the dropdown arrow. The builder should not show the full block catalog and should not position a popup above the input on first open.
- In the builder From/To fields, type `oak` or `sto`. The candidate list should open automatically, show every matching block ID in A-Z order, highlight the typed substring in blue, allow mouse scrolling, and collapse after Tab completion.
- Repeat the same suggestion test with mouse-click completion. The chosen block ID should fill the editor, the matching property rows should appear when applicable, and the JavaFX console should not log `ListViewBehavior` or index errors.
- Moving the mouse over block suggestions, Extra NBT choices, and property dropdown choices should show a visible hover highlight consistent with the main menu hover color.
- The builder helper text below the generated value should be visible before manual From/To input, then hide once the user types non-empty text into either From/To field.
- In the builder From block area, leaving Min Y and Max Y empty should generate the same rule as before.
- Filling only Min Y should generate `y(<min>.., source)`, filling only Max Y should generate `y(..<max>, source)`, and filling both should generate `y(<min>..<max>, source)`.
- Invalid Y input such as letters or Min Y greater than Max Y should prevent adding the rule and show validation feedback.
- Leaving the Builder Biome field empty should generate the same rule as before. Filling `plains` should generate `biome(minecraft:plains, source)`, filling `plains;minecraft:forest` should generate a semicolon-separated multi-biome wrapper, an unknown `minecraft:` biome ID should prevent adding the rule and show validation feedback, and a syntactically valid non-`minecraft:` biome ID should remain manually typeable.
- In the Builder Biome field, typing `pla` or `for` should open a filtered biome list, highlight the typed substring, and support Tab completion and mouse-click completion without console errors.
- In the Builder Biome field, typing after a semicolon such as `minecraft:plains;for` should complete only the current biome token, preserving the earlier token.
- Clicking the empty Builder Biome dropdown before typing should not show the full biome list.
- Choose each Builder preset and click `Use preset`. The From/To inputs should be filled visibly, remain editable, and require the normal `Add rule` action before the generated ReplaceBlocks value changes.
- The Air preset should fill `minecraft:air` -> `minecraft:stone` and show a warning about sparse/missing sections.
- The Fluids, Logs/leaves, and Ores presets should fill visible source-mode regex text and ordinary target blocks, then generate valid ReplaceBlocks text after `Add rule`.
- The Containers preset should set Extra NBT to present, fill a visible container source regex, target `minecraft:air`, show a data-loss warning, and generate a `tile(regex(...))=minecraft:air` rule after `Add rule`.

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
- Unsupported older preview chunks are still reported instead of silently counted.

Automated coverage:

- `ReplaceBlocksPreviewCountsTest` builds in-memory modern sections and verifies aggregate matched blocks, per-rule counts, overlap count, Y range preview counts, synthetic air-section Y filtering, and Y range execution filtering without touching world files.

## Test world preparation

- Create a small creative-mode Java Edition world.
- Save copies for at least:
  - Minecraft 1.18.x or compatible 1.18+ DataVersion
  - Minecraft 1.21.x or compatible 1.21+ DataVersion
- Put test chunks near spawn for easy inspection.
- Make a backup before every MCA Selector run.
- Keep region, entities, and poi files together when copying.

Suggested layout:

- one chunk with normal blocks
- one chunk with block states
- one chunk with containers / tile entities
- one sparse or empty area for air replacement
- signs or coordinate markers to identify positions

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

Phase 4F-2 manual copied-world checks:

- Pick a tiny copied selection crossing a visible biome boundary and replace a harmless marker block only in one biome.
- Confirm preview counts match execution on a fresh copy of the same selection.
- Verify blocks immediately across the biome boundary are not changed when their stored biome does not match.
- Repeat one combined rule with Y range plus biome condition if the boundary area has blocks at different heights.

Phase 4F-1 manual copied-world checks:

- Repeat air replacement on a tiny copied selection with a narrow Y range.
- Verify missing sections are not created outside the requested Y range.
- Compare preview counts with execution on a fresh copied world.
- Verify a normal block replacement across two visible heights, for example replacing stone with glass only at one marked Y layer.
- Verify a tile-filtered Y rule on containers if a copied test world has containers at different heights.

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

Optional:

- 1.17 world for old `Sections` / `TileEntities` comparison
- one pre-1.18 world only if a regression touches shared helpers

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
