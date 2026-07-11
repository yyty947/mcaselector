# ReplaceBlocks Roadmap

Date: 2026-06-04
Last updated: 2026-07-08

This roadmap is the current source of truth for ReplaceBlocks feature sequencing. The older linear phase list has been replaced with a dependency-driven route so later semantic work does not force repeated rewrites.

## Current Checkpoint

Implemented:

- Phase 1: independent `ReplaceBlocksRuleBuilderDialog` launched from `ChangeNBTDialog`.
- Phase 2: ReplaceBlocks-specific diagnostics and validation messages.
- Phase 3: non-mutating preview/dry-run for modern 1.18+ chunk formats; unsupported older preview chunks are reported instead of guessed.
- Phase 4: exact source block-state SNBT matching.
- Phase 4A: Java 1.21.9 block-state catalog foundation.
- Phase 4B: property-aware builder UI backed by `BlockStateCatalog.latestJava()`.
- Gate A: source matching design for Phase 4C/4D is decided in `docs/NEXT_DEVELOPMENT_REPLACE_BLOCKS.md`.
- Phase 4C/4D: explicit source modes implemented with `regex(...)`, `literal(...)`, and `props(...)`.
- Phase 4E: tile/block entity source safety controls implemented with `tile(...)` and `no_tile(...)`; target tile replacement now removes existing block entities at the target coordinates before adding replacement tile SNBT. The Builder labels this as an "Extra NBT" source condition and includes an extensible Help dialog for this and future Builder explanations.
- Phase 5A: per-rule preview counts, source-mode rows, and overlap warning in the ReplaceBlocks preview dialog.
- Phase 4F-1: Y range restrictions implemented with `y(min..max, source)`, Builder min/max Y fields, parser diagnostics, preview counts, and modern 1.18+ execution filtering.
- Phase 4F-2: biome restrictions implemented with `biome(<biome>[;<biome>...], source)`, Builder source biome input, parser diagnostics, preview counts, and modern 1.18+ execution filtering. Matching is block-position aware using the chunk biome value for the candidate block position; in modern chunks that value covers a 4x4x4 block cell.
- Phase 4G: Builder presets implemented for common replacement starting points. Built-in presets fill visible From/To and source condition controls, keep the generated rules editable before adding, and show warnings for air and container/data-block cases. Custom presets save the selected rule, otherwise the current valid draft, otherwise all table rules; loading appends non-duplicate rules without replacing current work.
- UI polish: ReplaceBlocks field-row diagnostics are debounced, the main dialog defaults wide enough to show `Builder`, empty Builder inputs start blank without immediate empty-rule errors or empty-query full-list popups, block suggestions support Tab and mouse-click completion, property dropdowns support an `all`/`全部` option, and Preview now lives beside Help in the Builder.

Still not implemented:

- Release hardening for broader copied-world regression coverage.

## Non-Negotiable Constraints

- Do not modify real Minecraft world saves.
- Do not change Gradle build logic during ReplaceBlocks UI work.
- Preserve advanced text input and legacy ReplaceBlocks values.
- Preserve existing `ReplaceBlocksField.change(...)` dispatch through `VersionHandler`.
- Do not silently change legacy source-name regex behavior.
- Do not silently change exact source-state SNBT matching into subset matching.
- Keep preview/dry-run non-mutating: no `replaceBlocks(...)`, no region save, no save jobs.
- Treat catalog data as UI/help data unless a later phase explicitly extends parser/execution semantics.

## Runtime Truth Anchors

Use these files before trusting any roadmap text:

- Parser and value model: `src/main/java/net/querz/mcaselector/changer/fields/ReplaceBlocksField.java`
- Source/target runtime types and preview contract: `src/main/java/net/querz/mcaselector/version/ChunkFilter.java`
- UI entrypoint: `src/main/java/net/querz/mcaselector/ui/dialog/ChangeNBTDialog.java`
- Builder UI: `src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderDialog.java`
- Diagnostics: `src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksDiagnostics.java`
- Preview scanner: `src/main/java/net/querz/mcaselector/io/job/ReplaceBlocksPreviewer.java`
- Modern execution: `src/main/java/net/querz/mcaselector/version/java_1_18/ChunkFilter_21w43a.java`
- Catalog: `src/main/java/net/querz/mcaselector/version/mapping/blockstate/BlockStateCatalog.java`

