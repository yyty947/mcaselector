# Task 3 report: catalogue-switch localization and documentation

Date: 2026-07-17
Branch: `feature/replace-blocks-ui`

## Scope completed

- Added dedicated catalogue-switch confirmation title/message `Translation` keys.
- Bound `confirmCatalogSwitch(...)` to the dedicated title and formatted message with the old/new Java/DataVersion labels.
- Updated the visible catalogue note with the no-ID-conversion and confirm-then-reset boundaries.
- Added native English and Simplified Chinese title/message/note text. All other 17 existing locales use the exact English fallback, with key ordering preserved.
- Extended the JavaFX Builder test to inspect the real Alert window and assert its dedicated title, formatted message, and both catalogue labels.
- Aligned all required active documents and the directly stale `docs/findings/ui_reference.md` with the five bundled catalogues and reset behavior.
- Preserved the controller's corrected `run --args="--mode printMissingTranslations"` plan command and expanded the Task 3 file list to match the active documentation scope.

No parser, preview/execution, CLI, Gradle/dependency, or world-data files were changed.

## RED evidence

After adding the enum keys and real Alert/note expectations, before adding locale entries or production binding:

```text
.\gradlew.bat test --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksRuleBuilderModelTest
49 tests completed, 2 failed
47 passed
```

Expected failures:

- `confirmingNonEmptyCatalogSwitchUsesNewCatalogAndCompletelyResetsBuilder`: the real Alert still used the generic Builder title and catalogue note body.
- `catalogControlExplainsThatItDoesNotMigrateIds`: the old note did not state that a confirmed non-empty switch clears Builder content.

The other 47 tests passed, so the RED was not a JavaFX startup or fixture failure.

## GREEN evidence

- Fresh focused test: `.\gradlew.bat test --rerun-tasks --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksRuleBuilderModelTest` -> 49/49 passed; four tasks executed.
- Compile: `.\gradlew.bat compileJava` -> success.
- Translation completeness: `.\gradlew.bat run --args="--mode printMissingTranslations"` -> exit 0 and no missing-key lines.
- Locale audit: 19/19 locale files contain adjacent ordered catalogue title/message/note keys; all 17 fallback locales exactly match English.
- Active-document audit: no stale current-behavior claims for `latestJava()`, single/latest-only catalogue support, future multi-catalogue selection, preserve-on-switch, or B-class-as-next-task outside the historical design/plan directives.
- `git diff --check` -> passed.

## Documentation state

Current active docs now consistently record:

- Bundled Java 1.18.2, 1.20.6, 1.21.9, 1.21.11, and 26.2 catalogues.
- Newest-by-default manual selection; no automatic world-version selection or cross-version ID migration.
- Direct empty switches; confirmation before non-empty switches; Cancel preserves old catalogue and all work; Confirm selects the new catalogue and fully resets Builder state.
- Versionless presets; switching clears active preset selection/content but never deletes saved presets; determinable exact custom-preset IDs outside the current catalogue warn without blocking, while regex is not guessed.
- Historical Phase 6/B-class evidence remains historical. The focused `UI-CATALOG` user-run row is **Pending**.

## Remaining concerns / controller gate

- The focused catalogue-switch UI acceptance has not been run by the user and must remain **Pending** until that evidence exists.
- The full test suite and `build shadowJar` were intentionally left for the controller's final gate, as allowed by the Task 3 brief.
