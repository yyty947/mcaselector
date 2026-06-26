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
- Phase 4 partial: exact source block-state SNBT matching.
- Phase 4A: Java 1.21.9 block-state catalog data foundation.
- Phase 4B: property-aware rule builder UI using the Java 1.21.9 catalog.
- Gate A: source matching design for Phase 4C/4D is decided in `docs/NEXT_DEVELOPMENT_REPLACE_BLOCKS.md`.
- Phase 4C/4D: explicit source modes and selected-property matching using `regex(...)`, `literal(...)`, and `props(...)`.
- Phase 5A: per-rule preview counts with source-mode rows and overlap warnings.
- UI polish: ReplaceBlocks field-row validation waits for a short typing pause, the default NBT Changer width shows the Builder button, and the empty builder's default example can be added directly.

Not implemented yet:

- Phase 4E: tile entity safety controls.
- Phase 4F: Y range and biome restrictions.
- Phase 5 follow-ups: richer preview UX beyond per-rule counts.
- Phase 6: copied-world test hardening and release prep.

## Next Recommended Task

Next best target: Phase 4E, tile entity source safety controls.

Goal:

- Verify current duplicate block-entity behavior on copied worlds.
- Add include/exclude controls for source blocks that already have tile/block entities.
- Keep target tile NBT editing out of scope until duplicate/stale block entity behavior is fixed or clearly guarded.
- Reflect tile eligibility and estimated add/remove/update counts in preview before mutation.
- Keep preview and execution behavior aligned.

Completed Phase 4B manual validation checklist:

- A user can build `literal(minecraft:stone)=minecraft:dirt` through the UI.
- A user can select `minecraft:acacia_trapdoor` and choose properties such as `facing`, `half`, `open`, `powered`, and `waterlogged`.
- A no-property block such as `minecraft:blue_ice` works without showing property rows.
- Generated values parse through the existing `ReplaceBlocksField`.
- Unknown or modded IDs can still be entered manually.
- Existing advanced text workflows still work.

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
- Phase 4A/4B catalog data is UI/help data. The builder consumes it to generate `literal(...)` and `props(...)` text.
- `BlockRegistry` validates block IDs but does not provide per-block property schema.
- Modern colored wool blocks are separate IDs, for example `minecraft:yellow_wool=minecraft:blue_wool`, not a color property replacement.
- Preview must stay non-mutating: do not call `replaceBlocks(...)`, do not save regions, and do not enqueue save jobs.
- Per-rule preview counts are implemented and should be preserved before tile/Y/biome condition work.

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

## Current Dirty Worktree Note

At the time this onboarding file was created, the worktree already contained many ReplaceBlocks phase changes plus pre-existing Gradle wrapper/property changes. Do not assume every dirty file is from the current task. Always inspect with `git status --short` and preserve unrelated user changes.

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
