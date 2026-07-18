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
- Phase 4A: Java 1.21.9 block-state catalogue data foundation, expanded to bundled Java 1.18.2, 1.20.6, 1.21.9, 1.21.11, and 26.2 catalogues.
- Phase 4B: property-aware rule builder UI using the indexed catalogues, newest-by-default manual selection, and no automatic world-version/ID migration.
- Gate A: source matching design for Phase 4C/4D is decided in `docs/NEXT_DEVELOPMENT_REPLACE_BLOCKS.md`.
- Phase 4C/4D: explicit source modes and selected-property matching using `regex(...)`, `literal(...)`, and `props(...)`.
- Phase 4E: tile/block entity source safety controls using `tile(...)` and `no_tile(...)`, with duplicate target tile cleanup, preview add/remove/update estimates, Extra NBT Builder labels, and copied-world in-game validation reported complete on 2026-06-28.
- Phase 5A: per-rule preview counts with source-mode rows and overlap warnings.
- Phase 4F-1: Y range restrictions using `y(min..max, source)`, with Builder min/max Y inputs, parser/diagnostic coverage, preview filtering, and modern 1.18+ execution filtering.
- Phase 4F-2: biome restrictions using `biome(<biome>[;<biome>...], source)`, with Builder source biome input, parser/diagnostic coverage, preview filtering, and modern 1.18+ execution filtering. Matching is block-position aware using the candidate block's biome value; in modern chunks one stored biome value covers a 4x4x4 block cell.
- Phase 4G: Builder presets for Air to stone, Fluids to air, Logs/leaves to air, Ores to stone, and Containers with Extra NBT to air. Built-ins fill visible inputs and warnings. Custom save precedence is selected rule, valid draft, then all table rules; loading appends non-duplicate rules without replacing current work.
- UI polish: ReplaceBlocks field-row validation waits for a short typing pause, the default NBT Changer width shows the Builder button, empty Builder From/To inputs start blank, Builder validation stays quiet until user action creates a real diagnostic, and Builder From/To inputs have auto-opening A-Z filtered suggestions with blue match highlights, Tab/click completion, boundary-only keyboard scrolling, explicit empty-arrow full-catalog expansion, stronger Builder-only dropdown highlights, pre-input helper text that hides after manual typing or selection, Builder-local Preview beside Help, and a Help dialog for Builder-specific explanations. The Builder toolbar is compact, the catalogue selector shows a short Java version with the full DataVersion in its tooltip, source-only conditions share a full-width restrictions strip, empty rules/results use compact localized placeholders, populated areas expand naturally, the Add Rule action keeps the shared button style, generated values wrap in a monospace result area, and block/biome autocomplete popups stay within the Builder window horizontally while preserving their vertical anchor.
- B-class hardening and the five-catalogue Builder are implemented. Empty catalogue switches are direct; a non-empty switch asks first, with Cancel preserving all work and Confirm selecting the new catalogue then fully resetting Builder state. Presets remain versionless; switching never deletes saved presets, and determinable exact custom-preset IDs outside the active catalogue produce non-blocking advisories.

Completed release validation:

- Phase 6 is complete. Automated tests, translation completeness, `build shadowJar`, Zulu JDK FX packaging, DataVersion 2860/4671 and 26.3 snapshot checks, real biome boundaries, game load/save/reload, game-log review, adjacent-ring relight, copied-world game loading, and the fresh-Builder From/To/Biome popup-anchor rerun all have evidence.
- The focused five-catalogue `UI-CATALOG` rerun passed by user report on 2026-07-17: empty switching was direct, Cancel preserved all work, Confirm fully reset the Builder under the new catalogue, saved presets remained available, exact out-of-catalogue preset IDs warned without blocking, and regex sources were not misclassified.

## Next Recommended Task

Next best target: prepare the upstream contribution using the logical PR boundaries recorded in `docs/RELEASE_REVIEW_REPLACE_BLOCKS.md`.

Goal:

- Preserve parser/diagnostic, catalogue/Builder, runtime-safety/performance, and UI/docs/test boundaries so upstream can review or revert them independently.
- Carry the automated, packaging, copied-world, game, and `UI-CATALOG` evidence with the relevant PR descriptions.
- Do not reopen automatic world-version selection or cross-version ID migration as part of upstream preparation.

