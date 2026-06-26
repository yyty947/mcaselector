# ReplaceBlocks Roadmap

Date: 2026-06-04
Last updated: 2026-06-26

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
- Phase 5A: per-rule preview counts, source-mode rows, and overlap warning in the ReplaceBlocks preview dialog.
- UI polish: ReplaceBlocks field-row diagnostics are debounced, the main dialog defaults wide enough to show `Builder`, and the empty builder's default example can be added directly.

Still not implemented:

- Tile entity include/exclude controls.
- Y range restrictions.
- Biome restrictions.
- Presets.
- Copied-world release hardening.

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
- New explicit source wrappers are `regex(...)`, `literal(...)`, and `props(...)`.
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

Why this moves before tile/Y/biome:

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

Goal: expose tile/block entity safety controls without creating duplicate or stale block entities.

Recommended order:

- First verify current duplicate behavior on copied worlds.
- Add include/exclude tile-entity-source controls before adding tile NBT editing.
- Reflect tile eligibility and estimated add/remove/update counts in preview.
- Add target tile entity editing only after replacement-over-existing-tile behavior is fixed or clearly documented.

Success criteria:

- Users can choose whether tile entity blocks are eligible for replacement.
- Preview estimates tile effects before mutation.
- Copied-world tests cover tile-to-non-tile, non-tile-to-tile, and tile-to-tile replacement.

### Phase 4F-1: Y Range Restrictions

Goal: add the safest spatial condition first.

Recommended order:

- Add min/max Y controls to builder and internal rule representation.
- Apply the same Y logic in preview and execution.
- Show Y range in generated summaries.
- Test against normal terrain and air replacement in tiny copied selections.

Success criteria:

- Replacement affects only the requested Y range.
- Preview counts match a later run on a fresh copied world.
- Air replacement does not unexpectedly fill outside the requested vertical range.

### Phase 4F-2: Biome Restrictions

Goal: add biome-aware restrictions only after the Y-range path is stable.

Required design decision:

- Document whether biome matching is block-position aware, section/palette aware, or chunk/selection aware before implementation.

Success criteria:

- The UI states the biome matching granularity.
- Preview and execution use the same granularity.
- Copied-world tests cover at least one boundary case near a biome transition.

### Phase 4G: Presets

Goal: speed up common safe rules after matching modes and preview summaries are reliable.

Recommended presets:

- Air to block, with strong warnings.
- Fluids.
- Logs/leaves.
- Ores.
- Containers, gated by tile safety warnings.

Success criteria:

- Presets generate visible, editable rules.
- Presets do not hide source mode, tile behavior, Y range, or biome conditions.
- Advanced text remains available.

### Phase 6: Release Hardening

Goal: make ReplaceBlocks safe enough for repeated use on copied worlds.

This is a release gate, not a place to begin testing. Each implementation phase above should add its own parser tests, preview checks, docs, and copied-world notes.

Release checklist:

- `.\gradlew.bat compileJava`
- Relevant unit tests, including parser/source-mode tests and catalog tests.
- JavaFX manual inspection for changed dialogs.
- Copied-world tests for 1.18+ and 1.21+.
- Normal blocks, stateful blocks, tile entity blocks, air, waterlogged blocks, and heightmap-sensitive terrain.
- Preview-vs-execution count comparison on fresh copied worlds.
- Minecraft world-load validation and log check.
- Docs updated in the same change set as behavior changes.

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
- Current empty-builder defaults are real `minecraft:stone` -> `minecraft:dirt` inputs, so the example can be added as a valid rule immediately.
- Current ReplaceBlocks field-row diagnostics wait for a short typing pause before showing valid/invalid feedback.
- Current preview is implemented for modern 1.18+ paths, reports unsupported older chunks instead of guessing, and shows per-rule counts plus overlap warnings.
- Current source-state support remains exact matching; selected-property subset matching is explicit through `props(...)`.
- Current catalog support exposes Java 1.21.9 block IDs, properties, allowed values, and defaults; the builder consumes it for dropdowns.
- `BlockRegistry` validates IDs but does not provide per-block property schema.
- Do not mutate real Minecraft worlds while developing or testing this feature.
