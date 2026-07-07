# ONBOARDING.md

This repository is a fork of MCA Selector. The active fork goal is to improve the existing ReplaceBlocks feature with a clearer JavaFX UI, richer replacement options, better validation/error messages, and safe preview/dry-run workflows.

Start here when a new Codex session has no prior chat context.

## How to Take Over

1. Read this file first.
2. Read `AGENTS.md` for repository rules and forbidden actions.
3. Read `docs/ROADMAP.md` to confirm the current phase.
4. Read only the directly relevant docs or source files for the task:
   - `docs/DEV_NOTES_REPLACE_BLOCKS.md`
   - `docs/findings/replaceblocks_analysis.md`
   - `docs/findings/ui_reference.md`
   - `docs/NEXT_DEVELOPMENT_REPLACE_BLOCKS.md`
   - `docs/TEST_PLAN.md`
5. Before editing, run:

```powershell
git status --short
```

Do not recursively read all of `src/`. Index first, then open only the files needed for the current question or phase.

## Current Status

Implemented:

- Phase 1: ReplaceBlocks rule builder UI in `ChangeNBTDialog`.
- Phase 2: ReplaceBlocks-specific validation diagnostics and error messages.
- Phase 3: non-mutating preview/dry-run for modern 1.18+ chunk formats.
- Phase 4: exact source block-state SNBT matching.
- Phase 4A: Java 1.21.9 block-state catalog data foundation.
- Phase 4B: property-aware rule builder UI using the Java 1.21.9 catalog.
- Gate A: source matching design for Phase 4C/4D is decided in `docs/NEXT_DEVELOPMENT_REPLACE_BLOCKS.md`.
- Phase 4C/4D: explicit source modes and selected-property matching using `regex(...)`, `literal(...)`, and `props(...)`.
- Phase 4E: tile/block entity source safety controls using `tile(...)` and `no_tile(...)`, with duplicate target tile cleanup, preview add/remove/update estimates, Extra NBT Builder labels, and copied-world in-game validation reported complete on 2026-06-28.
- Phase 5A: per-rule preview counts with source-mode rows and overlap warnings.
- Phase 4F-1: Y range restrictions using `y(min..max, source)`, with Builder min/max Y inputs, parser/diagnostic coverage, preview filtering, and modern 1.18+ execution filtering.
- UI polish: ReplaceBlocks field-row validation waits for a short typing pause, the default NBT Changer width shows the Builder button, empty Builder From/To inputs start blank, Builder validation stays quiet until user action creates a real diagnostic, and Builder From/To inputs have auto-opening A-Z filtered suggestions with blue match highlights, Tab/click completion, no empty-query full-list popup, pre-input helper text that hides after manual typing or selection, and a Help dialog for Builder-specific explanations.

Not implemented yet:

- Phase 4F-2: biome restrictions.
- Phase 4G: presets.
- Phase 6: copied-world test hardening and release prep.

## Next Recommended Task

Next best target: Phase 4F-2 design, biome restrictions.

Goal:

- Decide whether biome matching is block-position aware, section/palette aware, or chunk/selection aware before coding.
- Keep preview and execution granularity identical.
- Preserve Phase 4E tile eligibility, Phase 4F-1 Y filtering, and per-rule preview counts while adding biome filtering.
- Plan copied-world testing around at least one biome boundary case.

Current builder/UI manual validation checklist:

- A user can build `literal(minecraft:stone)=minecraft:dirt` through the UI.
- A user can select `minecraft:acacia_trapdoor` and choose properties such as `facing`, `half`, `open`, `powered`, and `waterlogged`.
- A no-property block such as `minecraft:blue_ice` works without showing property rows.
- Generated values parse through the existing `ReplaceBlocksField`.
- Unknown or modded IDs can still be entered manually.
- Existing advanced text workflows still work.
- Typing `oak` or `sto` in Builder From/To opens a scrollable A-Z filtered candidate list, highlights the typed substring in blue, and collapses after Tab or mouse-click completion without JavaFX selection errors.
- Opening an empty Builder From/To dropdown before typing should not show a full block list or position the popup above the input.
- The Builder helper text below the generated value is visible before manual From/To input and hides once the user types non-empty text into either field.
- The Builder source tile selector can generate `tile(literal(minecraft:chest))=...` and `no_tile(literal(minecraft:chest))=...`, and the generated values parse through the existing `ReplaceBlocksField`.
- The Builder source tile selector is labeled as `Extra NBT` in the UI, and its Help button explains the any/present/absent choices without closing the Builder.
- Builder property dropdowns default to `all`/`全部`; source-side `all` omits that property from `props(...)`, and all properties set to `all` generates `literal(...)`.
- Builder source min/max Y fields default to empty; filling either field wraps the source as `y(min..max, source)`, for example `y(-64..64, literal(minecraft:stone))=minecraft:dirt`.

## Important Files

Current UI and diagnostics:

- `src/main/java/net/querz/mcaselector/ui/dialog/ChangeNBTDialog.java`
- `src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderDialog.java`
- `src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksDiagnostics.java`
- `src/main/resources/style/component/change-nbt-dialog.css`
- `src/main/java/net/querz/mcaselector/text/Translation.java`

Parser and execution path:

