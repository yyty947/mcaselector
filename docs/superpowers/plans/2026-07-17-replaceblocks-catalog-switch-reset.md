# ReplaceBlocks Builder Catalogue Switch Reset Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make catalogue changes either preserve the entire Builder on cancel or reset it completely on confirmation, while keeping presets versionless and catalogue warnings advisory.

**Architecture:** Keep catalogue switching as Dialog orchestration and reuse `BlockInput.clear()` for field teardown. Add a small re-entry guard around the selector listener, a single dialog-level reset path, and a compatibility check for exact IDs appended by custom presets. Do not add a migration model, custom Skin, or persistence change.

**Tech Stack:** Java 21, JavaFX 21, JUnit 5, Gradle, MCA Selector translation text resources.

## Global Constraints

- Work on `feature/replace-blocks-ui` and preserve the existing uncommitted multi-catalogue resources and UI note work.
- Catalogue data remains UI/help data and must not change parser, preview, execution, CLI, or world mutation behavior.
- Presets remain `name + ReplaceBlocks text`; do not add DataVersion or change global-config serialization.
- Unknown future/modded IDs remain accepted with non-blocking warnings.
- Do not modify Gradle build logic or JavaFX dependencies.
- UI-dense acceptance is performed by the user after automated verification.

---

### Task 1: Catalogue confirmation and complete reset

**Files:**
- Modify: `src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderDialog.java`
- Test: `src/test/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderModelTest.java`

**Interfaces:**
- Consumes: `hasBuilderContent()`, `BlockInput.clear()`, `updateResult()`, `ReplaceBlocksCatalogModel.select(String)`.
- Produces: one guarded catalogue-change request path and one complete Builder reset path used only after a successful switch.

- [ ] **Step 1: Write failing JavaFX regression tests**

Add focused tests that construct a dialog with two catalogues or inject the selected catalogue through the existing model, then assert these behaviors separately:

```java
assertFalse(requestOnEmpty.confirmationRequested());
assertSame(newCatalog, requestOnEmpty.selected());

assertTrue(cancelled.confirmationRequested());
assertSame(oldCatalog, cancelled.selected());
assertEquals(originalDraft, cancelled.draft());

assertTrue(confirmed.confirmationRequested());
assertSame(newCatalog, confirmed.selected());
assertTrue(confirmed.ruleItems().isEmpty());
assertEquals("", confirmed.fromText());
assertEquals("", confirmed.toText());
assertNull(confirmed.selectedPreset());
assertEquals("", confirmed.result());
```

The confirmed case must populate From/To, a specific property, Extra NBT, Y, biome, at least one rule, table selection, preset selection, validation text, and visible/transient popup state before switching. The cancelled case must verify the same values remain unchanged.

- [ ] **Step 2: Run the focused tests and verify RED**

Run:

```powershell
.\gradlew.bat test --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksRuleBuilderModelTest
```

Expected: the new tests fail because catalogue changes neither confirm nor fully reset current Builder state.

- [ ] **Step 3: Implement guarded switching and reset**

In `createCatalogControl()`, replace the direct listener body with a guarded request method. The request must check content before changing `catalogModel`, restore the previous selector value under the guard on cancel, and on success select the new model, update `blockNames`, update each `BlockInput` catalogue, then run a full reset.

The reset must perform the equivalent of:

```java
presets.hide();
presets.getSelectionModel().clearSelection();
rules.getSelectionModel().clearSelection();
ruleItems.clear();
from.clear();
to.clear();
updateResult();
```

Extend `BlockInput.clear()` so it hides all owned ComboBoxes, hides dynamic property ComboBoxes before removal, increments pending suggestion revisions, clears explicit-catalog flags and highlight indices, clears popup pseudo-state, restores `SourceTileMode.ANY`, and rebuilds empty suggestions against the newly selected catalogue.

Expand meaningful-content detection so a non-default Extra NBT choice and non-default property choice cannot bypass confirmation. A selected-but-unapplied preset alone must not request confirmation.

- [ ] **Step 4: Run the focused tests and verify GREEN**

Run the same focused test command. Expected: every new catalogue switch/reset test passes with no JavaFX exception.

### Task 2: Versionless preset compatibility warning

**Files:**
- Modify: `src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderDialog.java`
- Test: `src/test/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderModelTest.java`

**Interfaces:**
- Consumes: `parseEditableRule(Rule)`, selected `BlockStateCatalog`, existing source/target unknown translations.
- Produces: a non-blocking diagnostic for the first determinable exact ID outside the selected catalogue among newly appended custom-preset rules.

