# ReplaceBlocks Builder Shortcuts and Marquee Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add focused Builder keyboard support and Windows-style rule-table multi-selection, including empty-area marquee selection, so users can save or delete a chosen subset of rules.

**Architecture:** Keep keyboard routing local to the control that owns focus: editable ComboBox editors own completion-list navigation, property and preset ComboBoxes own their open-popup navigation, and the rules table owns `Delete`/`Esc`. Switch the existing rules table to JavaFX multiple selection and draw a mouse-transparent `Rectangle` over its empty-area drag path; convert visible row bounds to `Rectangle2D` values and select only rows intersecting the completed marquee.

**Tech Stack:** Java 21, JavaFX `TableView`/`ComboBox`, JUnit 5, existing ReplaceBlocks Builder model tests.

## Global Constraints

- Work only on `feature/replace-blocks-ui`; preserve user changes and confirm `git status --short` before edits.
- Do not change `ReplaceBlocksField` grammar, version-specific execution, advanced text input, CLI behavior, world saves, or Gradle configuration.
- Keep normal text-field caret, selection, typing, mouse completion, Tab completion, dialog `Enter`/`Esc`, and existing preset precedence intact.
- The existing validator remains the source of truth for `Ctrl+Enter` and preset values.
- Run `compileJava` and `test`; run the JavaFX application for a manual Builder check before committing.

---

### Task 1: Lock model behavior with failing tests

**Files:**
- Modify: `src/test/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderModelTest.java`
- Modify: `src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderDialog.java`

**Interfaces:**
- Produces `static String formatRulesValue(List<Rule> rules)` for parser-normalized multi-rule preset serialization.
- Produces `static int popupSelectionTarget(KeyCode code, int selectedIndex, int itemCount, int visibleRows)`; returns `-1` for a non-navigation key or an empty list.
- Produces `static List<Integer> intersectingRuleIndices(Rectangle2D marquee, List<VisibleRuleBounds> rows)`; only non-empty geometric intersections are returned.

- [x] **Step 1: Write failing serialization and geometry tests**

```java
@Test
void formatsOnlyTheSelectedRulesForPresetSaving() {
    List<ReplaceBlocksRuleBuilderDialog.Rule> selected = List.of(
            new ReplaceBlocksRuleBuilderDialog.Rule("minecraft:stone", "minecraft:dirt"),
            new ReplaceBlocksRuleBuilderDialog.Rule("minecraft:oak_log", "minecraft:air"));

    assertEquals("literal(minecraft:stone)=minecraft:dirt, literal(minecraft:oak_log)=minecraft:air",
            ReplaceBlocksRuleBuilderDialog.formatRulesValue(selected));
}

@Test
void returnsEveryVisibleRuleIntersectingTheMarquee() {
    List<ReplaceBlocksRuleBuilderDialog.VisibleRuleBounds> rows = List.of(
            new ReplaceBlocksRuleBuilderDialog.VisibleRuleBounds(0, new Rectangle2D(0, 0, 600, 24)),
            new ReplaceBlocksRuleBuilderDialog.VisibleRuleBounds(1, new Rectangle2D(0, 24, 600, 24)),
            new ReplaceBlocksRuleBuilderDialog.VisibleRuleBounds(2, new Rectangle2D(0, 48, 600, 24)));

    assertEquals(List.of(0, 1), ReplaceBlocksRuleBuilderDialog.intersectingRuleIndices(
            new Rectangle2D(16, 8, 120, 38), rows));
}
```

- [x] **Step 2: Write the failing popup navigation test**

```java
@Test
void movesAnOpenPopupSelectionByRowsOrPages() {
    assertEquals(0, ReplaceBlocksRuleBuilderDialog.popupSelectionTarget(KeyCode.DOWN, -1, 20, 12));
    assertEquals(11, ReplaceBlocksRuleBuilderDialog.popupSelectionTarget(KeyCode.PAGE_DOWN, 0, 20, 12));
    assertEquals(0, ReplaceBlocksRuleBuilderDialog.popupSelectionTarget(KeyCode.PAGE_UP, 11, 20, 12));
    assertEquals(-1, ReplaceBlocksRuleBuilderDialog.popupSelectionTarget(KeyCode.ENTER, 0, 20, 12));
}
```

- [x] **Step 3: Run the focused test class and verify RED**