## Recommended Route

### Gate A: Source Matching Design

Status: completed as a design gate on 2026-06-14. The chosen source-mode model and syntax live in `docs/NEXT_DEVELOPMENT_REPLACE_BLOCKS.md`.

Goal: design Phase 4C and 4D together before changing parser or execution behavior.

Why this comes first:

- Selected-property matching and literal/regex modes both change how a source rule matches palette entries.
- Current raw syntax already has two legacy meanings: source names are Java regexes, source SNBT is exact state matching.
- Overloading partial SNBT as subset matching would silently break existing exact-match expectations.

Decisions:

- Internal source modes are `legacy-regex-name`, `regex-name`, `literal-name`, `exact-state`, and `selected-properties`.
- Legacy bare or quoted source strings stay as regex compatibility syntax.
- Existing bare source SNBT stays exact-state matching.
- New explicit source wrappers are `regex(...)`, `literal(...)`, `props(...)`, `tile(...)`, `no_tile(...)`, `y(...)`, and `biome(...)`.
- Builder-generated simple block-ID rules now use `literal(...)`.
- Parser and diagnostic tests must land before version-layer behavior changes.

Deliverable:

- Done: `docs/NEXT_DEVELOPMENT_REPLACE_BLOCKS.md` contains chosen syntax, source-mode semantics, builder contract, implementation order, and acceptance tests.
- Next: implement parser/diagnostic tests before touching version-layer behavior.

### Phase 4C/4D: Implement Explicit Source Modes

Status: implemented on 2026-06-14.

Goal: make source matching explicit while preserving legacy compatibility.

Recommended implementation order:

- Add/extend source-mode representation in `ChunkFilter.BlockReplaceSource` or a nearby type.
- Keep bare source strings as legacy regex mode.
- Keep source SNBT as exact state mode.
- Add explicit regex mode with `regex(...)`.
- Add explicit literal block ID mode with `literal(...)`.
- Add explicit selected-properties mode that matches `Name` exactly and requires every selected property to equal the palette state.
- Make builder-generated simple block-ID rules use literal mode only after parser/execution support exists.
- Keep advanced text warnings for legacy regex-looking sources.
- Preserve current rule iteration behavior: multiple matching rules can apply to the same original block state because execution does not break after the first match.

Success criteria:

- Existing raw values still parse and behave the same.
- Exact full-state source matching still behaves the same.
- Explicit regex mode matches the same way as legacy regex mode, but labels the intent.
- Selected-properties mode can replace all north-facing instances of one block without selecting every other property.
- Literal mode does not interpret regex metacharacters in names.
- Preview and execution agree on copied-world counts.

Implementation notes:

- `ChunkFilter.BlockReplaceSource` now carries an explicit source mode.
- Bare and quoted source strings remain `legacy-regex-name`.
- Source SNBT remains `exact-state`.
- `regex(...)`, `literal(...)`, and `props(...)` parse through `ReplaceBlocksField`.
- Builder-generated simple source IDs now serialize as `literal(...)`.
- Builder-generated catalog property sources serialize as `props(...)`.
- Dedicated source-mode UI controls and per-property subset checkboxes are still a possible UX follow-up.

### Phase 5A: Per-Rule Preview Counts

Status: implemented on 2026-06-26.

Goal: make later condition work testable before adding tile and spatial restrictions.

Why this moved before tile/Y/biome:

- Total matched blocks are not enough once rules have different source modes.
- Per-rule counts expose accidental regex, subset, tile, or Y-range overreach early.

Work:

- Done: preview data counts matches by rule.
- Done: preview dialog shows one row per rule with source mode, source text, target text, and matched block count.
- Done: aggregate scanned chunks, affected chunks, affected sections, matched blocks, tile estimates, and warnings remain visible.
- Done: overlapping source rules produce a visible overlap warning; aggregate matched blocks count positions once, while per-rule rows count every rule match.
- Done: preview remains non-mutating.