- [ ] **Step 1: Write failing compatibility tests**

Cover an exact unknown target, an exact unknown literal/property source, and a regex source:

```java
assertEquals(UNKNOWN_TARGET, firstPresetCompatibilityIssue(exactUnknownTarget));
assertEquals(UNKNOWN_SOURCE, firstPresetCompatibilityIssue(exactUnknownSource));
assertNull(firstPresetCompatibilityIssue(regexOnlySource));
```

Also assert that rules are appended despite the warning and that the persisted preset record remains `name + value` only.

- [ ] **Step 2: Run focused tests and verify RED**

Run the Builder model and preset repository test classes. Expected: the new custom-preset warning assertions fail because custom presets currently append without catalogue diagnostics.

- [ ] **Step 3: Implement minimal advisory checking**

Collect only newly appended custom-preset rules. Parse each rule, check target `NAME`/`STATE` and source exact-name/selected-properties types against the selected catalogue, skip regex and unresolvable advanced forms, prefer the first unknown target warning over the first unknown source warning, and call `showDiagnostic(...)` after `updateResult()`. Never reject or remove the appended rule.

- [ ] **Step 4: Run focused tests and verify GREEN**

Run:

```powershell
.\gradlew.bat test --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksRuleBuilderModelTest --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksPresetRepositoryTest
```

Expected: both suites pass.

### Task 3: Localization and documentation alignment

**Files:**
- Modify: `src/main/java/net/querz/mcaselector/text/Translation.java`
- Modify: `src/main/resources/lang/*.txt`
- Modify: `docs/REPLACE_BLOCKS.md`
- Modify: `docs/DEV_NOTES_REPLACE_BLOCKS.md`
- Modify: `docs/ROADMAP.md`
- Modify: `docs/TEST_PLAN.md`
- Modify: `ONBOARDING.md`

**Interfaces:**
- Consumes: final Task 1 and Task 2 behavior.
- Produces: complete localized confirmation text and current multi-catalogue/manual-test documentation.

- [ ] **Step 1: Add failing translation/document assertions**

Add the catalogue-switch title/message enum keys and run the translation completeness task before resource entries exist. Expected: it reports the missing keys. Add or update a source assertion so the visible catalogue note contains both the no-conversion and reset-on-confirmation concepts.

- [ ] **Step 2: Add localized resources**

Use native Chinese and English text. Add the English fallback to every other existing locale file, matching the repository's B-class translation policy. Bind the new confirmation alert title and message through `Translation` properties/formatting.

- [ ] **Step 3: Replace stale documentation**

Remove the claim that switching catalogues preserves drafts/rules. Document empty direct switch, non-empty confirmation, cancel preservation, confirmed full reset, versionless presets, non-blocking custom-preset warnings, and the five bundled catalogues. Mark the focused switch-reset UI rerun pending rather than reusing the old pass result.

- [ ] **Step 4: Run automated verification**

Run:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksRuleBuilderModelTest
.\gradlew.bat test
.\gradlew.bat printMissingTranslations
.\gradlew.bat build shadowJar
```

Expected: every command exits 0, all tests pass, translation output contains no missing key, and `shadowJar` completes successfully.

### Task 4: User-run UI acceptance and commit

**Files:**
- Modify after user evidence: `docs/TEST_PLAN.md`

**Interfaces:**
- Consumes: automated verification evidence and the user's UI result.
- Produces: final test-plan status and one intentional commit containing the existing multi-catalogue work plus this reset behavior.

- [ ] **Step 1: Hand off exact manual checks**

Ask the user to restart MCA Selector, create a 26.2-only property-bearing From/To draft and rule, select a preset, and test both Cancel and Confirm while switching to 1.18.2. Cancel must preserve everything; Confirm must leave the new 1.18.2 catalogue selected and every Builder field/list/result/status empty or default. Then apply a custom preset containing a 26.2-only exact ID and verify it remains added with a non-blocking current-catalogue warning.

- [ ] **Step 2: Update evidence only after user confirmation**

Change the focused catalogue-switch row in `docs/TEST_PLAN.md` from Pending to Passed with the user-reported date and scope. Do not change it when the UI check fails.

- [ ] **Step 3: Re-run the final automated gate and commit**

Run the full verification commands from Task 3 again after any evidence edit, inspect `git diff --check` and `git status --short`, then commit the intended files with:

```powershell
git commit -m "Reset Builder when switching catalogues"
```
