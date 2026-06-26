# ReplaceBlocks Test Plan

Date: 2026-06-04
Last updated: 2026-06-26

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
- Opening the builder with an empty ReplaceBlocks value should prefill real default inputs, and pressing `Add rule` immediately should add the valid default `literal(minecraft:stone)=minecraft:dirt` rule.
- Search/select `minecraft:acacia_trapdoor`; the builder should show `facing`, `half`, `open`, `powered`, and `waterlogged` dropdowns with defaults.
- Change several trapdoor properties and add the rule; the generated text should use full block-state SNBT with `Name` and `Properties`, then parse successfully.
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
- In the builder, a simple source block rule should generate `literal(...)`.
- In the builder, selecting source block properties should generate `props(...)`.

## UI interaction regression checks

Manual checks:

- In the NBT Changer field row, type `minecraft:stone=minecraft:dirt` directly into ReplaceBlocks. The field should stay visually neutral while typing and only show valid/invalid feedback after a short pause.
- With the default NBT Changer dialog size, the ReplaceBlocks `Builder` button should be visible without dragging the horizontal scrollbar.
- Open an empty builder and press `Add rule` without editing the default from/to values. It should add a valid `literal(minecraft:stone)=minecraft:dirt` rule instead of showing an invalid-source message.

## Per-rule preview counts

Implemented as Phase 5A. Preserve these checks before tile filters, Y range, biome restrictions, or presets.

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

- `ReplaceBlocksPreviewCountsTest` builds an in-memory modern section and verifies aggregate matched blocks, per-rule counts, and overlap count without touching world files.

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

Before tile UI expansion:

- Verify tile-to-tile replacement on copied worlds.
- Decide whether any duplicate block entity behavior must be fixed before exposing target tile NBT editing.
- Prefer include/exclude tile-entity-source filters before rich tile NBT editing.

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

When Y range is implemented:

- Repeat air replacement on a tiny copied selection with a narrow Y range.
- Verify missing sections are not created outside the requested Y range.
- Compare preview counts with execution on a fresh copied world.

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
