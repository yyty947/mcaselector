package net.querz.mcaselector.ui.dialog;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.changer.fields.ReplaceBlocksField;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.UIFactory;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.mapping.blockstate.BlockStateCatalog;
import net.querz.mcaselector.version.mapping.registry.BiomeRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReplaceBlocksRuleBuilderDialog extends Dialog<String> {

	private static final PseudoClass error = PseudoClass.getPseudoClass("error");
	private static final PseudoClass warning = PseudoClass.getPseudoClass("warning");
	private static final ButtonType HELP = new ButtonType("", ButtonBar.ButtonData.LEFT);
	private static final Color PROPERTY_CHOICE_TEXT = Color.web("#f2f2f2");
	private static final Pattern RESOURCE_LOCATION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

	private final Stage primaryStage;
	private final BlockStateCatalog catalog = BlockStateCatalog.latestJava();
	private final ObservableList<String> blockNames = FXCollections.observableArrayList(catalog.blockNames().stream().sorted().collect(Collectors.toList()));
	private final ObservableList<String> biomeCatalogNames = FXCollections.observableArrayList(BiomeRegistry.names().stream().sorted().collect(Collectors.toList()));
	private final BlockInput from = new BlockInput(true);
	private final BlockInput to = new BlockInput(false);
	private final ComboBox<ReplaceBlocksPreset> presets = new ComboBox<>();
	private final TableView<Rule> rules = new TableView<>();
	private final ObservableList<Rule> ruleItems = FXCollections.observableArrayList();
	private final TextField result = new TextField();
	private final Label validation = new Label();

	public ReplaceBlocksRuleBuilderDialog(Stage primaryStage, String initialValue) {
		this.primaryStage = primaryStage;
		titleProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_TITLE.getProperty());
		initStyle(StageStyle.UTILITY);
		setResizable(true);

		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getStylesheets().add(Objects.requireNonNull(ChangeNBTDialog.class.getClassLoader().getResource("style/component/change-nbt-dialog.css")).toExternalForm());
		getDialogPane().getStyleClass().add("replace-blocks-builder-pane");
		getDialogPane().getButtonTypes().addAll(HELP, ButtonType.OK, ButtonType.CANCEL);
		getDialogPane().setPrefSize(900, 650);
		Node help = getDialogPane().lookupButton(HELP);
		if (help instanceof Button button) {
			button.textProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_BUTTON.getProperty());
			button.addEventFilter(ActionEvent.ACTION, event -> {
				showHelp();
				event.consume();
			});
		}
		Node ok = getDialogPane().lookupButton(ButtonType.OK);
		ok.setDisable(true);

		setResultConverter(p -> p == ButtonType.OK ? result.getText() : null);

		Button add = UIFactory.button(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_ADD_RULE);
		add.setOnAction(this::addRule);
		VBox addActions = new VBox();
		addActions.getStyleClass().add("replace-blocks-builder-add-actions");
		Label addSpacer = new Label(" ");
		addActions.getChildren().addAll(addSpacer, add);

		GridPane input = new GridPane();
		input.getStyleClass().add("replace-blocks-builder-input");
		input.setHgap(8);
		input.setVgap(6);
		input.add(from, 0, 0);
		input.add(to, 1, 0);
		input.add(addActions, 2, 0);
		GridPane.setHgrow(from, Priority.ALWAYS);
		GridPane.setHgrow(to, Priority.ALWAYS);
		GridPane.setValignment(addActions, VPos.TOP);

		presets.setItems(FXCollections.observableArrayList(ReplaceBlocksPreset.values()));
		presets.setMaxWidth(Double.MAX_VALUE);
		presets.promptTextProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_PROMPT.getProperty());
		Button applyPreset = UIFactory.button(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_APPLY);
		applyPreset.disableProperty().bind(presets.valueProperty().isNull());
		applyPreset.setOnAction(this::applyPreset);
		GridPane presetInput = new GridPane();
		presetInput.getStyleClass().add("replace-blocks-builder-presets");
		presetInput.setHgap(8);
		Label presetLabel = UIFactory.label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET);
		presetInput.add(presetLabel, 0, 0);
		presetInput.add(presets, 1, 0);
		presetInput.add(applyPreset, 2, 0);
		GridPane.setHgrow(presets, Priority.ALWAYS);

		TableColumn<Rule, String> fromColumn = new TableColumn<>();
		fromColumn.textProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_FROM.getProperty());
		fromColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().from()));
		fromColumn.setSortable(false);

		TableColumn<Rule, String> toColumn = new TableColumn<>();
		toColumn.textProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_TO.getProperty());
		toColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().to()));
		toColumn.setSortable(false);

		rules.setItems(ruleItems);
		rules.getStyleClass().add("replace-blocks-builder-rules-table");
		rules.setPlaceholder(new Label());
		rules.getColumns().add(fromColumn);
		rules.getColumns().add(toColumn);
		rules.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		rules.setMinHeight(150);
		rules.setPrefHeight(220);
		ruleItems.addListener((ListChangeListener<Rule>) c -> updateRulesTableHeight());
		updateRulesTableHeight();

		Button delete = UIFactory.button(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_DELETE_RULE);
		delete.disableProperty().bind(rules.getSelectionModel().selectedItemProperty().isNull());
		delete.setOnAction(e -> {
			ruleItems.remove(rules.getSelectionModel().getSelectedItem());
			updateResult();
		});

		result.setEditable(false);
		result.setFocusTraversable(false);

		validation.getStyleClass().add("replace-blocks-builder-validation");
		validation.setText("");

		VBox content = new VBox();
		content.getStyleClass().add("replace-blocks-builder");
		content.setSpacing(6);
		content.setPadding(new Insets(4));
		Label advanced = UIFactory.label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_ADVANCED);
		advanced.visibleProperty().bind(from.userInputPresentProperty().or(to.userInputPresentProperty()).not());
		advanced.managedProperty().bind(advanced.visibleProperty());
		Label rulesLabel = UIFactory.label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_RULES);
		Label resultLabel = UIFactory.label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_RESULT);
		content.getChildren().addAll(presetInput, input, rulesLabel, rules, delete, resultLabel, result, validation, advanced);
		VBox.setMargin(rulesLabel, new Insets(8, 0, 0, 0));
		VBox.setVgrow(rules, Priority.ALWAYS);
		getDialogPane().setContent(content);

		loadSimpleRules(initialValue);
		updateResult();
	}

	private void applyPreset(ActionEvent event) {
		ReplaceBlocksPreset preset = presets.getValue();
		if (preset == null) {
			return;
		}
		from.applyPreset(preset.source(), preset.tileMode());
		to.applyPreset(preset.target(), SourceTileMode.ANY);
		if (preset.warning() == null) {
			updateResult();
		} else {
			showDiagnostic(new ReplaceBlocksDiagnostics.Diagnostic(
					ReplaceBlocksDiagnostics.Severity.WARNING,
					preset.warning().toString()));
		}
		from.focusInput();
	}

	private void updateRulesTableHeight() {
		rules.setMaxHeight(ruleItems.isEmpty() ? 220 : Double.MAX_VALUE);
	}

	private void addRule(ActionEvent event) {
		ValueResult fromName = from.value();
		if (fromName.diagnostic().isError()) {
			showDiagnostic(fromName.diagnostic());
			return;
		}
		ValueResult toName = to.value();
		if (toName.diagnostic().isError()) {
			showDiagnostic(toName.diagnostic());
			return;
		}
		if (hasRule(fromName.value(), toName.value())) {
			showDiagnostic(ReplaceBlocksDiagnostics.builderDuplicate());
			return;
		}
		ruleItems.add(new Rule(fromName.value(), toName.value()));
		from.focusInput();
		updateResult();
	}

	private boolean hasRule(String from, String to) {
		return ruleItems.stream()
				.anyMatch(rule -> rule.from().equals(from) && rule.to().equals(to));
	}

	private void loadSimpleRules(String initialValue) {
		if (initialValue == null || initialValue.isBlank()) {
			return;
		}
		ObservableList<Rule> parsed = FXCollections.observableArrayList();
		ReplaceBlocksField field = new ReplaceBlocksField();
		if (!ReplaceBlocksDiagnostics.parseReplaceBlocksValue(field, initialValue)) {
			return;
		}
		for (Map.Entry<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> rule : field.getNewValue().entrySet()) {
			if (rule.getValue().getType() == ChunkFilter.BlockReplaceType.NAME_TILE
					|| rule.getValue().getType() == ChunkFilter.BlockReplaceType.STATE_TILE) {
				return;
			}
			parsed.add(new Rule(rule.getKey().toString(), rule.getValue().toString()));
		}
		ruleItems.addAll(parsed);
	}

	private void updateResult() {
		if (ruleItems.isEmpty()) {
			result.clear();
			validation.textProperty().unbind();
			validation.setText("");
			validation.pseudoClassStateChanged(error, false);
			validation.pseudoClassStateChanged(warning, false);
			getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
			return;
		}
		StringBuilder builder = new StringBuilder();
		for (Rule rule : ruleItems) {
			if (!builder.isEmpty()) {
				builder.append(", ");
			}
			builder.append(formatFrom(rule.from())).append("=").append(formatTo(rule.to()));
		}
		result.setText(builder.toString());
		ReplaceBlocksField field = new ReplaceBlocksField();
		boolean valid = ReplaceBlocksDiagnostics.parseReplaceBlocksValue(field, result.getText());
		getDialogPane().lookupButton(ButtonType.OK).setDisable(!valid);
		showDiagnostic(ReplaceBlocksDiagnostics.diagnoseValue(result.getText(), valid));
	}

	private void showHelp() {
		Dialog<Void> help = new Dialog<>();
		help.initOwner(getDialogPane().getScene().getWindow());
		help.initStyle(StageStyle.UTILITY);
		help.setResizable(true);
		help.titleProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_TITLE.getProperty());
		help.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
		help.getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		help.getDialogPane().getStylesheets().add(Objects.requireNonNull(ChangeNBTDialog.class.getClassLoader().getResource("style/component/change-nbt-dialog.css")).toExternalForm());
		help.getDialogPane().getStyleClass().add("replace-blocks-builder-help-pane");
		help.getDialogPane().setPrefSize(560, 380);

		VBox content = new VBox(8);
		content.getStyleClass().add("replace-blocks-builder-help");
		content.setPadding(new Insets(8));
		content.getChildren().addAll(
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_NBT_TITLE, "replace-blocks-builder-help-heading"),
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_NBT_INTRO, "replace-blocks-builder-help-text"),
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_NBT_ANY, "replace-blocks-builder-help-text"),
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_NBT_PRESENT, "replace-blocks-builder-help-text"),
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_NBT_ABSENT, "replace-blocks-builder-help-text"),
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_NBT_NOTE, "replace-blocks-builder-help-text"),
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_BIOME_TITLE, "replace-blocks-builder-help-heading"),
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_BIOME_INTRO, "replace-blocks-builder-help-text"),
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_BIOME_SYNTAX, "replace-blocks-builder-help-text")
		);

		ScrollPane scroll = new ScrollPane(content);
		scroll.setFitToWidth(true);
		scroll.getStyleClass().add("replace-blocks-builder-help-scroll");
		help.getDialogPane().setContent(scroll);
		help.showAndWait();
	}

	private static Label helpLabel(Translation translation, String styleClass) {
		Label label = UIFactory.label(translation);
		label.setWrapText(true);
		label.getStyleClass().add(styleClass);
		return label;
	}

	private String formatFrom(String name) {
		if (name.startsWith("{") || isSourceModeExpression(name)) {
			return name;
		}
		return "literal(" + formatWrapperArgument(name) + ")";
	}

	private String formatTo(String name) {
		if (name.startsWith("{")) {
			return name;
		}
		if (name.startsWith("minecraft:")) {
			return name;
		}
		return "{Name:\"" + name + "\"}";
	}

	private static boolean isSourceModeExpression(String value) {
		return value.startsWith("literal(") || value.startsWith("regex(") || value.startsWith("props(")
				|| value.startsWith("tile(") || value.startsWith("no_tile(") || value.startsWith("y(") || value.startsWith("biome(");
	}

	private static boolean isTileEntityModeExpression(String value) {
		return value.startsWith("tile(") || value.startsWith("no_tile(");
	}

	private static boolean isYRangeExpression(String value) {
		return value.startsWith("y(");
	}

	private static boolean isBiomeExpression(String value) {
		return value.startsWith("biome(");
	}

	private static String formatWrapperArgument(String value) {
		if (!value.equals(value.trim()) || value.contains(")")) {
			return "'" + value + "'";
		}
		return value;
	}

	private void showDiagnostic(ReplaceBlocksDiagnostics.Diagnostic diagnostic) {
		validation.textProperty().unbind();
		validation.pseudoClassStateChanged(error, false);
		validation.pseudoClassStateChanged(warning, false);
		if (diagnostic.isNone()) {
			validation.setText("");
			return;
		}
		validation.setText(diagnostic.message());
		validation.pseudoClassStateChanged(diagnostic.isError() ? error : warning, true);
	}

	private static String blockStateSNBT(String name, Map<String, String> properties) {
		StringBuilder builder = new StringBuilder("{Name:\"").append(name).append("\"");
		if (!properties.isEmpty()) {
			builder.append(",Properties:{");
			boolean first = true;
			for (Map.Entry<String, String> property : properties.entrySet()) {
				if (!first) {
					builder.append(",");
				}
				builder.append(property.getKey()).append(":\"").append(property.getValue()).append("\"");
				first = false;
			}
			builder.append("}");
		}
		builder.append("}");
		return builder.toString();
	}

	private class BlockInput extends VBox {

		private final boolean source;
		private final ComboBox<String> block = new ComboBox<>();
		private final FilteredList<String> blockSuggestions = new FilteredList<>(blockNames);
		private final ComboBox<SourceTileMode> tileEntityMode = new ComboBox<>();
		private final TextField minY = new TextField();
		private final TextField maxY = new TextField();
		private final ComboBox<String> biomeNames = new ComboBox<>();
		private final FilteredList<String> biomeSuggestions = new FilteredList<>(biomeCatalogNames);
		private final GridPane properties = new GridPane();
		private final Map<String, ComboBox<PropertyChoice>> propertyEditors = new LinkedHashMap<>();
		private final BooleanProperty userInputPresent = new SimpleBooleanProperty(false);
		private boolean updatingItems;
		private boolean updatingBiomeItems;
		private boolean suppressSuggestions;
		private boolean suppressBiomeSuggestions;
		private boolean normalizeNextTyped;
		private boolean normalizeBiomeNextTyped;
		private int normalizedBiomeCaret = -1;

		private BlockInput(boolean source) {
			this.source = source;
			getStyleClass().add("replace-blocks-builder-block");
			setSpacing(4);

			Label label = UIFactory.label(source
					? Translation.DIALOG_REPLACE_BLOCKS_BUILDER_FROM
					: Translation.DIALOG_REPLACE_BLOCKS_BUILDER_TO);

			block.setEditable(true);
			block.setMaxWidth(Double.MAX_VALUE);
			block.setVisibleRowCount(12);
			block.setItems(blockSuggestions);
			block.setCellFactory(v -> new HighlightedBlockCell());
			block.getEditor().setAlignment(Pos.CENTER);
			block.getEditor().setOnAction(ReplaceBlocksRuleBuilderDialog.this::addRule);
			block.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
			block.getEditor().addEventFilter(KeyEvent.KEY_TYPED, this::handleKeyTyped);
			block.getEditor().addEventFilter(MouseEvent.MOUSE_PRESSED, e -> normalizeNextTyped = false);
			block.getEditor().textProperty().addListener((a, o, n) -> {
				if (suppressSuggestions) {
					return;
				}
				updateUserInputPresent();
				if (!updatingItems) {
					updateBlockSuggestions(n);
					rebuildProperties(n);
					showBlockSuggestions(n);
				}
			});
			block.valueProperty().addListener((a, o, n) -> {
				if (!updatingItems && !suppressSuggestions && n != null) {
					if (blockNames.contains(n)) {
						Platform.runLater(() -> {
							if (!suppressSuggestions && Objects.equals(block.getValue(), n)) {
								acceptSelectedValue(n);
							}
						});
					} else {
						rebuildProperties(n);
					}
				}
			});

			properties.getStyleClass().add("replace-blocks-builder-properties");
			properties.setHgap(6);
			properties.setVgap(4);

			getChildren().addAll(label, block);
			if (source) {
				tileEntityMode.setItems(FXCollections.observableArrayList(SourceTileMode.values()));
				tileEntityMode.setValue(SourceTileMode.ANY);
				tileEntityMode.setMaxWidth(Double.MAX_VALUE);
				getChildren().add(tileEntityMode);

				GridPane yRange = new GridPane();
				yRange.getStyleClass().add("replace-blocks-builder-y-range");
				yRange.setHgap(6);
				Label minLabel = UIFactory.label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_Y_MIN);
				Label maxLabel = UIFactory.label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_Y_MAX);
				minY.promptTextProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_Y_MIN.getProperty());
				maxY.promptTextProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_Y_MAX.getProperty());
				minY.setMaxWidth(Double.MAX_VALUE);
				maxY.setMaxWidth(Double.MAX_VALUE);
				minY.textProperty().addListener((a, o, n) -> updateUserInputPresent());
				maxY.textProperty().addListener((a, o, n) -> updateUserInputPresent());
				yRange.add(minLabel, 0, 0);
				yRange.add(minY, 1, 0);
				yRange.add(maxLabel, 2, 0);
				yRange.add(maxY, 3, 0);
				GridPane.setHgrow(minY, Priority.ALWAYS);
				GridPane.setHgrow(maxY, Priority.ALWAYS);
				getChildren().add(yRange);

				GridPane biomeFilter = new GridPane();
				biomeFilter.getStyleClass().add("replace-blocks-builder-biome");
				biomeFilter.setHgap(6);
				Label biomeLabel = UIFactory.label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_BIOME);
				biomeNames.setEditable(true);
				biomeNames.setMaxWidth(Double.MAX_VALUE);
				biomeNames.setVisibleRowCount(12);
				biomeNames.setItems(biomeSuggestions);
				biomeNames.setCellFactory(v -> new HighlightedBiomeCell());
				biomeNames.getEditor().promptTextProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_BIOME_PROMPT.getProperty());
				biomeNames.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, this::handleBiomeKeyPressed);
				biomeNames.getEditor().addEventFilter(KeyEvent.KEY_TYPED, this::handleBiomeKeyTyped);
				biomeNames.getEditor().addEventFilter(MouseEvent.MOUSE_PRESSED, e -> normalizeBiomeNextTyped = false);
				biomeNames.getEditor().textProperty().addListener((a, o, n) -> {
					if (suppressBiomeSuggestions) {
						return;
					}
					updateUserInputPresent();
					if (!updatingBiomeItems) {
						Platform.runLater(() -> {
							if (!suppressBiomeSuggestions) {
								updateBiomeSuggestions();
								showBiomeSuggestions();
							}
						});
					}
				});
				biomeNames.valueProperty().addListener((a, o, n) -> {
					if (!updatingBiomeItems && !suppressBiomeSuggestions && n != null && biomeCatalogNames.contains(n)) {
						Platform.runLater(() -> {
							if (!suppressBiomeSuggestions && Objects.equals(biomeNames.getValue(), n)) {
								completeBiomeSuggestion(n);
							}
						});
					}
				});
				biomeFilter.add(biomeLabel, 0, 0);
				biomeFilter.add(biomeNames, 1, 0);
				GridPane.setHgrow(biomeNames, Priority.ALWAYS);
				getChildren().add(biomeFilter);
			}
			getChildren().add(properties);
			VBox.setVgrow(properties, Priority.NEVER);
			updateBlockSuggestions("");
			updateBiomeSuggestions();
			rebuildProperties("");
		}

		private BooleanProperty userInputPresentProperty() {
			return userInputPresent;
		}

		private ValueResult value() {
			String generated = generatedValue();
			if (generated == null) {
				return new ValueResult(null, ReplaceBlocksDiagnostics.builderInvalid());
			}
			if (source && isSourceModeExpression(generated)) {
				ReplaceBlocksField field = new ReplaceBlocksField();
				boolean valid = ReplaceBlocksDiagnostics.parseReplaceBlocksValue(field, generated + "=minecraft:stone");
				return new ValueResult(generated, valid ? ReplaceBlocksDiagnostics.none() : ReplaceBlocksDiagnostics.builderInvalid());
			}
			ReplaceBlocksDiagnostics.NameResult normalized = ReplaceBlocksDiagnostics.normalizeBuilderValue(generated, source);
			return new ValueResult(normalized.name(), normalized.diagnostic());
		}

		private String generatedValue() {
			String text = currentText();
			if (text.isEmpty()) {
				return text;
			}
			if (text.startsWith("{") || source && isSourceModeExpression(text)) {
				return source ? applySourceConditions(text) : text;
			}
			ReplaceBlocksDiagnostics.NameResult normalized = ReplaceBlocksDiagnostics.normalizeBuilderName(text, source);
			if (normalized.diagnostic().isError()) {
				return text;
			}
			String name = normalized.name();
			if (propertyEditors.isEmpty()) {
				String value = source ? "literal(" + formatWrapperArgument(name) + ")" : name;
				return source ? applySourceConditions(value) : value;
			}
			Map<String, String> selectedProperties = new LinkedHashMap<>();
			for (Map.Entry<String, ComboBox<PropertyChoice>> property : propertyEditors.entrySet()) {
				PropertyChoice choice = property.getValue().getValue();
				if (choice == null) {
					return null;
				}
				if (choice.all()) {
					continue;
				}
				if (!catalog.isValidPropertyValue(name, property.getKey(), choice.value())) {
					return null;
				}
				selectedProperties.put(property.getKey(), choice.value());
			}
			String value;
			if (selectedProperties.isEmpty()) {
				value = source ? "literal(" + formatWrapperArgument(name) + ")" : name;
			} else {
				String blockState = blockStateSNBT(name, selectedProperties);
				value = source ? "props(" + blockState + ")" : blockState;
			}
			return source ? applySourceConditions(value) : value;
		}

		private String applySourceConditions(String value) {
			return applySourceBiome(applySourceYRange(applySourceTileMode(value)));
		}

		private String applySourceTileMode(String value) {
			if (!source || value == null || value.isEmpty() || isTileEntityModeExpression(value)) {
				return value;
			}
			return tileEntityMode.getValue().wrap(value);
		}

		private String applySourceYRange(String value) {
			if (!source || value == null || value.isEmpty() || isYRangeExpression(value)) {
				return value;
			}
			String range = yRangeText();
			if (range == null) {
				return value;
			}
			if (range.isEmpty()) {
				return null;
			}
			return "y(" + range + ", " + value + ")";
		}

		private String yRangeText() {
			String min = minY.getText() == null ? "" : minY.getText().trim();
			String max = maxY.getText() == null ? "" : maxY.getText().trim();
			if (min.isEmpty() && max.isEmpty()) {
				return null;
			}
			try {
				Integer minValue = min.isEmpty() ? null : Integer.parseInt(min);
				Integer maxValue = max.isEmpty() ? null : Integer.parseInt(max);
				if (minValue != null && maxValue != null && minValue > maxValue) {
					return "";
				}
				return min + ".." + max;
			} catch (NumberFormatException ex) {
				return "";
			}
		}

		private String applySourceBiome(String value) {
			if (!source || value == null || value.isEmpty() || isBiomeExpression(value)) {
				return value;
			}
			String biomes = biomeText();
			if (biomes == null) {
				return value;
			}
			if (biomes.isEmpty()) {
				return null;
			}
			return "biome(" + biomes + ", " + value + ")";
		}

		private String biomeText() {
			String raw = biomeNames.getEditor().getText() == null ? "" : biomeNames.getEditor().getText().trim();
			if (raw.isEmpty()) {
				return null;
			}
			String[] parts = raw.split(";", -1);
			List<String> normalized = new ArrayList<>(parts.length);
			for (String part : parts) {
				String biome = part.trim();
				if (biome.isEmpty()) {
					return "";
				}
				if (!biome.contains(":")) {
					biome = "minecraft:" + biome;
				}
				if (biome.startsWith("minecraft:") && !BiomeRegistry.isValidName(biome)
						|| !biome.startsWith("minecraft:") && !RESOURCE_LOCATION.matcher(biome).matches()) {
					return "";
				}
				normalized.add(biome);
			}
			return String.join(";", normalized);
		}

		private void clear() {
			suppressSuggestions = true;
			try {
				block.setValue(null);
				block.getEditor().clear();
				minY.clear();
				maxY.clear();
				clearBiomeInput();
				propertyEditors.clear();
				properties.getChildren().clear();
				updateBlockSuggestions("");
				updateBiomeSuggestions();
				userInputPresent.set(false);
			} finally {
				suppressSuggestions = false;
			}
		}

		private void applyPreset(String text, SourceTileMode sourceTileMode) {
			suppressSuggestions = true;
			try {
				String value = text == null ? "" : text;
				block.setValue(null);
				block.getSelectionModel().clearSelection();
				block.getEditor().setText(value);
				block.hide();
				updateBlockSuggestions(value);
				rebuildProperties(value);
				normalizeNextTyped = false;
				collapseEditorCaretToEnd();
				if (source) {
					tileEntityMode.setValue(sourceTileMode == null ? SourceTileMode.ANY : sourceTileMode);
					minY.clear();
					maxY.clear();
					clearBiomeInput();
				}
				updateUserInputPresent();
			} finally {
				suppressSuggestions = false;
			}
		}

		private void focusInput() {
			block.requestFocus();
			block.getEditor().requestFocus();
		}

		private String currentText() {
			String text = block.getEditor().getText();
			return text == null ? "" : text.trim();
		}

		private void updateUserInputPresent() {
			userInputPresent.set(!currentText().isEmpty()
					|| minY.getText() != null && !minY.getText().isBlank()
					|| maxY.getText() != null && !maxY.getText().isBlank()
					|| biomeNames.getEditor().getText() != null && !biomeNames.getEditor().getText().isBlank());
		}

		private void updateBlockSuggestions(String query) {
			String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
			updatingItems = true;
			try {
				if (text.isEmpty() || text.startsWith("{") || source && isSourceModeExpression(text)) {
					blockSuggestions.setPredicate(name -> false);
				} else {
					blockSuggestions.setPredicate(name -> matchesBlockSearch(name, text));
				}
			} finally {
				updatingItems = false;
			}
		}

		private void showBlockSuggestions(String query) {
			if (suppressSuggestions) {
				return;
			}
			String text = query == null ? "" : query.trim();
			if (text.isEmpty() || text.startsWith("{") || source && isSourceModeExpression(text) || block.getItems().isEmpty()) {
				block.hide();
				return;
			}
			block.show();
		}

		private void updateBiomeSuggestions() {
			String query = currentBiomeQuery().toLowerCase(Locale.ROOT);
			updatingBiomeItems = true;
			try {
				if (query.isEmpty()) {
					biomeSuggestions.setPredicate(name -> false);
				} else {
					biomeSuggestions.setPredicate(name -> matchesBlockSearch(name, query));
				}
			} finally {
				updatingBiomeItems = false;
			}
		}

		private void showBiomeSuggestions() {
			if (suppressBiomeSuggestions) {
				return;
			}
			if (currentBiomeQuery().isEmpty() || biomeNames.getItems().isEmpty()) {
				biomeNames.hide();
				return;
			}
			biomeNames.show();
		}

		private void handleBiomeKeyPressed(KeyEvent event) {
			if (event.getCode() != KeyCode.TAB || !biomeNames.isShowing()) {
				if (isCaretControl(event)) {
					normalizeBiomeNextTyped = false;
				}
				return;
			}
			String suggestion = selectedBiomeSuggestion();
			if (suggestion == null) {
				return;
			}
			completeBiomeSuggestion(suggestion);
			event.consume();
		}

		private void handleBiomeKeyTyped(KeyEvent event) {
			if (!normalizeBiomeNextTyped) {
				return;
			}
			normalizeBiomeNextTyped = false;
			collapseBiomeCaret();
		}

		private String selectedBiomeSuggestion() {
			String selected = biomeNames.getSelectionModel().getSelectedItem();
			if (selected != null && biomeNames.getItems().contains(selected)) {
				return selected;
			}
			return biomeNames.getItems().isEmpty() ? null : biomeNames.getItems().get(0);
		}

		private void completeBiomeSuggestion(String suggestion) {
			TextField editor = biomeNames.getEditor();
			String text = editor.getText();
			if (text == null) {
				text = "";
			}
			int caret = Math.max(0, Math.min(editor.getCaretPosition(), text.length()));
			int start = text.lastIndexOf(';', Math.max(0, caret - 1)) + 1;
			int end = text.indexOf(';', caret);
			if (end < 0) {
				end = text.length();
			}
			String completed = text.substring(0, start) + suggestion + text.substring(end);
			int caretPosition = start + suggestion.length();

			suppressBiomeSuggestions = true;
			try {
				biomeNames.setValue(null);
				biomeNames.getSelectionModel().clearSelection();
				editor.setText(completed);
				editor.selectRange(caretPosition, caretPosition);
				updateUserInputPresent();
				updateBiomeSuggestions();
				biomeNames.hide();
				normalizedBiomeCaret = caretPosition;
				normalizeBiomeNextTyped = true;
				collapseBiomeCaretLater();
			} finally {
				suppressBiomeSuggestions = false;
			}
		}

		private String currentBiomeQuery() {
			TextField editor = biomeNames.getEditor();
			String text = editor.getText();
			if (text == null || text.isEmpty()) {
				return "";
			}
			int caret = Math.max(0, Math.min(editor.getCaretPosition(), text.length()));
			int start = text.lastIndexOf(';', Math.max(0, caret - 1)) + 1;
			int end = text.indexOf(';', caret);
			if (end < 0) {
				end = text.length();
			}
			return text.substring(start, end).trim();
		}

		private void collapseBiomeCaret() {
			if (normalizedBiomeCaret < 0) {
				return;
			}
			TextField editor = biomeNames.getEditor();
			int caret = Math.min(normalizedBiomeCaret, editor.getLength());
			editor.selectRange(caret, caret);
		}

		private void collapseBiomeCaretLater() {
			Platform.runLater(() -> {
				if (normalizeBiomeNextTyped) {
					collapseBiomeCaret();
				}
			});
		}

		private void clearBiomeInput() {
			suppressBiomeSuggestions = true;
			try {
				biomeNames.setValue(null);
				biomeNames.getSelectionModel().clearSelection();
				biomeNames.getEditor().clear();
				biomeNames.hide();
				normalizeBiomeNextTyped = false;
				normalizedBiomeCaret = -1;
			} finally {
				suppressBiomeSuggestions = false;
			}
		}

		private void handleKeyPressed(KeyEvent event) {
			if (event.getCode() != KeyCode.TAB || !block.isShowing()) {
				if (isCaretControl(event)) {
					normalizeNextTyped = false;
				}
				return;
			}
			String suggestion = selectedSuggestion();
			if (suggestion == null) {
				return;
			}
			completeSuggestion(suggestion);
			event.consume();
		}

		private boolean isCaretControl(KeyEvent event) {
			return event.isShortcutDown() || event.isShiftDown()
					|| event.getCode() == KeyCode.LEFT
					|| event.getCode() == KeyCode.RIGHT
					|| event.getCode() == KeyCode.HOME
					|| event.getCode() == KeyCode.END
					|| event.getCode() == KeyCode.BACK_SPACE
					|| event.getCode() == KeyCode.DELETE;
		}

		private void handleKeyTyped(KeyEvent event) {
			if (!normalizeNextTyped) {
				return;
			}
			normalizeNextTyped = false;
			collapseEditorCaretToEnd();
		}

		private String selectedSuggestion() {
			String selected = block.getSelectionModel().getSelectedItem();
			if (selected != null && block.getItems().contains(selected)) {
				return selected;
			}
			return block.getItems().isEmpty() ? null : block.getItems().get(0);
		}

		private void acceptSelectedValue(String value) {
			commitEditorText(value);
		}

		private void completeSuggestion(String suggestion) {
			commitEditorText(suggestion);
		}

		private void commitEditorText(String text) {
			suppressSuggestions = true;
			try {
				block.setValue(null);
				block.getSelectionModel().clearSelection();
				block.getEditor().setText(text);
				updateUserInputPresent();
				updateBlockSuggestions(text);
				rebuildProperties(text);
				block.hide();
				collapseEditorCaretToEnd();
				collapseEditorCaretToEndLater();
				normalizeNextTyped = true;
			} finally {
				suppressSuggestions = false;
			}
		}

		private void collapseEditorCaretToEnd() {
			String text = block.getEditor().getText();
			if (text != null) {
				block.getEditor().selectRange(text.length(), text.length());
			}
		}

		private void collapseEditorCaretToEndLater() {
			Platform.runLater(() -> {
				if (normalizeNextTyped) {
					collapseEditorCaretToEnd();
				}
			});
		}

		private boolean matchesBlockSearch(String name, String query) {
			if (name.contains(query)) {
				return true;
			}
			int namespace = name.indexOf(':');
			return namespace >= 0 && name.substring(namespace + 1).contains(query);
		}

		private void rebuildProperties(String rawBlockName) {
			propertyEditors.clear();
			properties.getChildren().clear();

			String text = rawBlockName == null ? "" : rawBlockName.trim();
			if (text.isEmpty() || text.startsWith("{") || source && isSourceModeExpression(text)) {
				return;
			}

			String blockName = BlockStateCatalog.normalizeName(text);
			Map<String, List<String>> catalogProperties = catalog.properties(blockName);
			if (catalogProperties.isEmpty()) {
				return;
			}

			int row = 0;
			for (Map.Entry<String, List<String>> property : catalogProperties.entrySet()) {
				Label name = new Label(property.getKey());
				ObservableList<PropertyChoice> choices = FXCollections.observableArrayList();
				choices.add(PropertyChoice.allChoice());
				choices.addAll(property.getValue().stream().map(PropertyChoice::value).collect(Collectors.toList()));
				ComboBox<PropertyChoice> value = new ComboBox<>(choices);
				value.setMaxWidth(Double.MAX_VALUE);
				value.setCellFactory(v -> new PropertyChoiceCell());
				value.setButtonCell(new PropertyChoiceCell());
				value.setValue(PropertyChoice.allChoice());
				value.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
					if (!value.isShowing()) {
						Platform.runLater(value::show);
					}
				});
				propertyEditors.put(property.getKey(), value);
				properties.add(name, 0, row);
				properties.add(value, 1, row);
				GridPane.setHgrow(value, Priority.ALWAYS);
				row++;
			}
		}

		private class HighlightedBlockCell extends ListCell<String> {

			private HighlightedBlockCell() {
				addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
					if (!isEmpty() && getItem() != null) {
						completeSuggestion(getItem());
						event.consume();
					}
				});
			}

			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
					return;
				}
				setText(null);
				setGraphic(highlightedText(item, currentText()));
			}

			private TextFlow highlightedText(String item, String query) {
				TextFlow flow = new TextFlow();
				flow.getStyleClass().add("replace-blocks-builder-suggestion-text");
				String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
				if (needle.isEmpty()) {
					flow.getChildren().add(text(item, false));
					return flow;
				}

				String haystack = item.toLowerCase(Locale.ROOT);
				int offset = 0;
				int match;
				while ((match = haystack.indexOf(needle, offset)) >= 0) {
					if (match > offset) {
						flow.getChildren().add(text(item.substring(offset, match), false));
					}
					int end = match + needle.length();
					flow.getChildren().add(text(item.substring(match, end), true));
					offset = end;
				}
				if (offset < item.length()) {
					flow.getChildren().add(text(item.substring(offset), false));
				}
				if (flow.getChildren().isEmpty()) {
					flow.getChildren().add(text(item, false));
				}
				return flow;
			}

			private Text text(String value, boolean highlight) {
				Text text = new Text(value);
				text.getStyleClass().add(highlight ? "replace-blocks-builder-suggestion-highlight" : "replace-blocks-builder-suggestion-normal");
				return text;
			}
		}

		private class HighlightedBiomeCell extends ListCell<String> {

			private HighlightedBiomeCell() {
				addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
					if (!isEmpty() && getItem() != null) {
						completeBiomeSuggestion(getItem());
						event.consume();
					}
				});
			}

			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
					return;
				}
				setText(null);
				setGraphic(highlightedText(item, currentBiomeQuery()));
			}

			private TextFlow highlightedText(String item, String query) {
				TextFlow flow = new TextFlow();
				flow.getStyleClass().add("replace-blocks-builder-suggestion-text");
				String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
				if (needle.isEmpty()) {
					flow.getChildren().add(text(item, false));
					return flow;
				}

				String haystack = item.toLowerCase(Locale.ROOT);
				int offset = 0;
				int match;
				while ((match = haystack.indexOf(needle, offset)) >= 0) {
					if (match > offset) {
						flow.getChildren().add(text(item.substring(offset, match), false));
					}
					int end = match + needle.length();
					flow.getChildren().add(text(item.substring(match, end), true));
					offset = end;
				}
				if (offset < item.length()) {
					flow.getChildren().add(text(item.substring(offset), false));
				}
				if (flow.getChildren().isEmpty()) {
					flow.getChildren().add(text(item, false));
				}
				return flow;
			}

			private Text text(String value, boolean highlight) {
				Text text = new Text(value);
				text.getStyleClass().add(highlight ? "replace-blocks-builder-suggestion-highlight" : "replace-blocks-builder-suggestion-normal");
				return text;
			}
		}
	}

	private record ValueResult(String value, ReplaceBlocksDiagnostics.Diagnostic diagnostic) {}

	private record Rule(String from, String to) {}

	private enum ReplaceBlocksPreset {
		AIR_TO_STONE(
				Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_AIR_TO_STONE,
				"minecraft:air",
				"minecraft:stone",
				SourceTileMode.ANY,
				Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_WARNING_AIR),
		FLUIDS_TO_AIR(
				Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_FLUIDS_TO_AIR,
				"regex(minecraft:(water|lava))",
				"minecraft:air",
				SourceTileMode.ANY,
				null),
		LOGS_LEAVES_TO_AIR(
				Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_LOGS_LEAVES_TO_AIR,
				"regex(minecraft:.*_(log|wood|stem|hyphae|leaves))",
				"minecraft:air",
				SourceTileMode.ANY,
				null),
		ORES_TO_STONE(
				Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_ORES_TO_STONE,
				"regex(minecraft:.*_ore)",
				"minecraft:stone",
				SourceTileMode.ANY,
				null),
		CONTAINERS_TO_AIR(
				Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_CONTAINERS_TO_AIR,
				"regex(minecraft:.*(chest|barrel|furnace|smoker|hopper|dispenser|dropper|shulker_box))",
				"minecraft:air",
				SourceTileMode.REQUIRE,
				Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_WARNING_CONTAINERS);

		private final Translation label;
		private final String source;
		private final String target;
		private final SourceTileMode tileMode;
		private final Translation warning;

		ReplaceBlocksPreset(Translation label, String source, String target, SourceTileMode tileMode, Translation warning) {
			this.label = label;
			this.source = source;
			this.target = target;
			this.tileMode = tileMode;
			this.warning = warning;
		}

		private String source() {
			return source;
		}

		private String target() {
			return target;
		}

		private SourceTileMode tileMode() {
			return tileMode;
		}

		private Translation warning() {
			return warning;
		}

		@Override
		public String toString() {
			return label.toString();
		}
	}

	private record PropertyChoice(String value, boolean all) {

		private static PropertyChoice allChoice() {
			return new PropertyChoice(null, true);
		}

		private static PropertyChoice value(String value) {
			return new PropertyChoice(value, false);
		}

		@Override
		public String toString() {
			return all ? Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PROPERTY_ALL.toString() : value;
		}
	}

	private static class PropertyChoiceCell extends ListCell<PropertyChoice> {

		@Override
		protected void updateItem(PropertyChoice item, boolean empty) {
			super.updateItem(item, empty);
			setText(null);
			if (empty || item == null) {
				setGraphic(null);
				return;
			}
			Text text = new Text(item.toString());
			text.setFill(PROPERTY_CHOICE_TEXT);
			setGraphic(text);
		}
	}

	private enum SourceTileMode {
		ANY(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_TILE_SOURCE_ANY, null),
		REQUIRE(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_TILE_SOURCE_REQUIRE, "tile"),
		EXCLUDE(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_TILE_SOURCE_EXCLUDE, "no_tile");

		private final Translation label;
		private final String wrapper;

		SourceTileMode(Translation label, String wrapper) {
			this.label = label;
			this.wrapper = wrapper;
		}

		private String wrap(String value) {
			return wrapper == null ? value : wrapper + "(" + value + ")";
		}

		@Override
		public String toString() {
			return label.toString();
		}
	}
}