Current builder/UI manual validation checklist:

- A user can build `literal(minecraft:stone)=minecraft:dirt` through the UI.
- A user can select `minecraft:acacia_trapdoor` and choose properties such as `facing`, `half`, `open`, `powered`, and `waterlogged`.
- A no-property block such as `minecraft:blue_ice` works without showing property rows.
- Generated values parse through the existing `ReplaceBlocksField`.
- Unknown or modded IDs can still be entered manually.
- Existing advanced text workflows still work.
- Typing `oak` or `sto` in Builder From/To opens a scrollable A-Z filtered candidate list, highlights the typed substring in blue, and collapses after Tab or mouse-click completion without JavaFX selection errors.
- Clicking an empty Builder From/To dropdown arrow should show the complete A-Z block list without causing an automatic popup when the Builder first opens.
- The Builder helper text below the generated value is visible before manual From/To input and hides once the user types non-empty text into either field.
- The Builder source tile selector can generate `tile(literal(minecraft:chest))=...` and `no_tile(literal(minecraft:chest))=...`, and the generated values parse through the existing `ReplaceBlocksField`.
- The Builder source tile selector is labeled as `Extra NBT` in the UI, and its Help button explains the any/present/absent choices without closing the Builder.
- Builder property dropdowns default to `all`/`全部`; source-side `all` omits that property from `props(...)`, and all properties set to `all` generates `literal(...)`.
- Builder source min/max Y fields default to empty; filling either field wraps the source as `y(min..max, source)`, for example `y(-64..64, literal(minecraft:stone))=minecraft:dirt`.
- Builder source Biome input is searchable like the From/To block selectors. It suggests known vanilla biome IDs, completes the current semicolon-separated biome token with Tab or mouse click, and shows the complete A-Z biome catalog only when the user explicitly clicks the empty dropdown arrow.
- Builder presets fill visible, editable From/To and source condition controls instead of adding hidden behavior. Air and container presets show warning text before the user adds a rule.
- Builder custom presets save one selected rule first, otherwise a valid draft, otherwise all table rules. Presets remain versionless ReplaceBlocks text; loading appends non-duplicate rules and preserves current table rules and draft input, with non-blocking current-catalogue advisories for determinable exact IDs.
- The catalogue selector offers Java 1.18.2, 1.20.6, 1.21.9, 1.21.11, and 26.2 and defaults to the newest. Empty switches are direct. For a non-empty Builder, Cancel preserves all work; Confirm selects the new catalogue and fully resets the Builder without deleting saved presets.
- The Builder layout keeps preset/catalogue actions in a compact top toolbar, places Extra NBT/Y/biome source restrictions in one full-width optional strip below From/To, keeps the Min Y/Max Y labels visible while their fields absorb remaining width, shows an empty-state prompt instead of a large blank rules table, and expands the rules table/result area only when content exists. Add Rule uses the same shared button treatment as the surrounding controls; disabled preset actions remain readable.
- Builder From/To/Biome autocomplete popups preserve the field's left edge when there is enough room, cap their width at the Builder's right boundary when needed, and shift left only when the field itself is too close to that boundary. Ordinary preset, property, and Extra NBT dropdowns keep their native behavior.
- Builder Preview is next to Help in the Builder button bar. It uses the generated Builder value, respects the selection-only state captured when the Builder was opened, and must remain non-mutating.

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
- Biome source wrapper is `biome(<biome>[;<biome>...], source)`. Biomes may omit the `minecraft:` namespace in Builder/parser input, but serialized values use full IDs. Modern preview and execution use block-position-aware biome matching at the stored 4x4x4 biome-cell granularity.
- Target tile SNBT still uses the existing `target;{tile SNBT}` syntax; rich target tile NBT editing is not in the builder yet.
- Phase 4A/4B catalogue data is UI/help data. The builder consumes the manually selected catalogue to generate `literal(...)` and `props(...)` text; it does not detect world versions or migrate IDs.
- `BlockRegistry` validates block IDs but does not provide per-block property schema.
- Modern colored wool blocks are separate IDs, for example `minecraft:yellow_wool=minecraft:blue_wool`, not a color property replacement.
- Preview must stay non-mutating: do not call `replaceBlocks(...)`, do not save regions, and do not enqueue save jobs.
- Per-rule preview counts are implemented and should be preserved before preset work.

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