Run: `./gradlew.bat test --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksRuleBuilderModelTest`

Expected: compilation fails because the three model helpers do not exist yet.

- [x] **Step 4: Implement the smallest reusable model helpers**

```java
static String formatRulesValue(List<Rule> rules) {
    return rules.stream()
            .map(rule -> formatFrom(rule.from()) + "=" + formatTo(rule.to()))
            .collect(Collectors.joining(", "));
}

static int popupSelectionTarget(KeyCode code, int selectedIndex, int itemCount, int visibleRows) {
    if (itemCount <= 0) {
        return -1;
    }
    int current = selectedIndex < 0 ? 0 : selectedIndex;
    return switch (code) {
        case UP -> Math.max(0, selectedIndex < 0 ? itemCount - 1 : current - 1);
        case DOWN -> Math.min(itemCount - 1, selectedIndex < 0 ? 0 : current + 1);
        case PAGE_UP -> Math.max(0, current - Math.max(1, visibleRows - 1));
        case PAGE_DOWN -> Math.min(itemCount - 1, current + Math.max(1, visibleRows - 1));
        default -> -1;
    };
}

static List<Integer> intersectingRuleIndices(Rectangle2D marquee, List<VisibleRuleBounds> rows) {
    if (marquee.getWidth() <= 0 || marquee.getHeight() <= 0) {
        return List.of();
    }
    return rows.stream().filter(row -> row.bounds().intersects(marquee)).map(VisibleRuleBounds::index).toList();
}
```

- [x] **Step 5: Run the focused test class and verify GREEN**

Run: `./gradlew.bat test --tests net.querz.mcaselector.ui.dialog.ReplaceBlocksRuleBuilderModelTest`

Expected: all tests in the class pass.

### Task 2: Add local keyboard routing and rule-table multi-selection

**Files:**
- Modify: `src/main/java/net/querz/mcaselector/ui/dialog/ReplaceBlocksRuleBuilderDialog.java`
- Modify: `src/main/resources/style/component/change-nbt-dialog.css`

**Interfaces:**
- Consumes Task 1 helper methods.
- Produces multiple-rule preset values through `presetValue()` and `formatRulesValue(...)`.
- Produces local `Ctrl+Enter`, rules-table `Delete`/`Esc`, and popup list navigation without a dialog-wide key filter.

- [x] **Step 1: Make the rule table natively multi-selectable**

```java
rules.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
rules.getSelectionModel().getSelectedItems().addListener(
        (ListChangeListener<Rule>) change -> updatePresetButtons());

edit.disableProperty().bind(Bindings.size(rules.getSelectionModel().getSelectedItems()).isNotEqualTo(1));
delete.disableProperty().bind(Bindings.isEmpty(rules.getSelectionModel().getSelectedItems()));
```

Replace the one-rule delete action with `deleteSelectedRules()` that copies `getSelectedItems()`, removes every copied `Rule` from `ruleItems`, then calls `updateResult()` and `clearPresetSelectionAfterEdit()`. Keep edit restricted to exactly one selected row.

- [x] **Step 2: Make selected subsets take precedence when saving presets**

```java
private PresetValue presetValue() {
    List<Rule> selected = List.copyOf(rules.getSelectionModel().getSelectedItems());
    if (!selected.isEmpty()) {
        return validatedPresetValue(formatRulesValue(selected));
    }
    // Existing valid draft, then all-table-rules fallback remains unchanged.
}
```

This preserves the documented order: selected rule or rules, valid draft, then all rules.

- [x] **Step 3: Install focused shortcut handlers**

```java
private void handleBuilderShortcut(KeyEvent event) {
    if (event.isControlDown() && event.getCode() == KeyCode.ENTER) {
        addRule(null);
        event.consume();
    }
}

private void handleRulesKeyPressed(KeyEvent event) {
    if (event.getCode() == KeyCode.DELETE && !rules.getSelectionModel().getSelectedItems().isEmpty()) {
        deleteSelectedRules();
        event.consume();
    } else if (event.getCode() == KeyCode.ESCAPE) {
        rules.getSelectionModel().clearSelection();
        event.consume();
    }
}
```

Attach `handleBuilderShortcut` only to From/To editors, source tile/Y/biome controls, and dynamically created property controls. Attach `handleRulesKeyPressed` only to `rules`.

- [x] **Step 4: Add open-popup navigation without taking over normal editor keys**

