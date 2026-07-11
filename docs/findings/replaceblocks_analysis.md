# ReplaceBlocks Analysis

Date: 2026-06-04
Last updated: 2026-07-07

Scope: targeted reconnaissance of ReplaceBlocks parsing and modern 1.18+ implementation.

## Field system entry

`FieldType.REPLACE_BLOCKS` is registered with:

- display name `ReplaceBlocks`
- constructor `ReplaceBlocksField::new`
- `headlessOnly = false`, so it appears in the JavaFX NBT Changer UI
- `clearCache = true`

`Field` stores `newValue`, exposes `parseNewValue(...)`, and defines `change(...)` / `force(...)`.

`ChangeParser` parses advanced text in the form:

```text
FieldName = value, OtherField = value
```

For ReplaceBlocks, advanced values usually need outer quotes because the generic parser's unquoted value character set does not include `=`, `,`, `:`, `;`, or SNBT punctuation.

Example advanced query:

```text
ReplaceBlocks = "minecraft:stone=minecraft:dirt"
```

The field-row text box in `ChangeNBTDialog` passes only the ReplaceBlocks value to `ReplaceBlocksField.parseNewValue(...)`, so it does not need the outer `ReplaceBlocks = "..."` wrapper.

## ReplaceBlocksField input format

Declared format:

```text
<from=to>[,<from=to>,...]
```

Source (`from`) formats:

- `minecraft:<block-name>`; legacy Java regex source matching
- `<block-name>`; normalized to `minecraft:<block-name>`, registry-validated, and then treated as legacy Java regex source matching
- `'<custom-block-name-with-namespace>'`; quotes stripped, not registry-validated, and treated as legacy Java regex source matching
- `literal(<block-name>)`; explicit literal source ID matching
- `regex(<java-regex>)`; explicit Java regex source matching
- `props(<snbt-string-block-state-with-Name-and-Properties>)`; selected-property matching against one source block name
- `y(<min>..<max>, <source>)`; source-side world-Y range filter, with either boundary optional
- `<snbt-string-block-state>`; exact match against the stored palette block-state compound

Target (`to`) formats:

- `minecraft:<block-name>`
- `<block-name>`; normalized to `minecraft:<block-name>` and registry-validated
- `'<custom-block-name-with-namespace>'`
- `<snbt-string-block-state>`
- `<to>;<snbt-string-tile-entity>`

Examples:

```text
stone=dirt
minecraft:stone=minecraft:dirt
literal(minecraft:stone)=minecraft:dirt
regex(minecraft:.*_log)=minecraft:stone
props({Name:"minecraft:oak_stairs",Properties:{facing:"north"}})=minecraft:stone
y(-64..64, literal(minecraft:stone))=minecraft:dirt
y(64.., tile(literal(minecraft:chest)))=minecraft:stone
{Name:"minecraft:oak_stairs",Properties:{facing:"north",half:"bottom",shape:"straight",waterlogged:"false"}}={Name:"minecraft:oak_stairs",Properties:{facing:"south",half:"bottom",shape:"straight",waterlogged:"false"}}
stone={Name:"minecraft:oak_log",Properties:{axis:"y"}}
chest=minecraft:barrel;{id:"minecraft:barrel"}
```

## Parsing behavior

`ReplaceBlocksField.parseNewValue(...)`:

- trims the full value
- loops over comma-separated rules
- reads source SNBT as a compound when the source starts with `{`
- otherwise reads the source up to the next `=`
- normalizes and validates unquoted non-namespaced source blocks
- parses target SNBT block state when `to` starts with `{`
- parses quoted target names when `to` starts with `'`
- parses simple target names until `,` or `;`
- parses optional tile entity SNBT after `;`
- stores rules in `Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData>`
- returns `false` through `super.parseNewValue(...)` on any parse failure
- parses source tile wrappers `tile(...)` and `no_tile(...)`, which can wrap existing source expressions such as `literal(...)`, `regex(...)`, `props(...)`, source SNBT, `y(...)`, or legacy bare sources
- parses source Y wrapper `y(min..max, source)`, which can wrap existing source expressions and requires at least one integer boundary
- parses source biome wrapper `biome(<biome>[;<biome>...], source)`, which can wrap existing source expressions and uses block-position-aware biome matching in modern 1.18+ preview/execution; the stored biome value covers a 4x4x4 block cell