Success criteria:

- Met: a user can identify which rule will make changes.
- Met: deduplication/overlap behavior is documented in the dialog and tests.
- Met: unsupported chunks remain visible before execution.

### Phase 4E: Tile Entity Safety

Status: implemented on 2026-06-28. User-reported in-game copied-world validation completed on 2026-06-28.

Goal: expose tile/block entity safety controls without creating duplicate or stale block entities.

Recommended order:

- Done: source-side `tile(...)` and `no_tile(...)` wrappers let rules include only positions with existing block entities or exclude positions with existing block entities.
- Done: the builder exposes source tile eligibility as an additive source-side "Extra NBT" selector while preserving advanced text input.
- Done: the builder includes a Help button that opens a dialog explaining the Extra NBT options; keep that dialog extensible for future Builder help sections.
- Done: preview reflects tile additions, removals, and updates before mutation.
- Done: modern 1.18+ execution removes existing block entities at the replacement coordinates before adding target tile SNBT, preventing duplicate entries at the same coordinates in the covered path.
- Add target tile entity editing only after replacement-over-existing-tile behavior is fixed or clearly documented.

Success criteria:

- Met in UI/parser: users can choose whether tile entity blocks are eligible for replacement.
- Met in preview: preview estimates add/remove/update tile effects before mutation.
- Met in automated modern tests: tile-only, no-tile, tile-to-tile preview, and duplicate target tile cleanup are covered.
- Met in manual validation: user reported copied-world in-game testing completed for Phase 4E on 2026-06-28.

### Phase 4F-1: Y Range Restrictions

Status: implemented on 2026-07-07.

Goal: add the safest spatial condition first.

Recommended order:

- Done: add min/max Y controls to builder and internal rule representation.
- Done: apply the same Y logic in preview and execution.
- Done: show Y range in generated rule text and per-rule preview rows through `BlockReplaceSource.toString()`.
- Test against normal terrain and air replacement in tiny copied selections.

Success criteria:

- Met in automated modern-path tests: replacement affects only the requested Y range.
- Met in automated preview tests: per-rule and aggregate preview counts respect Y filtering.
- Still needs copied-world validation: preview counts should match a later run on a fresh copied world.
- Still needs copied-world validation: air replacement should not unexpectedly fill outside the requested vertical range.

### Phase 4F-2: Biome Restrictions

Status: implemented on 2026-07-08.

Goal: add biome-aware restrictions only after the Y-range path is stable.

Required design decision:

- Decided: biome matching is block-position aware. For each candidate block position, preview and execution read the biome palette entry for that position. In modern 1.18+ chunks the stored biome value covers a 4x4x4 block cell, so every block in the same biome cell shares the same biome condition result.

Success criteria:

- Met in UI/parser: Builder exposes a source Biome field and generated rules use `biome(<biome>[;<biome>...], source)`.
- Met in help/docs: Builder Help states the block-position/4x4x4 biome-cell granularity.
- Met in automated modern-path tests: preview and execution both restrict replacements to the matching biome cell.
- Still needs copied-world validation: test at least one boundary case near a biome transition and compare preview counts with execution on a fresh copy.

### Phase 4G: Presets

Goal: speed up common safe rules after matching modes and preview summaries are reliable.

Recommended presets:

- Air to block, with strong warnings.
- Fluids.
- Logs/leaves.
- Ores.
- Containers, gated by tile safety warnings.

Success criteria:

- Done: the Builder has a Preset row for Air to stone, Fluids to air, Logs/leaves to air, Ores to stone, and Containers with Extra NBT to air.
- Done: built-in presets fill visible From/To and source condition controls instead of adding hidden behavior; users can edit source mode text, target, Extra NBT mode, Y range, and biome fields before adding a rule.
- Done: air and container presets show warning text before adding a rule.
- Done: custom presets save the selected table rule when one is selected, otherwise a valid From/To draft, otherwise all table rules. Loading appends all non-duplicate preset rules without clearing the current table or draft. Presets can still be overwritten or deleted.
- Done: advanced text remains available.