- `src/main/java/net/querz/mcaselector/changer/fields/ReplaceBlocksField.java`
- `src/main/java/net/querz/mcaselector/changer/ChangeParser.java`
- `src/main/java/net/querz/mcaselector/changer/FieldType.java`
- `src/main/java/net/querz/mcaselector/io/job/FieldChanger.java`
- `src/main/java/net/querz/mcaselector/io/job/ReplaceBlocksPreviewer.java`
- `src/main/java/net/querz/mcaselector/io/mca/ChunkData.java`

Version and block-state internals:

- `src/main/java/net/querz/mcaselector/version/ChunkFilter.java`
- `src/main/java/net/querz/mcaselector/version/VersionHandler.java`
- `src/main/java/net/querz/mcaselector/version/java_1_18/ChunkFilter_21w43a.java`
- `src/main/java/net/querz/mcaselector/version/java_1_18/ChunkFilter_21w37a.java`
- `src/main/java/net/querz/mcaselector/version/mapping/blockstate/BlockStateCatalog.java`
- `src/main/resources/mapping/block_states/java_1_21_9.json`

Tests:

- `src/test/java/net/querz/mcaselector/changer/fields/ReplaceBlocksFieldTest.java`
- `src/test/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksDiagnosticsTest.java`
- `src/test/java/net/querz/mcaselector/version/java_1_18/ReplaceBlocksPreviewCountsTest.java`
- `src/test/java/net/querz/mcaselector/version/mapping/blockstate/BlockStateCatalogTest.java`

## Critical Behavior Facts

- ReplaceBlocks is a standard NBT Changer field, not a standalone tool.
- UI and CLI both flow through `ChangeParser -> ReplaceBlocksField -> FieldChanger -> ChunkData.applyFieldChanges -> VersionHandler -> ChunkFilter.Blocks.replaceBlocks`.
- The advanced text input must remain available.
- Existing source block-name matching uses Java regex semantics via `String.matches(...)`.
- Source block-state SNBT currently uses exact `CompoundTag` matching.
- Partial source property matching is implemented only through explicit `props(...)`.
- Source modes are `legacy-regex-name`, `regex-name`, `literal-name`, `exact-state`, and `selected-properties`.
- Source-side wrappers are `regex(...)`, `literal(...)`, and `props(...)`.
- Tile source wrappers are `tile(...)` for only positions with existing block entities and `no_tile(...)` for excluding positions with existing block entities.
- Y range source wrapper is `y(min..max, source)`; either boundary may be omitted, but at least one integer boundary is required.
- Target tile SNBT still uses the existing `target;{tile SNBT}` syntax; rich target tile NBT editing is not in the builder yet.
- Phase 4A/4B catalog data is UI/help data. The builder consumes it to generate `literal(...)` and `props(...)` text.
- `BlockRegistry` validates block IDs but does not provide per-block property schema.
- Modern colored wool blocks are separate IDs, for example `minecraft:yellow_wool=minecraft:blue_wool`, not a color property replacement.
- Preview must stay non-mutating: do not call `replaceBlocks(...)`, do not save regions, and do not enqueue save jobs.
- Per-rule preview counts are implemented and should be preserved before biome condition and preset work.

## Forbidden Actions

- Do not modify real Minecraft world saves.
- Do not run destructive commands against world data.
- Do not change Gradle build logic during ReplaceBlocks UI work.
- Do not rewrite unrelated UI or version code.
- Do not remove the advanced text workflow.
- Do not silently change source regex behavior.
- Do not silently change exact source-state matching into subset matching.
- Do not make broad full-repository source sweeps unless explicitly requested.
- Do not revert user changes or unrelated dirty worktree files.

## Validation Commands

Minimum after Java changes:

```powershell
.\gradlew.bat compileJava
```

When tests are relevant:

```powershell
.\gradlew.bat test
```

For the Phase 4A catalog test:

```powershell
.\gradlew.bat test --tests net.querz.mcaselector.version.mapping.blockstate.BlockStateCatalogTest
```

For ReplaceBlocks parser and diagnostics tests:

```powershell
.\gradlew.bat test --tests net.querz.mcaselector.changer.fields.ReplaceBlocksFieldTest --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksDiagnosticsTest
```

For JavaFX UI work:

```powershell
.\gradlew.bat run
```

Then manually inspect the relevant dialog. If `processResources` fails because a running MCA Selector window locks a file under `build/resources/main`, close the running JavaFX app and rerun.

For editable JavaFX ComboBox work, manually test both keyboard completion and mouse selection. Avoid synchronously replacing items or clearing selection from inside the ComboBox mouse-selection event path; this previously produced `ListViewBehavior` index errors.

## Dirty Worktree Rule

Do not assume every dirty file belongs to the current task. Always inspect with `git status --short` before editing, identify whether existing changes are user changes or prior task leftovers, and preserve unrelated work.

## Copied-World Testing Rule

Any test that mutates world data must use a disposable copied test world only. The preferred final test set is:

- one small Java 1.18+ world
- one small Java 1.21+ world
- normal blocks
- stateful blocks
- tile/block entity blocks
- air gaps
- waterlogged blocks
- heightmap-sensitive terrain

Do not claim world mutation behavior is safe until it has been tested on copied worlds and loaded in Minecraft.
