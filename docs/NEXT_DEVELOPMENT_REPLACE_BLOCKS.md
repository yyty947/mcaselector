# ReplaceBlocks Next Development Plan

Date: 2026-06-14
Gate A decision date: 2026-06-14

Purpose: prevent rework during the remaining ReplaceBlocks phases by separating source-matching semantics, preview observability, tile safety, spatial restrictions, and release hardening.

## Current Baseline

Implemented behavior:

- The builder can create simple block rules.
- The builder uses `BlockStateCatalog.latestJava()` for searchable block IDs and property dropdowns.
- Builder-generated target property rules produce block-state SNBT containing the properties not left at `all`.
- Builder-generated simple source block IDs produce `literal(...)`.
- Builder-generated source property rules produce `props(...)` when at least one property is not left at `all`; all-source-properties `all` produces `literal(...)`.
- Empty builder From/To inputs start blank, do not immediately show validation errors, and do not show the full block catalog before the user types.
- Advanced text input remains available.
- Source name strings still use Java regex matching through `String.matches(...)`.
- Source SNBT uses exact `CompoundTag` equality against palette block states.
- Explicit regex source mode is available through `regex(...)`.
- Explicit literal source mode is available through `literal(...)`.
- Selected-property source matching is available through `props(...)`.
- Preview is non-mutating and supports modern 1.18+ chunk formats.
- Preview shows aggregate counts plus one per-rule row with source mode, generated source text, target text, and matched block count.
- Preview warns when more than one rule matches the same original block position.
- Tile source filters are available through `tile(...)` and `no_tile(...)`.
- The builder exposes tile source filters as `Extra NBT: any/present/absent` and has an extensible Help dialog for Builder explanations.
- Preview estimates tile entity additions, removals, and updates.
- Modern 1.18+ tile-target replacement removes existing block entities at the same coordinates before adding target tile SNBT.
- Y range filters are available through `y(min..max, source)` and are applied in modern preview and execution.
- The builder exposes source-side min/max Y inputs and leaves them empty by default.

Not implemented:

- Dedicated source-mode selector UI and per-property subset checkboxes.
- Biome restrictions and presets.
- Rich target tile NBT editing.

## Source Matching Model

Gate A and Phase 4C/4D are implemented. Keep the model below stable unless parser tests, preview expectations, and docs are updated together.

Source modes:

- `legacy-regex-name`: existing bare or quoted source string behavior. This remains compatibility mode and still calls `String.matches(...)` on the palette state's `Name`.
- `regex-name`: explicit regex source mode using `regex(...)`. Runtime matching is the same as legacy regex mode, but the syntax, labels, and diagnostics show that regex behavior is intentional.
- `literal-name`: explicit block ID mode using `literal(...)`. Runtime matching compares the palette state's `Name` with `String.equals(...)`; regex metacharacters have no special meaning.
- `exact-state`: existing source SNBT mode. A source that starts with `{` remains full `CompoundTag.equals(...)` against the palette block-state compound.
- `selected-properties`: explicit subset mode using `props(...)`. Runtime matching requires exact `Name` equality and equality for every selected property key only.

Compatibility rules:

- Do not change bare source strings. `stone=dirt`, `minecraft:stone=minecraft:dirt`, and quoted legacy sources must behave as they do now.
- Do not reinterpret existing source SNBT. `{Name:"minecraft:oak_stairs",Properties:{facing:"north"}}=...` remains exact-state matching and may not match stored palette states with additional properties.
- Add new syntax only for new behavior. Literal and selected-properties matching must be opt-in.
- The builder generates `literal(...)` for ordinary simple source block IDs.

## Chosen Raw Syntax

New syntax applies only to the source side of `from=to`. Target syntax remains unchanged.

```text
minecraft:stone=minecraft:dirt
regex(minecraft:.*_log)=minecraft:stone
literal(minecraft:stone)=minecraft:dirt
literal(stone)=minecraft:dirt
{Name:"minecraft:oak_stairs",Properties:{facing:"north",half:"bottom",shape:"straight",waterlogged:"false"}}=minecraft:stone
props({Name:"minecraft:oak_stairs",Properties:{facing:"north"}})=minecraft:stone
y(-64..64, literal(minecraft:stone))=minecraft:dirt
y(64.., tile(literal(minecraft:chest)))=minecraft:stone
```

