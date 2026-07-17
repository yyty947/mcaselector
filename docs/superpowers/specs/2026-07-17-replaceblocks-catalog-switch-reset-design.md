# ReplaceBlocks Builder Catalogue Switch Reset Design

## Goal

Make manual block-catalogue switching deterministic: a non-empty Builder asks for confirmation and, when confirmed, clears all Builder work instead of attempting a partial cross-version migration.

## Catalogue switch behavior

- Selecting the already active catalogue is a no-op.
- An empty Builder switches catalogue immediately.
- A non-empty Builder shows a catalogue-specific confirmation before the selected catalogue or any Builder state changes.
- Cancelling restores the old selector value and preserves the draft, properties, rules, selections, generated text, validation message, and saved presets.
- Confirming selects the requested catalogue and resets the Builder to the initial empty state under that catalogue. The newly selected catalogue remains active.
- The switch never renames, converts, or validates chunk block IDs and does not change parser or execution behavior.

Builder content means any rule or meaningful draft state: From/To text, a non-default property choice, non-default Extra NBT mode, Y limit, or biome. Merely selecting a preset without applying it is not content, but every successful catalogue switch clears the preset selection.

## Complete reset

A successful switch must:

- close From, To, biome, property, Extra NBT, and preset popups;
- invalidate pending autocomplete updates and clear transient explicit-catalog/highlight state;
- clear From and To text, dynamic properties, Extra NBT, Y limits, and biome;
- restore Extra NBT to `ANY`;
- clear all rule rows and table selection;
- clear the selected preset without deleting built-in or saved presets;
- clear generated ReplaceBlocks text and validation/status feedback;
- disable OK and Preview as on a newly opened empty Builder.

## Presets

Presets remain versionless parser-compatible ReplaceBlocks text. Neither built-in nor custom presets gain a DataVersion field. This preserves regex, modded/future IDs, advanced SNBT, and the existing global configuration format.

Applying a custom preset remains non-blocking. For newly appended rules whose exact source or target ID can be determined, the Builder checks the currently selected catalogue and shows the existing advisory unknown-source/unknown-target warning for the first missing ID. Regex or otherwise non-exact sources are not guessed. Unknown IDs remain accepted.

## Localization

Add catalogue-switch confirmation title and message keys. Chinese and English receive native text; other existing locales receive the established English fallback so translation completeness remains intact. Reuse the existing styled confirmation-dialog construction, not the window-close wording.

The visible catalogue note must explain both boundaries: the catalogue affects suggestions/properties rather than ID conversion, and switching a non-empty Builder clears its content after confirmation.

## Tests and documentation

Automated JavaFX/model coverage must prove:

- empty switches do not request confirmation;
- non-empty cancelled switches preserve the old catalogue and every state;
- confirmed switches use the new catalogue and fully reset all state, including popups, preset selection, Extra NBT, properties, rules, result, and validation;
- the re-entrant selector rollback cannot trigger a second switch;
- versionless custom presets remain accepted and warn for determinable IDs outside the selected catalogue.

Replace the previous documentation claim that catalogue changes preserve drafts/rules. Update the catalogue inventory to Java 1.18.2, 1.20.6, 1.21.9, 1.21.11, and 26.2. Historical release evidence may remain labelled as historical. The focused user-run manual test passed on 2026-07-17, closing the catalogue-switch UX gate.

## Out of scope

- Automatic world-version detection.
- Cross-version ID mappings or migrations.
- Parser, preview, execution, CLI, world-data, Gradle, or JavaFX dependency changes.
- Adding a version field to saved presets.