```java
private boolean moveOpenPopupSelection(ComboBox<?> combo, KeyEvent event) {
    if (!combo.isShowing()) {
        return false;
    }
    if (event.getCode() == KeyCode.ESCAPE) {
        combo.hide();
        event.consume();
        return true;
    }
    int target = popupSelectionTarget(event.getCode(), combo.getSelectionModel().getSelectedIndex(),
            combo.getItems().size(), combo.getVisibleRowCount());
    if (target < 0) {
        return false;
    }
    combo.getSelectionModel().select(target);
    event.consume();
    return true;
}
```

For From and biome editable ComboBoxes, call this before the existing Tab handler; `Enter` completes the highlighted value with the existing `completeSuggestion(...)`/`completeBiomeSuggestion(...)` method, and `Esc` closes only the popup. For property and preset lists, apply the selection movement while open; `Enter` commits the highlighted value by closing the popup. No handler runs when the popup is closed, so ordinary caret movement and text editing remain native.

- [x] **Step 5: Add empty-area marquee selection**

```java
private final Pane ruleMarqueeLayer = new Pane();
private final Rectangle ruleMarquee = new Rectangle();
private Point2D ruleMarqueeAnchor;
private boolean ruleMarqueeAdditive;

private void applyRuleMarquee() {
    Rectangle2D selection = new Rectangle2D(ruleMarquee.getX(), ruleMarquee.getY(),
            ruleMarquee.getWidth(), ruleMarquee.getHeight());
    List<Integer> matches = intersectingRuleIndices(selection, visibleRuleBounds());
    if (!ruleMarqueeAdditive) {
        rules.getSelectionModel().clearSelection();
    }
    matches.forEach(index -> rules.getSelectionModel().select(index));
}
```

Wrap `rules` and `ruleMarqueeLayer` in a `StackPane`. Keep the layer mouse-transparent, style `ruleMarquee` as a translucent outlined rectangle, and listen to the table's primary-button press/drag/release events only when `isFilledRuleRow(event.getTarget())` is false. Draw using table-local coordinates; at release, transform visible non-empty `.table-row-cell` bounds into the same coordinate space, select intersecting rows, then hide the rectangle. Without Ctrl, the marquee replaces the selection; with Ctrl, it adds to the existing selection. Normal row clicks, Ctrl/Shift row selection, and blank-area click-to-clear remain native.

- [x] **Step 6: Compile after JavaFX integration**

Run: `./gradlew.bat compileJava`

Expected: `BUILD SUCCESSFUL`.

### Task 3: Record regression coverage and verify the Builder

**Files:**
- Modify: `docs/superpowers/specs/2026-07-12-replaceblocks-builder-shortcuts-design.md`
- Modify: `docs/TEST_PLAN.md`
- Modify: `docs/ROADMAP.md`

- [x] **Step 1: Mark the shortcut design implemented and document multi-selection scope**

Change the design status to `Implemented` and add the rule-table behavior: Ctrl/Shift selection remains native; dragging from empty table space selects intersecting visible rules; Ctrl-drag adds; multi-selection saves only selected rules; editing remains single-row only; Delete deletes all selected rules.

- [x] **Step 2: Extend the manual regression matrix**

Add English/Chinese checks for: Up/Down/PageUp/PageDown/Enter/Esc in From/To/biome/property/preset popups; text-caret preservation when popups are closed; valid and invalid `Ctrl+Enter`; table `Delete`/`Esc`; Ctrl/Shift row selection; empty-area marquee selecting two of three rows; Ctrl-marquee addition; saving/deleting a selected subset; no JavaFX console exception.

- [x] **Step 3: Run full automated validation**

Run: `./gradlew.bat test`

Expected: all existing and new JUnit tests pass.

- [ ] **Step 4: Run manual Builder validation (user handoff)**

Run: `./gradlew.bat run`

Expected: manually verify the Task 3 checks in English and Chinese, including a selected two-of-three-rule custom preset and no JavaFX errors in the console. If a real-world visual regression cannot be reproduced in the local JavaFX run, leave it for the user with exact steps rather than claiming it passed.

- [ ] **Step 5: Check patch quality and commit**

Run: `git diff --check` then `git status --short`.

Commit only the Builder Java/CSS/tests and the three documentation files with:

```text
Improve Builder shortcuts and rule selection
```
