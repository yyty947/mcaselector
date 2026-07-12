# ReplaceBlocks Builder Dropdown UX Implementation Plan

> **For agentic workers:** Execute inline in the current session. Do not dispatch subagents. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every ReplaceBlocks Builder dropdown visually clear, keep autocomplete lists stationary while keyboard highlights remain visible, and show full block/biome catalogs on explicit empty-arrow expansion.

**Architecture:** Extend the existing Builder-local ComboBox helpers in `ReplaceBlocksRuleBuilderDialog`. Autocomplete highlight state remains independent of JavaFX selection/value state, and a custom pseudo-class provides the visual highlight without triggering `ListView` focus-driven scrolling. A shared popup configurator attaches Builder-only styling to every ComboBox popup.

**Tech Stack:** Java 21, JavaFX 21 controls/skins, JUnit Jupiter, JavaFX CSS, Gradle.

## Global Constraints

- Preserve the existing popup-Scene key capture and non-selecting autocomplete navigation.
- Preserve advanced text input, parser/execution behavior, mouse completion, Tab/Enter completion, and biome semicolon-token completion.
- Do not modify Minecraft worlds or Gradle build logic.
- Do not restyle ComboBoxes outside the ReplaceBlocks Builder.
- Perform one final commit after automated checks and the user's explicit finish/commit request; keep the final focused UI acceptance gate documented when it has not yet been rerun.

---

### Task 1: Boundary-aware autocomplete highlight and scrolling

**Files:**
- Modify: `src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderDialog.java`
- Test: `src/test/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderModelTest.java`

**Interfaces:**
- Consumes: `popupSelectionTarget(KeyCode, int, int, int)` and popup-Scene key capture.
- Produces: `popupNeedsReveal(int target, int firstVisible, int lastVisible)`, `highlightPopupSuggestion(ComboBox<?>, int)`, and the `autocomplete-highlighted` pseudo-class.

- [x] **Step 1: Write failing boundary and control-level tests**

Add assertions showing that indices inside `[firstVisible, lastVisible]` do not request scrolling, while indices outside do. Update the JavaFX control test to require editor text `aca`, `null` ComboBox value, selection index `-1`, and the custom highlight pseudo-class after navigation rather than a changed `FocusModel` index.

```java
assertFalse(ReplaceBlocksRuleBuilderDialog.popupNeedsReveal(0, 0, 4));
assertFalse(ReplaceBlocksRuleBuilderDialog.popupNeedsReveal(3, 0, 4));
assertFalse(ReplaceBlocksRuleBuilderDialog.popupNeedsReveal(4, 0, 4));
assertTrue(ReplaceBlocksRuleBuilderDialog.popupNeedsReveal(5, 0, 4));
assertTrue(ReplaceBlocksRuleBuilderDialog.popupNeedsReveal(4, 5, 9));
```

- [x] **Step 2: Run the focused test and confirm RED**

Run:

```powershell
.\gradlew.bat test --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksRuleBuilderModelTest
```

Expected: compilation or assertion failure because boundary-aware reveal and custom popup highlighting are not implemented.

- [x] **Step 3: Implement non-focusing popup highlighting**

Replace the unconditional `popup.getFocusModel().focus(index)` plus `popup.scrollTo(index)` path. Inspect the popup's `VirtualFlow` first/last visible cells, apply `autocomplete-highlighted` only to the cell matching the independent highlight index, and call `popup.scrollTo(index)` only when `popupNeedsReveal(...)` is true. Reapply the pseudo-class on the next JavaFX pulse after a boundary scroll creates a new cell.

```java
static boolean popupNeedsReveal(int target, int firstVisible, int lastVisible) {
	return target < firstVisible || target > lastVisible;
}
```

- [x] **Step 4: Run the focused test and confirm GREEN**

Run the Task 1 command again. Expected: all `ReplaceBlocksRuleBuilderModelTest` tests pass with no editor/value/selection mutation.

---

### Task 2: Explicit empty-query catalog expansion

**Files:**
- Modify: `src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderDialog.java`
- Test: `src/test/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderModelTest.java`

**Interfaces:**
- Consumes: sorted `blockNames`, sorted `biomeCatalogNames`, current block/biome editor query, and existing suggestion lists.
- Produces: `suggestionsForQuery(List<String>, String, boolean)` and arrow-button expansion handlers for block and biome ComboBoxes.

- [x] **Step 1: Write failing suggestion-mode tests**

```java
List<String> names = List.of("minecraft:acacia", "minecraft:stone");
assertEquals(List.of(), ReplaceBlocksRuleBuilderDialog.suggestionsForQuery(names, "", false));
assertEquals(names, ReplaceBlocksRuleBuilderDialog.suggestionsForQuery(names, "", true));
assertEquals(List.of("minecraft:acacia"),
		ReplaceBlocksRuleBuilderDialog.suggestionsForQuery(names, "aca", false));
```

Add a JavaFX event test proving that an explicit arrow-button press fills the empty block/biome suggestion list without changing the editor value.