Meanings:

- `minecraft:stone=minecraft:dirt` is legacy regex-name mode. It happens to behave like a literal match for this ID, but the semantics remain regex.
- `regex(minecraft:.*_log)=minecraft:stone` is explicit regex-name mode. The argument is the Java regex pattern used by `String.matches(...)`.
- `literal(minecraft:stone)=minecraft:dirt` is exact block ID mode.
- `literal(stone)=minecraft:dirt` normalizes the source to `minecraft:stone`.
- Bare source SNBT remains exact-state mode.
- `props(...)` parses the inner SNBT as a block state selector, but only the listed `Properties` keys participate in matching.
- `y(min..max, source)` limits the wrapped source expression by world block Y. Either boundary may be omitted, for example `y(64.., literal(stone))` or `y(..0, literal(stone))`, but at least one integer boundary is required.

Wrapper parsing decisions:

- Supported wrappers are `literal(...)`, `regex(...)`, and `props(...)`.
- Spatial source wrapper `y(min..max, source)` may wrap the other source expressions.
- Wrapper names are lowercase and source-side only.
- `literal(...)` accepts a block ID or short vanilla ID. Short IDs normalize to `minecraft:<id>`.
- `regex(...)` accepts a Java regex pattern and should be validated by compiling the pattern before execution.
- If a literal or regex argument needs a closing parenthesis or leading/trailing whitespace, allow a single-quoted argument such as `regex('minecraft:(oak|birch)_log')`.
- `props(...)` must contain valid SNBT whose root compound has `Name` and a non-empty `Properties` compound.
- `props(...)` with no selected properties should be rejected with a targeted diagnostic; use `literal(...)` for name-only matching.
- Invalid Y ranges such as `y(.., source)`, `y(10..0, source)`, or `y(foo..10, source)` should be rejected with a targeted diagnostic.

Selected-properties matching:

- Compare `Name` with exact string equality.
- Read the selector's `Properties` compound.
- For every selected property key, require the palette state's `Properties` compound to contain the same key with an equal tag value.
- Ignore palette properties that are not listed in the selector.
- If the palette state has no `Properties`, or is missing any selected key, it does not match.

## Builder Contract

The builder now exposes source intent in generated text for the modes it can emit.

Recommended builder source modes:

- `Block ID`: generate `literal(<source>)=<target>`.
- `Regex`: generate `regex(<pattern>)=<target>`.
- `Exact state`: generate existing source SNBT directly.
- `Selected properties`: generate `props(<source-state-selector>)=<target>`.

Builder property behavior:

- Known catalog block properties default to `all`.
- Selected source properties serialize through `props(...)`; source properties left at `all` are omitted.
- Target properties left at `all` are omitted; if every target property is `all`, the target serializes as a simple block name.
- Unknown or modded resource locations should still be enterable manually.
- Source min/max Y inputs default to empty; filling either field wraps the generated source with `y(...)`.
- Advanced text remains the escape hatch for legacy regex values and hand-written SNBT.

## Phase 4C/4D Implementation Status

Implemented:

- Parser and diagnostic tests cover legacy compatibility and new syntax.
- `ChunkFilter.BlockReplaceSource` carries a source-mode enum and round-trips every source mode.
- `matches(...)` supports literal-name and selected-properties.
- `matchesAir()` follows the same source-mode semantics as `matches(...)`.
- `ReplaceBlocksField.readSource(...)` and `valueToString()` round-trip every source mode.
- `ReplaceBlocksDiagnostics` reports targeted errors for invalid wrappers, invalid regex patterns, invalid literal IDs, and empty `props(...)` selectors.
- Preview and execution share `BlockReplaceSource.matches(...)`.
- Builder-generated simple block ID sources use `literal(...)`.

Known UI follow-up:

- The builder can emit `literal(...)` and `props(...)`, and advanced text can use `regex(...)`.
- A dedicated source-mode selector and per-property subset checkboxes are still future UX polish.

## Preview And Rule Ordering

Current execution scans rules in insertion order for each original block state. It does not break after the first matching rule. If multiple source rules match the same original state, more than one replacement operation can run for that position.

Phase 4C/4D preserves this behavior unless a later phase explicitly changes and tests rule conflict semantics.

Phase 5A per-rule preview now reports:

- aggregate matched blocks, counting each block position once when any rule matches.
- per-rule matched blocks, counting each rule match separately.
- a visible overlap warning when multiple rules match the same original block position.

## Acceptance Tests For 4C/4D

Parser and round-trip tests:

- `minecraft:stone=minecraft:dirt` parses as `legacy-regex-name` and serializes as the existing syntax.
- `'custom:block'=minecraft:stone` remains legacy regex-name compatibility syntax.
- `regex(minecraft:.*_log)=minecraft:stone` parses as `regex-name`.
- `literal(minecraft:stone)=minecraft:dirt` parses as `literal-name`.
- `literal(stone)=minecraft:dirt` serializes with `minecraft:stone`.
- Existing source SNBT parses as `exact-state`.
- `props({Name:"minecraft:oak_stairs",Properties:{facing:"north"}})=minecraft:stone` parses as `selected-properties`.
- Empty `props({Name:"minecraft:stone"})=minecraft:dirt` is rejected with a targeted diagnostic.
- Invalid regex syntax is rejected before execution.

Matching tests:

- Legacy regex behavior remains unchanged.
- Explicit regex and legacy regex match the same names for the same pattern.
- Literal mode treats regex metacharacters literally.
- Exact-state mode requires full compound equality.
- Selected-properties mode matches all north-facing stairs regardless of unselected `half`, `shape`, or `waterlogged` values.
- Selected-properties mode does not match another block name with the same property key.
- Preview and execution use the same matching result on copied worlds.

## Preview Baseline Before More Conditions

Source modes are now represented internally, and per-rule preview counts are implemented. Preserve this preview baseline while implementing biome restrictions or presets.

Reason:

- Total matched block counts are too coarse once rules can use literal, regex, exact-state, and selected-property matching.
- Per-rule counts make later condition work much easier to validate.

Preview baseline:

- Keep the aggregate summary.
- Keep one row per rule with generated source text, source mode, target text, and matched block count.
- Keep the overlap warning because execution can apply multiple matching rules to the same original state.
- Preserve warnings for air replacement, tile entities, lighting, heightmaps, and unsupported chunks.
- Keep preview non-mutating.

## Tile Entity Safety

Phase 4E is implemented as of 2026-06-28. User-reported in-game copied-world validation completed on 2026-06-28; keep copied-world checks in the release-hardening rhythm for future behavior changes.

Do not add rich tile NBT editing first.

Recommended order:

- Done: source-side `tile(...)` matches only original positions that already have a block entity.
- Done: source-side `no_tile(...)` excludes original positions that already have a block entity.
- Done: builder source controls can generate both wrappers without replacing advanced text input, using clearer Extra NBT labels and a Help dialog that can grow with later Builder documentation.
- Done: preview estimates tile add/remove/update effects and keeps per-rule rows.
- Done: target tile replacement removes existing block entities at the same coordinates before adding the new tile SNBT in modern 1.18+ paths.
- Add tile NBT editing later, with warning-heavy UI for containers, signs, banners, command blocks, and similar blocks.

## Spatial Restrictions

Implement in two parts:

- Done: Y range first.
- Biome restriction after Y range is stable.

Y range is implemented through `y(min..max, source)`. It is source-side, uses world block Y, and can wrap `literal(...)`, `regex(...)`, `props(...)`, source SNBT, `tile(...)`, or `no_tile(...)`. Modern 1.18+ preview and execution use the same Y predicate. Air replacement still must be tested on tiny copied selections because it can create sparse sections; automated tests cover narrow synthetic air-section preview counts, but copied-world validation is still required.

Biome restriction needs a documented granularity decision before coding:

- block-position aware
- section/palette aware
- chunk/selection aware

Do not implement biome UI until that decision is explicit.

## Presets

Add presets after source modes, preview, tile safety, and Y range are stable.

Presets should generate editable rules rather than hidden behavior. They must show source mode, target, tile behavior, Y range, and biome conditions when relevant.

## Testing Rhythm

Every remaining phase should include:

- Parser/source-mode tests when syntax or semantics change.
- Preview tests or manual preview checks before execution checks.
- Copied-world manual tests for any mutating behavior.
- JavaFX manual inspection for dialog changes.
- Documentation updates in the same change set.

Minimum command checks after Java changes:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
```

Use narrower tests during development when possible, then broaden before release hardening.
