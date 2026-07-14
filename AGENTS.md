# AGENTS.md

This repository is a fork of MCA Selector. Current fork goal: improve the existing ReplaceBlocks feature with a clearer JavaFX UI, more configurable replacement options, better validation/error messages, and eventual preview/dry-run support.

## Current working mode

- Read code on demand.
- Index first, then open only directly relevant files or methods.
- Do not recursively read all of `src/`.
- Do not perform broad full-repository source analysis unless the task explicitly requires it.
- Summarize findings after each related group of files before going deeper.

## Important files for ReplaceBlocks

- `src/main/java/net/querz/mcaselector/changer/fields/ReplaceBlocksField.java`
- `src/main/java/net/querz/mcaselector/changer/ChangeParser.java`
- `src/main/java/net/querz/mcaselector/changer/FieldType.java`
- `src/main/java/net/querz/mcaselector/version/ChunkFilter.java`
- `src/main/java/net/querz/mcaselector/version/VersionHandler.java`
- `src/main/java/net/querz/mcaselector/version/java_1_18/ChunkFilter_21w43a.java`
- `src/main/java/net/querz/mcaselector/version/java_1_18/ChunkFilter_21w37a.java`
- `src/main/java/net/querz/mcaselector/ui/dialog/ChangeNBTDialog.java`
- `src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderDialog.java`
- `src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksDiagnostics.java`
- `src/main/java/net/querz/mcaselector/ui/DialogHelper.java`
- `src/main/java/net/querz/mcaselector/io/job/FieldChanger.java`
- `src/main/java/net/querz/mcaselector/io/job/ReplaceBlocksPreviewer.java`
- `src/main/java/net/querz/mcaselector/io/mca/ChunkData.java`
- `src/main/java/net/querz/mcaselector/version/mapping/blockstate/BlockStateCatalog.java`
- `src/main/resources/mapping/block_states/java_1_21_9.json`

## Documentation to read first

- `ONBOARDING.md`
- `docs/DEV_NOTES_REPLACE_BLOCKS.md`
- `docs/findings/replaceblocks_analysis.md`
- `docs/findings/ui_reference.md`
- `docs/ROADMAP.md`
- `docs/NEXT_DEVELOPMENT_REPLACE_BLOCKS.md`
- `docs/TEST_PLAN.md`

## Forbidden unless explicitly requested

- Do not modify real Minecraft world saves.
- Do not run destructive commands against world data.
- Do not modify Gradle build logic during ReplaceBlocks UI work.
- Do not rewrite unrelated UI or version code.
- Do not change CLI syntax as part of a UI-only task.
- Do not change `ReplaceBlocksField` parsing in phase 1.
- Do not change version-specific `replaceBlocks(...)` behavior in phase 1.
- Do not remove or revert user changes.

## Before modifying code

- Run `git status --short`.
- Identify whether existing changes are user changes.
- Read only the files needed for the current task.
- Reconfirm the current phase from `docs/ROADMAP.md`.
- For ReplaceBlocks UI phase 1, confirm the intended behavior still round-trips through the existing ReplaceBlocks text format.
- If tests require world data, create or use only disposable copied test worlds.

## After modifying code

Minimum validation for Java changes:

```powershell
.\gradlew.bat compileJava
```

When behavior changes or tests exist:

```powershell
.\gradlew.bat test
```

For JavaFX UI changes:

```powershell
.\gradlew.bat run
```

Then manually inspect the relevant dialog. Do not claim world mutation behavior is safe unless it was tested on copied worlds.

## ReplaceBlocks-specific constraints

- Preserve existing `ReplaceBlocksField.parseNewValue(...)` behavior unless the task is explicitly about parser changes.
- Preserve `ReplaceBlocksField.change(...)` dispatch through `VersionHandler`.
- Preserve current advanced text input.
- A rule-builder UI should generate the current ReplaceBlocks value string and feed it through existing validation.
- Treat source name values carefully because execution uses Java regex matching.
- Source block state SNBT uses exact `CompoundTag` matching; do not silently change it to subset matching without updating parser, preview, tests, and docs.
- Phase 4A/4B block-state catalog data is UI/help data only; the builder may generate text from it, but do not treat it as a parser or replacement behavior change.
- Phase 4C/4D source modes are implemented; do not change `regex(...)`, `literal(...)`, or `props(...)` semantics without updating parser tests, preview expectations, and docs together.
- Keep legacy bare/quoted sources as regex compatibility syntax.
- Keep bare source SNBT as exact-state matching.
- Phase 4F-1 Y range restrictions are implemented through `y(min..max, source)`; do not change that syntax or matching semantics without updating parser tests, preview expectations, copied-world notes, and docs together.
- Phase 4F-2 biome restrictions are implemented through `biome(<biome>[;<biome>...], source)`; matching is block-position aware at modern 1.18+ 4x4x4 biome-cell granularity. Do not change that syntax or granularity without updating parser tests, preview expectations, copied-world notes, and docs together.
- Preserve per-rule preview counts before piling on preset conditions.
- Warn about air replacement because it can create missing sections.
- Keep preview/dry-run paths non-mutating: do not call `replaceBlocks(...)`, do not save regions, and do not enqueue save jobs from preview.
- Verify tile/block entity replacement for duplicate entities before expanding tile UI support.
- Verify 1.18+ and 1.21+ heightmap behavior on copied worlds before relying on it.

## Current implementation status

- Phase 1 rule builder is implemented.
- Phase 2 validation diagnostics are implemented.
- Phase 3 preview/dry-run is implemented for modern 1.18+ chunk formats.
- Phase 4 exact source block-state matching is implemented.
- Phase 4A Java 1.21.9 block-state catalog foundation is implemented.
- Phase 4B property-aware rule builder UI is implemented.
- Gate A source matching design is completed.
- Phase 4C/4D explicit source modes are implemented.
- Phase 4E tile/block entity source safety controls are implemented with `tile(...)` and `no_tile(...)`, clearer Extra NBT Builder labels/help, preview add/remove/update estimates, duplicate target tile cleanup in modern 1.18+ paths, and user-reported copied-world in-game validation.
- Phase 5A per-rule preview counts are implemented.
- Phase 4F-1 Y range restrictions are implemented with Builder min/max Y controls, parser/diagnostic support, preview filtering, and modern 1.18+ execution filtering.
- Phase 4F-2 biome restrictions are implemented with Builder source biome input, parser/diagnostic support, preview filtering, and modern 1.18+ execution filtering.
- Phase 4G presets are implemented as visible Builder input fillers for common replacements, with warning text for air and container/data-block cases.
- ReplaceBlocks UI polish is implemented: debounced field-row diagnostics, visible default Builder button, and addable empty-builder example defaults.
- Phase 6 release hardening is complete, including semantic preset normalization, Builder restoration, Change/Force parity, selection relight-ring expansion, pre-1.18 fail-closed execution, modern preview/execution parity, heightmaps, and Builder popup navigation regressions. Translation completeness and `build shadowJar` pass.
- Packaging, DataVersion 2860/4671 and 26.3 snapshot checks, real biome boundaries, Minecraft load/save/reload, game-log review, the 1.21 adjacent-ring relight rerun, the 26.3 copied-world game load, and the final Builder dropdown/first-popup UX rerun all have evidence.