- [x] **Step 2: Run the focused test and confirm RED**

Run the Task 1 command. Expected: failure because explicit empty-query expansion does not exist.

- [x] **Step 3: Implement explicit expansion**

Add one shared filter function and attach a mouse-pressed filter that recognizes the ComboBox arrow-button ancestry. On an empty block or biome editor, populate the complete sorted catalog before native ComboBox showing proceeds. Keep scheduled text-change filtering in `allowEmpty=false` mode so an empty Builder never opens automatically and clearing text returns to the closed/empty filtered state.

- [x] **Step 4: Run the focused test and confirm GREEN**

Run the Task 1 command. Expected: explicit empty expansion, filtered input, and existing completion tests all pass.

---

### Task 3: Builder-only dropdown visual system

**Files:**
- Modify: `src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderDialog.java`
- Modify: `src/main/resources/style/component/change-nbt-dialog.css`
- Test: `src/test/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderModelTest.java`

**Interfaces:**
- Consumes: every Builder ComboBox and its `ComboBoxListViewSkin` popup `ListView`.
- Produces: shared popup style class `replace-blocks-builder-dropdown` and visual states for `:hover`, `:focused`, `:selected`, and `:autocomplete-highlighted`.

- [x] **Step 1: Write failing style-hook test**

Create a JavaFX ComboBox with `ComboBoxListViewSkin`, invoke the shared Builder popup configurator, and assert that its popup content contains `replace-blocks-builder-dropdown`.

- [x] **Step 2: Run the focused test and confirm RED**

Run the Task 1 command. Expected: failure because the shared popup style hook is absent.

- [x] **Step 3: Attach the shared configurator to every Builder ComboBox**

Apply it to presets, From, To, source Extra NBT, source biome, and every dynamically created property ComboBox. The configurator must handle both an already-installed skin and a later skin replacement.

- [x] **Step 4: Add Builder popup CSS**

Use a light translucent blue for hover and solid `#4aa3ff` for focused, selected, and autocomplete-highlighted rows. Keep text white and override the substring-highlight fill to white on a solid-blue row. Use an inset border/background layer so row dimensions do not change.

```css
.replace-blocks-builder-dropdown .list-cell:filled:hover {
    -fx-background-color: rgba(74, 163, 255, 0.32);
}

.replace-blocks-builder-dropdown .list-cell:filled:focused,
.replace-blocks-builder-dropdown .list-cell:filled:selected,
.replace-blocks-builder-dropdown .list-cell:filled:autocomplete-highlighted {
    -fx-background-color: #4aa3ff;
    -fx-text-fill: white;
}
```

- [x] **Step 5: Run the focused test and confirm GREEN**

Run the Task 1 command. Expected: all Builder model/control tests pass.

---

### Task 4: Documentation, full verification, and user acceptance

**Files:**
- Modify: `ONBOARDING.md`
- Modify: `AGENTS.md`
- Modify: `docs/ROADMAP.md`
- Modify: `docs/DEV_NOTES_REPLACE_BLOCKS.md`
- Modify: `docs/TEST_PLAN.md`
- Modify: `docs/findings/ui_reference.md`
- Keep: `docs/superpowers/specs/2026-07-13-replaceblocks-builder-dropdown-ux-design.md`

**Interfaces:**
- Consumes: completed Tasks 1-3 behavior.
- Produces: synchronized current-state documentation and final evidence.

- [x] **Step 1: Update behavior and manual-test documentation**

Record boundary-aware scrolling, stronger Builder-only popup states, and explicit empty-arrow expansion. Do not mark Phase 6 complete solely from automated tests.

- [x] **Step 2: Run full automated verification**

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
git diff --check
```

Expected: successful compile, zero test failures/errors, and no whitespace errors beyond informational line-ending warnings.

- [x] **Step 3: Provide the user UI acceptance checklist**

Require a fresh restart and checks for From, To, biome, preset, property, and Extra NBT lists. Passing means stationary in-range arrow navigation, boundary-only scrolling, page navigation, distinct light-blue hover and solid-blue keyboard/selected states, full sorted empty-arrow lists, preserved completion behavior, and no JavaFX console exceptions.

- [x] **Step 4: Commit after explicit user direction**

The user explicitly requested that the verified candidate be finished and committed while the focused manual dropdown rerun remains documented as the final Phase 6 gate:

```powershell
git add AGENTS.md ONBOARDING.md docs/ROADMAP.md docs/DEV_NOTES_REPLACE_BLOCKS.md docs/TEST_PLAN.md docs/findings/ui_reference.md docs/superpowers/specs/2026-07-13-replaceblocks-builder-dropdown-ux-design.md docs/superpowers/plans/2026-07-13-replaceblocks-builder-dropdown-ux.md src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderDialog.java src/main/resources/style/component/change-nbt-dialog.css src/test/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderModelTest.java
git commit -m "Improve Builder dropdown navigation"
```