Important details:

- Source name strings still use `String.matches(...)` at execution time and therefore behave like Java regex patterns.
- Source block state SNBT uses exact `CompoundTag` equality against the palette entry. This is not subset matching; include the full stored state properties when matching directional, waterlogged, slab, stair, trapdoor, and similar blocks.
- A source that already starts with `minecraft:` is not registry-validated in the source parser path.
- A quoted source is not registry-validated.
- Replacement uses `BlockReplaceSource.matches(blockState)`, which now dispatches by source mode: legacy regex name, explicit regex name, literal name, exact state, or selected properties.
- Explicit source wrappers are implemented as `regex(...)`, `literal(...)`, `props(...)`, `tile(...)`, `no_tile(...)`, `y(...)`, and `biome(...)`.
- `BlockReplaceData(String)` creates a block state compound `{Name:<name>}`.
- `BlockReplaceData(CompoundTag)` uses the SNBT compound directly as the target block state.
- `BlockReplaceData.toString()` emits either a block name, SNBT state, or `target;tileSNBT`.
- `ReplaceBlocksField.toString()` wraps the whole ReplaceBlocks value in quotes and escapes embedded quotes so advanced-query round trips work with source and target SNBT.

## Inputs likely to fail or surprise

- Advanced query without quotes:

```text
ReplaceBlocks = stone=dirt
```

The generic parser will not pass the full value to `ReplaceBlocksField`.

- Missing `=` inside the ReplaceBlocks value:

```text
stone
```

- Empty values:

```text
=dirt
stone=
```

- Invalid target registry name:

```text
stone=not_a_real_block
```

- Invalid SNBT:

```text
stone={Name:"minecraft:dirt"
```

- SNBT tile separator with no tile:

```text
chest=barrel;
```

- Quoted target followed by another token may be fragile:

```text
stone='custom:block', dirt=grass_block
stone='custom:block';{id:"custom:tile"}
```

The quoted-target parser appears to read to the end of the remaining string before checking for the closing quote. This needs a focused regression test before UI exposes quoted custom targets.

- Regex-like source values can match more than intended if quoted or otherwise accepted:

```text
'minecraft:.*_log'=minecraft:stone
```

This may be a hidden feature, but the UI should not accidentally introduce regex behavior without labeling it.

- Partial source-state SNBT may not match:

```text
{Name:"minecraft:oak_stairs",Properties:{facing:"north"}}=stone
```

Source-state matching is exact. If the stored palette entry also has `half`, `shape`, or `waterlogged`, the partial compound above will not match it.

- Invalid Y range wrappers fail:

```text
y(.., literal(stone))=dirt
y(10..0, literal(stone))=dirt
y(foo..10, literal(stone))=dirt
```

Use at least one integer boundary, and keep the minimum less than or equal to the maximum.

## Version dispatch

`VersionHandler.init()` indexes classes annotated with `@MCVersionImplementation`. `VersionHandler.getImpl(data, ChunkFilter.Blocks.class)` reads the chunk DataVersion and selects the implementation using `floorEntry(dataVersion)`.

For modern versions:

- DataVersion 2834: `ChunkFilter_21w37a.Blocks`
- DataVersion 2844 and later: `ChunkFilter_21w43a.Blocks`
- 1.19, 1.20, and 1.21 files inspected do not define newer `Blocks` implementations, so they inherit the 21w43a Blocks behavior through version floor selection unless another unread file overrides it.

## Modern 1.18+ replaceBlocks details

Primary implementation inspected: `ChunkFilter_21w43a.Blocks.replaceBlocks(...)`.

High-level flow:

- read root `sections`
- read chunk `xPos` and `zPos`, convert chunk position to block position
- compute section range with `Helper.findSectionRange(...)`
- if replacing `minecraft:air`, complete missing sections in the current section range
- if replacing `minecraft:air`, complete only missing sections whose section Y range intersects an air-matching source Y filter
- read root `block_entities`, or create an empty list
- for every section:
  - get `block_states`
  - get `block_states.palette`
  - get `block_states.data`
  - if palette has one entry and data is absent, synthesize `new long[256]`
  - remove `BlockLight` and `SkyLight`
  - iterate 4096 block indices
  - get current palette entry using `getBlockAt(...)`
  - match the current palette block state against each `BlockReplaceSource`
  - source matching dispatches through `BlockReplaceSource.matches(...)`, including legacy regex, explicit regex, literal, exact-state, selected-property modes, source tile eligibility, and source Y range
  - write target state using `setBlockAt(...)`
  - add, remove, or update block entity data at the absolute block location
  - cleanup palette after the section loop
  - write `block_states.data`, or remove it if the helper returns null
- write root `block_entities`

## Palette and block_states.data

The modern implementation uses a section-local palette and compacted block state array:

- `palette`: `ListTag` of block state compounds.
- `block_states.data`: long array of palette indices for 4096 block positions.

The inherited helpers from older version classes do the bit work:

- `indexToLocation(i)` maps section index to local x/y/z.
- `getBlockAt(index, blockStates, palette)` reads a palette index and returns the palette compound.
- `setBlockAt(index, blockState, blockStates, palette)` adds the target state to the palette if missing, grows `block_states.data` bit width if needed, and writes the palette index.
- `cleanupPalette(blockStates, palette)` removes unused palette entries, ensures air exists in the palette, and rewrites indices.
- `ChunkFilter_20w17a.Blocks` overrides bit packing to use `floor(64 / bits)` entries per long, matching newer compacted palette layout.

## Block entities / tile entities

In 21w43a+, block entities are stored at root key `block_entities`.

When a replacement has tile entity SNBT:

- the tile compound is copied
- `x`, `y`, and `z` are set to the absolute replaced block coordinates
- existing block entities at the same coordinates are removed
- the tile compound is appended to `block_entities`

When a replacement has no tile entity SNBT:

- the implementation searches `block_entities`
- if tile entities at the replaced coordinate exist, it removes them

Source-side tile filters:

- `tile(<source>)` matches only original block positions that already have a block entity.
- `no_tile(<source>)` matches only original block positions that do not already have a block entity.
- The Builder labels these source filters as `Extra NBT: any/present/absent`; the Help dialog explains that this only checks whether extra block entity data exists, not its contents.
- For overlapping rules, the tile source eligibility is based on the original block entity presence at that coordinate; add/remove/update effects still follow rule order.

Automated test coverage now verifies tile-only/no-tile preview counts, tile-to-tile update estimates, and duplicate cleanup for modern 1.18+ target tile replacement. User-reported copied-world in-game validation completed on 2026-06-28; future tile UI expansion still needs its own copied-world checklist.

Older 21w37a-era code uses `TileEntities`/level-style naming in some paths. The modern 21w43a code uses `block_entities`.

## Heightmaps

`ReplaceBlocksField.change(...)` calls four heightmap methods after block replacement:

- `worldSurface(data)`
- `oceanFloor(data)`
- `motionBlocking(data)`
- `motionBlockingNoLeaves(data)`

For 21w43a+, `Heightmap.getHeightMap(...)` scans root `sections`, reads `block_states.palette` and `block_states.data`, walks x/z columns from highest section down, and writes packed height values based on matcher predicates loaded from version-specific heightmap config resources.

Phase 6 confirmed and fixed three heightmap defects: packed values used the wrong entries-per-long loop bound, 21w37a read palette/data from the wrong section level, and 21w43a inherited a `Level.Heightmaps` writer even though its chunks store `Heightmaps` at root. Both early-flat and post-21w43a shapes now have automated scan/packing/writeback tests, including single-palette sections without a data array. DataVersion 2860/4671 copied files also retained all four 37-long heightmaps after ordinary, state, and bounded-air execution. Minecraft surface rendering, save/reload, and log validation remain release gates.

## Light data

For each changed section, the implementation removes:

- `BlockLight`
- `SkyLight`

The inspected ReplaceBlocks path does not remove root `isLightOn`. Minecraft or later processing may need to recalculate lighting. This should be included in world-load validation.

## Replacing air

Air replacement is handled specially because sparse chunks may omit all-air sections.

When `replace` contains `minecraft:air`:

- existing sections are collected by section Y
- missing section Y values inside the detected section range are created only when an air-matching source may match that section's block-Y span
- existing sections missing `block_states` are completed only when an air-matching source may match that section's block-Y span
- sections are sorted by Y and written back

`completeSection(...)` for 1.18-style sections:

- writes section `Y`
- creates `block_states`
- creates `block_states.data` with `new long[256]`
- creates `block_states.palette` with `minecraft:air`
- creates default biome data if missing

Risk: this can materially expand sparse chunk data across the existing section range. Y-restricted air replacement narrows this to sections intersecting the requested Y range, but a partial-section Y range may still require creating a whole 16-block-high section.

## Preview / dry-run

`ReplaceBlocksRuleBuilderDialog` exposes a Builder-local `Preview` button for the current generated ReplaceBlocks value. The raw field remains editable; opening Builder reparses existing values into the rule table where possible so advanced text can still be previewed through the Builder path.

Preview path:

- `ReplaceBlocksRuleBuilderDialog.previewReplaceBlocks(...)`
- `ReplaceBlocksPreviewer.preview(...)`
- read selected region data
- for each real region chunk, call `ChunkFilter.Blocks.previewReplaceBlocks(...)`
- show scanned regions/chunks, affected chunks/sections, aggregate matched block count, per-rule matched block rows, tile-entity add/remove/update estimates, overlap warnings, and other warnings

Safety behavior:

- Preview does not call `replaceBlocks(...)`.
- Preview does not call `Region.save(...)` or `saveWithTempFiles(...)`.
- Preview does not enqueue save jobs.
- Unsupported older chunk formats are counted as unsupported preview chunks.

Modern preview coverage:

- `ChunkFilter_21w37a.Blocks.previewReplaceBlocks(...)`
- `ChunkFilter_21w43a.Blocks.previewReplaceBlocks(...)`

Preview uses the same `BlockReplaceSource.matches(...)` source semantics as real replacement, including exact source-state SNBT matching, source tile eligibility, and source Y ranges.

Per-rule preview behavior:

- Aggregate matched blocks count each block position once if any rule matches.
- Per-rule rows count every rule match separately and show source mode, generated source text, target text, and matched block count.
- If more than one rule matches the same original block position, preview reports an overlap warning because per-rule row totals can exceed aggregate matched blocks.
- Y range preview counts use world block Y, not section Y. A rule such as `y(0..0, literal(minecraft:stone))=minecraft:dirt` matches one horizontal layer, or 256 blocks in a full 16x16 section.

## Block-state catalog foundation

Phase 4A adds a UI-facing block-state catalog without changing parser or execution behavior.

Files:

- `src/main/resources/mapping/block_states/java_1_21_9.json`
- `src/main/java/net/querz/mcaselector/version/mapping/blockstate/BlockStateCatalog.java`
- `src/test/java/net/querz/mcaselector/version/mapping/blockstate/BlockStateCatalogTest.java`

Data source:

- Generated from Mojang Java Edition 1.21.9 server `reports/blocks.json`.
- Stores block IDs, allowed property values, default properties, and state counts.
- Does not store the full state list or block definitions.

Current catalog and UI behavior impact:

- Phase 4B builder UI uses the catalog to populate searchable block selectors and property dropdown choices.
- Builder-generated target property choices serialize as block-state SNBT with `Name` and only the properties not left at `all`.
- Builder-generated simple source choices serialize as `literal(...)`.
- Builder-generated source property choices serialize as `props(...)`.
- Builder-generated source Y fields serialize as `y(min..max, source)` when either boundary is set.
- Builder-generated source biome fields serialize as `biome(<biome>[;<biome>...], source)` when one or more biome IDs are entered.
- Empty builders start with blank From/To inputs and stay visually quiet until user action produces a real diagnostic.
- Empty block selector queries do not show the full catalog; suggestion completion should be verified through both Tab and mouse-click paths because JavaFX handles those paths differently.
- Property dropdowns default to `all`/`全部`; source properties left at `all` are omitted from `props(...)`, and a source with every property at `all` serializes as `literal(...)`.
- Existing source SNBT remains exact matching; selected-property subset matching is explicit through `props(...)`.