### Phase 6: Release Hardening

Goal: make ReplaceBlocks safe enough for repeated use on copied worlds.

This is a release gate, not a place to begin testing. Each implementation phase above should add its own parser tests, preview checks, docs, and copied-world notes.

Release gates:

- Passed after the 2026-07-11 findings follow-up: compile and 58 automated tests covering parser/source modes, rule/preset restoration, catalog data, legacy fail-closed behavior, modern preview/execution parity, relight flags, and heightmap packing/writeback.
- Passed again on 2026-07-11: translation completeness and `build shadowJar`.
- Passed on 2026-07-11: Windows `jpackage` with Azul Zulu 21.0.11 JDK FX; the packaged MCA Selector 2.8 image opened independently of Gradle.
- Failed on `d17f0247`, fixed, focused rerun pending: Chinese/English completion, IME, popup resizing, property round-trip, preset precedence/append behavior, clean console, and final screenshots.
- Passed at file level on 2026-07-11 using disposable DataVersion 2860 and 4671 copies: preview hashes, ordinary/multiple rules, selection-only execution, state/waterlogged round-trip, tile add/remove/update, bounded air, Y + biome + tile composition, overlap counts, light invalidation, and heightmap shape.
- Passed: real 1.18/1.21 stored-biome boundary checks on disposable normal-terrain copies; preview hashes stayed unchanged, selected-biome matches reached zero after execution, and control-biome counts were unchanged.
- User game pass completed load/save/reload, state, container, heightmap, and log checks. A 1.21 stale-light result caused the execution path to write `isLightOn=0`; only the focused in-game relight rerun remains.
- Pending: final post-rerun documentation status and refresh of the already prepared local code/Wiki PR branches.

Any failed gate is fixed on the feature branch, receives the narrowest practical automated regression, and reruns both its focused checks and the final full gate. Phase 6 must not be marked complete while a required UI or copied-world gate remains blocked.

## Documentation Update Rule

When a future phase lands, update these docs in the same turn:

- `ONBOARDING.md`: current status and next task.
- `AGENTS.md`: current implementation status and constraints if they changed.
- `docs/ROADMAP.md`: phase status and sequencing.
- `docs/NEXT_DEVELOPMENT_REPLACE_BLOCKS.md`: design decisions and examples.
- `docs/DEV_NOTES_REPLACE_BLOCKS.md`: runtime behavior and risks.
- `docs/TEST_PLAN.md`: automated and manual validation.
- `docs/findings/ui_reference.md`: UI status if the builder changes.
- `docs/findings/replaceblocks_analysis.md`: parser/execution facts if semantics change.

## Context Notes

- Current UI already has a builder, advanced text preservation, validation messages, preview, and property dropdowns.
- Current builder generates `literal(...)` for simple source IDs and `props(...)` for catalog-backed source property rules.
- Current empty-builder From/To inputs start blank to avoid misleading placeholder behavior; empty dropdown clicks do not show the full catalog list.
- Current property dropdowns default to `all`/`全部`; source-side all properties generate `literal(...)`, and selected source properties generate `props(...)` with only the non-all properties.
- Current block suggestion completion has separate keyboard and mouse paths because editable JavaFX ComboBox selection is fragile when items or selection are changed synchronously during popup mouse handling.
- Current ReplaceBlocks field-row diagnostics wait for a short typing pause before showing valid/invalid feedback.
- Current preview is implemented for modern 1.18+ paths, reports unsupported older chunks instead of guessing, and shows per-rule counts plus overlap warnings.
- Current source-state support remains exact matching; selected-property subset matching is explicit through `props(...)`.
- Current catalog support exposes Java 1.21.9 block IDs, properties, allowed values, and defaults; the builder consumes it for dropdowns.
- `BlockRegistry` validates IDs but does not provide per-block property schema.
- Do not mutate real Minecraft worlds while developing or testing this feature.
