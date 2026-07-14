package net.querz.mcaselector.ui.dialog;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.BoundingBox;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ReplaceBlocksRuleBuilderModelTest {

	private static final Object javaFxStartupLock = new Object();
	private static boolean javaFxStarted;

	@Test
	void restoresLiteralSourceAndNamedTarget() {
		ReplaceBlocksRuleBuilderDialog.ParsedRule parsed = ReplaceBlocksRuleBuilderDialog.parseEditableRule(
				"literal(minecraft:acacia_stairs)",
				"minecraft:andesite_stairs");

		assertNotNull(parsed);
		assertEquals(ChunkFilter.BlockReplaceSourceType.LITERAL_NAME, parsed.source().getType());
		assertEquals("minecraft:acacia_stairs", parsed.source().getName());
		assertEquals(ChunkFilter.BlockReplaceType.NAME, parsed.target().getType());
		assertEquals("minecraft:andesite_stairs", parsed.target().getName());
	}

	@Test
	void restoresSelectedPropertiesAndSourceConditions() {
		ReplaceBlocksRuleBuilderDialog.ParsedRule parsed = ReplaceBlocksRuleBuilderDialog.parseEditableRule(
				"biome(minecraft:forest, y(-16..31, tile(props({Name:\"minecraft:acacia_stairs\",Properties:{facing:\"north\",half:\"top\"}}))))",
				"{Name:\"minecraft:andesite_stairs\",Properties:{facing:\"south\",half:\"bottom\"}}");

		assertNotNull(parsed);
		assertEquals(ChunkFilter.BlockReplaceSourceType.SELECTED_PROPERTIES, parsed.source().getType());
		assertEquals(ChunkFilter.BlockReplaceTileEntityMode.REQUIRE_TILE_ENTITY, parsed.source().getTileEntityMode());
		assertEquals(-16, parsed.source().getMinY());
		assertEquals(31, parsed.source().getMaxY());
		assertEquals(Map.of("facing", "north", "half", "top"),
				ReplaceBlocksRuleBuilderDialog.editableStateProperties(parsed.source().getState()));
		assertEquals(Map.of("facing", "south", "half", "bottom"),
				ReplaceBlocksRuleBuilderDialog.editableStateProperties(parsed.target().getState()));
		assertEquals(java.util.Set.of("minecraft:forest"), parsed.source().getBiomes());
	}

	@Test
	void keepsAdvancedStateAndTileTargetsAsFallbackData() {
		ReplaceBlocksRuleBuilderDialog.ParsedRule parsed = ReplaceBlocksRuleBuilderDialog.parseEditableRule(
				"{Name:\"minecraft:chest\",Properties:{facing:\"north\"},Custom:1b}",
				"minecraft:barrel;{id:\"minecraft:barrel\"}");

		assertNotNull(parsed);
		assertEquals(ChunkFilter.BlockReplaceSourceType.EXACT_STATE, parsed.source().getType());
		assertNull(ReplaceBlocksRuleBuilderDialog.editableStateProperties(parsed.source().getState()));
		assertEquals(ChunkFilter.BlockReplaceType.NAME_TILE, parsed.target().getType());
		assertNotNull(parsed.target().getTile());
	}

	@Test
	void wrapsPlainTableSourcesAsLiteralWhenLoadingForEdit() {
		ReplaceBlocksRuleBuilderDialog.ParsedRule parsed = ReplaceBlocksRuleBuilderDialog.parseEditableRule(
				"minecraft:stone",
				"minecraft:dirt");

		assertNotNull(parsed);
		assertEquals(ChunkFilter.BlockReplaceSourceType.LITERAL_NAME, parsed.source().getType());
		assertEquals("minecraft:stone", parsed.source().getName());
	}

	@Test
	void rejectsMalformedRules() {
		assertNull(ReplaceBlocksRuleBuilderDialog.parseEditableRule("literal(", "minecraft:stone"));
	}

	@Test
	void parsesEveryRuleFromMultiRulePresetWithoutLosingState() {
		List<ReplaceBlocksRuleBuilderDialog.Rule> rules = ReplaceBlocksRuleBuilderDialog.parseSimpleRules(
				"props({Name:\"minecraft:acacia_stairs\",Properties:{facing:\"north\"}})="
						+ "{Name:\"minecraft:andesite_stairs\",Properties:{facing:\"south\"}}, "
						+ "biome(minecraft:plains, literal(minecraft:stone))=minecraft:dirt");

		assertEquals(2, rules.size());
		ReplaceBlocksRuleBuilderDialog.ParsedRule stateRule = ReplaceBlocksRuleBuilderDialog.parseEditableRule(
				rules.get(0).from(), rules.get(0).to());
		assertNotNull(stateRule);
		assertEquals(Map.of("facing", "north"),
				ReplaceBlocksRuleBuilderDialog.editableStateProperties(stateRule.source().getState()));
		assertEquals(Map.of("facing", "south"),
				ReplaceBlocksRuleBuilderDialog.editableStateProperties(stateRule.target().getState()));
	}

	@Test
	void duplicatePresetSourcesKeepOnlyTheLastTarget() {
		String value = "literal(stone)=minecraft:dirt, literal(minecraft:stone)=minecraft:gold_block";

		assertEquals("literal(minecraft:stone)=minecraft:gold_block",
				ReplaceBlocksRuleBuilderDialog.normalizeRulesValue(value));
		List<ReplaceBlocksRuleBuilderDialog.Rule> rules = ReplaceBlocksRuleBuilderDialog.parseSimpleRules(value);
		assertEquals(1, rules.size());
		assertEquals("literal(minecraft:stone)", rules.get(0).from());
		assertEquals("minecraft:gold_block", rules.get(0).to());
	}

	@Test
	void biomeCompletionUsesTokenAtCaret() {
		String value = "minecraft:birch_forest;minecraft:pla";

		assertEquals("minecraft:pla", ReplaceBlocksRuleBuilderDialog.biomeToken(value, value.length()));
		assertEquals("minecraft:birch_forest", ReplaceBlocksRuleBuilderDialog.biomeToken(value, 8));
		assertEquals("", ReplaceBlocksRuleBuilderDialog.biomeToken("minecraft:plains;", 17));
	}

	@Test
	void normalizesEquivalentStateRulesForDuplicateDetection() {
		String first = ReplaceBlocksRuleBuilderDialog.normalizeRule(
				"tile(props({Name:\"minecraft:acacia_log\",Properties:{axis:\"y\"}}))",
				"{Name:\"minecraft:acacia_leaves\",Properties:{waterlogged:\"true\",distance:\"2\",persistent:\"false\"}}");
		String reordered = ReplaceBlocksRuleBuilderDialog.normalizeRule(
				"tile( props( { Properties: { axis: \"y\" }, Name: \"minecraft:acacia_log\" } ) )",
				"{ Properties: { persistent: \"false\", distance: \"2\", waterlogged: \"true\" }, Name: \"minecraft:acacia_leaves\" }");

		assertEquals(first, reordered);
	}

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

	@Test
	void recognizesControlEnterForAddingRules() {
		assertTrue(ReplaceBlocksRuleBuilderDialog.isBuilderAddShortcut(KeyCode.ENTER, true));
		assertTrue(ReplaceBlocksRuleBuilderDialog.isBuilderAddShortcut(KeyCode.SEPARATOR, true));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isBuilderAddShortcut(KeyCode.ENTER, false));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isBuilderAddShortcut(KeyCode.DOWN, true));
	}

	@Test
	void permitsMarqueeFromRulesAndBlankTableAreas() {
		assertTrue(ReplaceBlocksRuleBuilderDialog.canStartRuleMarquee(true, false));
		assertFalse(ReplaceBlocksRuleBuilderDialog.canStartRuleMarquee(true, true));
		assertFalse(ReplaceBlocksRuleBuilderDialog.canStartRuleMarquee(false, false));
	}

	@Test
	void movesAutocompleteHighlightWithoutChangingTheQuery() {
		assertEquals(0, ReplaceBlocksRuleBuilderDialog.popupSelectionTarget(KeyCode.DOWN, -1, 20, 12));
		assertEquals(11, ReplaceBlocksRuleBuilderDialog.popupSelectionTarget(KeyCode.PAGE_DOWN, 0, 20, 12));
		assertEquals(0, ReplaceBlocksRuleBuilderDialog.popupSelectionTarget(KeyCode.PAGE_UP, 11, 20, 12));
		assertEquals(-1, ReplaceBlocksRuleBuilderDialog.popupSelectionTarget(KeyCode.ENTER, 0, 20, 12));
	}

	@Test
	void revealsAutocompleteHighlightOnlyOutsideTheVisibleRange() {
		assertFalse(ReplaceBlocksRuleBuilderDialog.popupNeedsReveal(0, 0, 4));
		assertFalse(ReplaceBlocksRuleBuilderDialog.popupNeedsReveal(3, 0, 4));
		assertFalse(ReplaceBlocksRuleBuilderDialog.popupNeedsReveal(4, 0, 4));
		assertTrue(ReplaceBlocksRuleBuilderDialog.popupNeedsReveal(5, 0, 4));
		assertTrue(ReplaceBlocksRuleBuilderDialog.popupNeedsReveal(4, 5, 9));
	}

	@Test
	void pageNavigationUsesTheActuallyVisiblePopupRows() {
		assertEquals(5, ReplaceBlocksRuleBuilderDialog.popupPageSize(3, 7, 12));
		assertEquals(12, ReplaceBlocksRuleBuilderDialog.popupPageSize(3, 2, 12));
		assertEquals(1, ReplaceBlocksRuleBuilderDialog.popupPageSize(3, 2, 0));
	}

	@Test
	void treatsPartiallyClippedPopupRowsAsOutsideTheVisibleRange() {
		BoundingBox viewport = new BoundingBox(0, 0, 320, 120);

		assertTrue(ReplaceBlocksRuleBuilderDialog.isPopupCellFullyVisible(
				new BoundingBox(0, 0, 300, 24), viewport));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isPopupCellFullyVisible(
				new BoundingBox(0, -1, 300, 24), viewport));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isPopupCellFullyVisible(
				new BoundingBox(0, 100, 300, 24), viewport));
	}

	@Test
	void clearedAutocompleteIndexDoesNotLeaveAHighlightedCell() {
		assertTrue(ReplaceBlocksRuleBuilderDialog.isAutocompleteCellHighlighted(2, 2, false));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isAutocompleteCellHighlighted(2, -1, false));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isAutocompleteCellHighlighted(2, 2, true));
	}

	@Test
	void emptySuggestionsRequireExplicitExpansion() {
		List<String> names = List.of("minecraft:acacia", "minecraft:stone");

		assertEquals(List.of(), ReplaceBlocksRuleBuilderDialog.suggestionsForQuery(names, "", false));
		assertEquals(names, ReplaceBlocksRuleBuilderDialog.suggestionsForQuery(names, "", true));
		assertEquals(List.of("minecraft:acacia"),
				ReplaceBlocksRuleBuilderDialog.suggestionsForQuery(names, "aca", false));
	}

	@Test
	void clearsAnExplicitEmptyCatalogAfterItsPopupCloses() throws Throwable {
		runOnJavaFxThread(() -> {
			ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(
					"minecraft:acacia", "minecraft:stone"));
			comboBox.setEditable(true);
			comboBox.getSelectionModel().select(0);
			comboBox.getEditor().clear();

			ReplaceBlocksRuleBuilderDialog.clearEmptyExplicitCatalog(comboBox);

			assertTrue(comboBox.getItems().isEmpty());
			assertEquals(-1, comboBox.getSelectionModel().getSelectedIndex());
			assertNull(comboBox.getValue());
			assertEquals("", comboBox.getEditor().getText());
			comboBox.getSelectionModel().selectNext();
			assertEquals(-1, comboBox.getSelectionModel().getSelectedIndex());
		});
	}

	@Test
	void clearsOnlyTheCatalogFromTheMatchingExplicitClose() {
		assertTrue(ReplaceBlocksRuleBuilderDialog.shouldClearExplicitCatalog(true, false, ""));
		assertFalse(ReplaceBlocksRuleBuilderDialog.shouldClearExplicitCatalog(false, false, ""));
		assertFalse(ReplaceBlocksRuleBuilderDialog.shouldClearExplicitCatalog(true, true, ""));
		assertFalse(ReplaceBlocksRuleBuilderDialog.shouldClearExplicitCatalog(true, false, "minecraft:stone"));
	}

	@Test
	void recognizesOnlyTheComboBoxArrowAsExplicitExpansion() {
		HBox arrowButton = new HBox();
		arrowButton.getStyleClass().add("arrow-button");
		StackPane arrow = new StackPane();
		arrowButton.getChildren().add(arrow);

		assertTrue(ReplaceBlocksRuleBuilderDialog.isComboBoxArrowTarget(arrow));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isComboBoxArrowTarget(new StackPane()));
	}

	@Test
	void popupHighlightDoesNotUseEditableComboBoxFocusOrSelection() throws Throwable {
		runOnJavaFxThread(() -> {
			ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(
					"minecraft:acacia_button",
					"minecraft:acacia_door",
					"minecraft:acacia_fence"));
			comboBox.setEditable(true);
			comboBox.setSkin(new ComboBoxListViewSkin<>(comboBox));
			StackPane root = new StackPane(comboBox);
			new Scene(root);
			root.applyCss();
			comboBox.getEditor().setText("aca");

			assertTrue(ReplaceBlocksRuleBuilderDialog.focusPopupSuggestion(comboBox, 1));
			assertEquals("aca", comboBox.getEditor().getText());
			assertNull(comboBox.getValue());
			assertEquals(-1, comboBox.getSelectionModel().getSelectedIndex());
			ListView<?> popup = (ListView<?>) ((ComboBoxListViewSkin<?>) comboBox.getSkin()).getPopupContent();
			assertEquals(-1, popup.getFocusModel().getFocusedIndex());
		});
	}

	@Test
	void movingAutocompleteHighlightClearsNativePopupSelection() throws Throwable {
		runOnJavaFxThread(() -> {
			ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(
					"minecraft:acacia_button",
					"minecraft:acacia_door",
					"minecraft:acacia_fence"));
			comboBox.setEditable(true);
			comboBox.setSkin(new ComboBoxListViewSkin<>(comboBox));
			StackPane root = new StackPane(comboBox);
			new Scene(root);
			root.applyCss();

			ListView<?> popup = (ListView<?>) ((ComboBoxListViewSkin<?>) comboBox.getSkin()).getPopupContent();
			popup.getSelectionModel().select(0);
			popup.getFocusModel().focus(0);

			assertTrue(ReplaceBlocksRuleBuilderDialog.focusPopupSuggestion(comboBox, 2));
			assertEquals(-1, popup.getSelectionModel().getSelectedIndex());
			assertEquals(-1, popup.getFocusModel().getFocusedIndex());
		});
	}

	@Test
	void builderComboBoxAddsBuilderOnlyPopupStyle() throws Throwable {
		runOnJavaFxThread(() -> {
			ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList("one", "two"));
			ReplaceBlocksRuleBuilderDialog.configureBuilderComboBox(comboBox);
			comboBox.setSkin(new ComboBoxListViewSkin<>(comboBox));

			ListView<?> popup = (ListView<?>) ((ComboBoxListViewSkin<?>) comboBox.getSkin()).getPopupContent();
			assertTrue(popup.getStyleClass().contains("replace-blocks-builder-dropdown"));

			comboBox.setSkin(new ComboBoxListViewSkin<>(comboBox));
			ListView<?> replacementPopup = (ListView<?>) ((ComboBoxListViewSkin<?>) comboBox.getSkin()).getPopupContent();
			assertTrue(replacementPopup.getStyleClass().contains("replace-blocks-builder-dropdown"));
		});
	}

	@Test
	void popupNavigationFilterReceivesKeysFromPopupScene() throws Throwable {
		runOnJavaFxThread(() -> {
			ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(
					"minecraft:acacia_button",
					"minecraft:acacia_door",
					"minecraft:acacia_fence"));
			comboBox.setEditable(true);
			comboBox.setSkin(new ComboBoxListViewSkin<>(comboBox));
			StackPane root = new StackPane(comboBox);
			new Scene(root);
			root.applyCss();
			comboBox.getEditor().setText("aca");
			int[] highlightedIndex = {-1};
			ReplaceBlocksRuleBuilderDialog.installAutocompletePopupKeyFilter(comboBox, event -> {
				highlightedIndex[0] = ReplaceBlocksRuleBuilderDialog.popupSelectionTarget(
						event.getCode(), highlightedIndex[0], comboBox.getItems().size(), 3);
				ReplaceBlocksRuleBuilderDialog.focusPopupSuggestion(comboBox, highlightedIndex[0]);
				event.consume();
			});

			ListView<?> popup = (ListView<?>) ((ComboBoxListViewSkin<?>) comboBox.getSkin()).getPopupContent();
			popup.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN,
					false, false, false, false));

			assertEquals(0, highlightedIndex[0]);
			assertEquals("aca", comboBox.getEditor().getText());
			assertNull(comboBox.getValue());
			assertEquals(-1, comboBox.getSelectionModel().getSelectedIndex());
			assertEquals(-1, popup.getFocusModel().getFocusedIndex());
		});
	}

	@Test
	void openingAutocompletePopupClearsNativeNavigationBeforeFirstArrowKey() throws Throwable {
		runOnJavaFxThread(() -> {
			ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(
					"minecraft:acacia_button",
					"minecraft:acacia_door",
					"minecraft:acacia_fence"));
			comboBox.setEditable(true);
			comboBox.setSkin(new ComboBoxListViewSkin<>(comboBox));
			StackPane root = new StackPane(comboBox);
			new Scene(root);
			root.applyCss();
			ReplaceBlocksRuleBuilderDialog.installAutocompletePopupKeyFilter(comboBox, event -> {});

			ListView<?> popup = (ListView<?>) ((ComboBoxListViewSkin<?>) comboBox.getSkin()).getPopupContent();
			popup.getSelectionModel().select(0);
			popup.getFocusModel().focus(0);
			comboBox.fireEvent(new javafx.event.Event(ComboBoxBase.ON_SHOWN));

			assertEquals(-1, popup.getSelectionModel().getSelectedIndex());
			assertEquals(-1, popup.getFocusModel().getFocusedIndex());
			assertEquals(0, ReplaceBlocksRuleBuilderDialog.popupSelectionTarget(
					KeyCode.DOWN, -1, comboBox.getItems().size(), 3));
		});
	}

	@Test
	void marqueeOverlaysGeneratedPreviewWithoutChangingBuilderLayout() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = new Stage();
			primaryStage.setScene(new Scene(new StackPane()));
			try {
				ReplaceBlocksRuleBuilderDialog dialog = new ReplaceBlocksRuleBuilderDialog(primaryStage, "");
				Field marqueeField = ReplaceBlocksRuleBuilderDialog.class.getDeclaredField("ruleMarquee");
				marqueeField.setAccessible(true);
				Rectangle marquee = (Rectangle) marqueeField.get(dialog);
				StackPane layer = assertInstanceOf(StackPane.class, dialog.getDialogPane().getContent());
				double initialWidth = layer.prefWidth(-1);
				double initialHeight = layer.prefHeight(-1);

				marquee.setWidth(2000);
				marquee.setHeight(1200);

				assertEquals(initialWidth, layer.prefWidth(-1));
				assertEquals(initialHeight, layer.prefHeight(-1));
				assertSame(layer, marquee.getParent());
				assertSame(marquee, layer.getChildren().getLast());
			} finally {
				primaryStage.close();
			}
		});
	}

	@Test
	void marqueeUsesSubtleBlueOverlayStyle() throws Exception {
		InputStream resource = ReplaceBlocksRuleBuilderDialog.class.getClassLoader()
				.getResourceAsStream("style/component/change-nbt-dialog.css");
		assertNotNull(resource);
		try (InputStream input = resource) {
			String css = new String(input.readAllBytes(), StandardCharsets.UTF_8);

			assertTrue(css.contains("-fx-fill: rgba(74, 163, 255, 0.12);"));
			assertTrue(css.contains("-fx-stroke: #4aa3ff;"));
			assertTrue(css.contains("-fx-stroke-width: 1;"));
		}
	}

	@Test
	void routesAutocompleteKeysBeforeComboBoxBehavior() {
		assertTrue(ReplaceBlocksRuleBuilderDialog.isAutocompletePopupKey(KeyCode.UP));
		assertTrue(ReplaceBlocksRuleBuilderDialog.isAutocompletePopupKey(KeyCode.DOWN));
		assertTrue(ReplaceBlocksRuleBuilderDialog.isAutocompletePopupKey(KeyCode.PAGE_UP));
		assertTrue(ReplaceBlocksRuleBuilderDialog.isAutocompletePopupKey(KeyCode.PAGE_DOWN));
		assertTrue(ReplaceBlocksRuleBuilderDialog.isAutocompletePopupKey(KeyCode.ENTER));
		assertTrue(ReplaceBlocksRuleBuilderDialog.isAutocompletePopupKey(KeyCode.TAB));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isAutocompletePopupKey(KeyCode.LEFT));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isAutocompletePopupKey(KeyCode.A));
	}

	@Test
	void startsMarqueeOnlyAfterARealDrag() {
		assertFalse(ReplaceBlocksRuleBuilderDialog.isRuleMarqueeDrag(0, 0));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isRuleMarqueeDrag(2, 1));
		assertTrue(ReplaceBlocksRuleBuilderDialog.isRuleMarqueeDrag(3, 0));
		assertTrue(ReplaceBlocksRuleBuilderDialog.isRuleMarqueeDrag(0, 3));
	}

	@Test
	void recognizesWhenLinuxJavaFxControlTestsHaveNoDisplay() {
		assertFalse(supportsJavaFxControlTests("Linux", null, null));
		assertFalse(supportsJavaFxControlTests("Linux", "", ""));
		assertTrue(supportsJavaFxControlTests("Linux", ":99", null));
		assertTrue(supportsJavaFxControlTests("Windows 11", null, null));
	}

	private static void runOnJavaFxThread(ThrowingRunnable action) throws Throwable {
		assumeTrue(supportsJavaFxControlTests(System.getProperty("os.name"),
				System.getenv("DISPLAY"), System.getenv("WAYLAND_DISPLAY")),
				"JavaFX controls need a display on Linux");
		initializeJavaFx();
		CountDownLatch completed = new CountDownLatch(1);
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Platform.runLater(() -> {
			try {
				action.run();
			} catch (Throwable ex) {
				failure.set(ex);
			} finally {
				completed.countDown();
			}
		});
		assertTrue(completed.await(10, TimeUnit.SECONDS));
		if (failure.get() != null) {
			throw failure.get();
		}
	}

	private static boolean supportsJavaFxControlTests(String osName, String display, String waylandDisplay) {
		return osName == null || !osName.toLowerCase().contains("linux")
				|| display != null && !display.isBlank()
				|| waylandDisplay != null && !waylandDisplay.isBlank();
	}

	private static void initializeJavaFx() throws InterruptedException {
		synchronized (javaFxStartupLock) {
			if (javaFxStarted) {
				return;
			}
			CountDownLatch initialized = new CountDownLatch(1);
			try {
				Platform.startup(initialized::countDown);
				assertTrue(initialized.await(10, TimeUnit.SECONDS));
			} catch (IllegalStateException ex) {
				// Another test class may already have initialized the toolkit.
			}
			javaFxStarted = true;
		}
	}

	@FunctionalInterface
	private interface ThrowingRunnable {
		void run() throws Throwable;
	}
}
