# ReplaceBlocks Builder Transactional Close Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Builder Cancel and title-bar close preserve the session when discard is declined, while keeping the outer ReplaceBlocks text transactional.

**Architecture:** Snapshot Builder content after initial restoration and compare it on every close attempt. Intercept the underlying JavaFX window close event, route it and the Builder Cancel button through one discard decision, and leave OK as the only apply path.

**Tech Stack:** Java 21, JavaFX 21, JUnit 5, Gradle

## Global Constraints

- Preserve ReplaceBlocks syntax, parsing, preview, and execution semantics.
- Preserve the outer Change NBT field unless Builder OK returns a valid value.
- Do not change catalogue data, Gradle build logic, or Minecraft world data.
- Keep UI-intensive acceptance user-run.

---

### Task 1: Lock the transactional state contract

**Files:**
- Modify: `src/test/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderModelTest.java`
- Modify: `src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderDialog.java`

**Interfaces:**
- Consumes: restored `ruleItems` and current From/To draft controls
- Produces: `BuilderContentState`, `BlockInputState`, and `isBuilderDirty()`

- [x] **Step 1: Write failing tests**

Add tests proving that restored initial rules are clean, an edited draft is
dirty, reverting it is clean again, and resetting a Builder loaded from advanced
text remains dirty even after every visible control becomes empty.

- [x] **Step 2: Run the focused tests and verify RED**

```powershell
.\gradlew.bat test --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksRuleBuilderModelTest
```

Expected: failure because the Builder does not yet retain a baseline or expose
content-comparison dirty state.

- [x] **Step 3: Implement immutable content snapshots**

Capture rule order plus raw From/To text, source restrictions, and property
choices after `loadSimpleRules(initialValue)`. Compare the live snapshot with
that baseline; ignore popup, selection, validation, and catalogue-only state.

- [x] **Step 4: Run the focused tests and verify GREEN**

```powershell
.\gradlew.bat test --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksRuleBuilderModelTest
```

Expected: all focused tests pass.

### Task 2: Unify Builder close requests

**Files:**
- Modify: `src/test/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderModelTest.java`
- Modify: `src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderDialog.java`

**Interfaces:**
- Consumes: `isBuilderDirty()` and `confirmDiscardBuilder()`
- Produces: one close decision used by the Stage window filter and Cancel button

- [x] **Step 1: Write failing JavaFX close tests**

Add tests that fire the actual Builder `WINDOW_CLOSE_REQUEST`, then cancel or
close the nested discard confirmation and assert that the window event is
consumed and Builder remains visible. Add explicit-discard and Builder-Cancel
coverage.

- [x] **Step 2: Run the focused tests and verify RED**

```powershell
.\gradlew.bat test --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksRuleBuilderModelTest
```

Expected: the title-bar event remains unconsumed or Builder Cancel closes
without honoring the declined discard.

- [x] **Step 3: Implement the window-level close filter**

Install/remove a `WINDOW_CLOSE_REQUEST` event filter with the Builder lifecycle,
route Cancel through the same close decision, remove the old `dialogButtonClose`
and `DialogEvent` workaround, and let OK close without discard confirmation.

- [x] **Step 4: Run the focused tests and verify GREEN**

```powershell
.\gradlew.bat test --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksRuleBuilderModelTest
```

Expected: all focused tests pass with no orphaned JavaFX windows.

### Task 3: Localize, document, verify, and publish

**Files:**
- Modify: `src/main/java/net/querz/mcaselector/text/Translation.java`
- Modify: `src/main/resources/lang/*.txt`
- Modify: `ONBOARDING.md`
- Modify: `docs/DEV_NOTES_REPLACE_BLOCKS.md`
- Modify: `docs/TEST_PLAN.md`

**Interfaces:**
- Consumes: the final close behavior
- Produces: translated discard/continue actions and durable regression guidance

- [x] **Step 1: Add explicit localized actions and explanatory copy**

Use `Discard changes` and `Continue editing`; Chinese copy must say that the
outer ReplaceBlocks field keeps its pre-Builder value.

- [x] **Step 2: Update project documentation**

Record the JavaFX Stage/Dialog event distinction, content-baseline dirty
semantics, catalogue-reset behavior, and exact manual pass criteria.

- [x] **Step 3: Run all required verification**

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
.\gradlew.bat run --args="--mode printMissingTranslations"
.\gradlew.bat build shadowJar
git diff --check
```

Expected: every command exits zero, translation output has no missing keys, and
the diff has no whitespace errors.

- [x] **Step 4: Commit and push**

```powershell
git add ONBOARDING.md docs src
git commit -m "Fix transactional Builder closing"
git push origin feature/replace-blocks-ui
```

Expected: the commit is present locally and the remote branch advances to the
same commit.
