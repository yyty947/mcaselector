package net.querz.mcaselector.ui.dialog;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.changer.fields.ReplaceBlocksField;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.UIFactory;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.mapping.blockstate.BlockStateCatalog;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReplaceBlocksRuleBuilderDialog extends Dialog<String> {

	private static final PseudoClass error = PseudoClass.getPseudoClass("error");
	private static final PseudoClass warning = PseudoClass.getPseudoClass("warning");
	private static final String DEFAULT_FROM_BLOCK = "minecraft:stone";
	private static final String DEFAULT_TO_BLOCK = "minecraft:dirt";

	private final BlockStateCatalog catalog = BlockStateCatalog.latestJava();
	private final ObservableList<String> blockNames = FXCollections.observableArrayList(catalog.blockNames().stream().sorted().collect(Collectors.toList()));
	private final BlockInput from = new BlockInput(true);
	private final BlockInput to = new BlockInput(false);
	private final TableView<Rule> rules = new TableView<>();
	private final ObservableList<Rule> ruleItems = FXCollections.observableArrayList();
	private final TextField result = new TextField();
	private final Label validation = new Label();

	public ReplaceBlocksRuleBuilderDialog(Stage primaryStage, String initialValue) {
		titleProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_TITLE.getProperty());
		initStyle(StageStyle.UTILITY);
		setResizable(true);

		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getStylesheets().add(Objects.requireNonNull(ChangeNBTDialog.class.getClassLoader().getResource("style/component/change-nbt-dialog.css")).toExternalForm());
		getDialogPane().getStyleClass().add("replace-blocks-builder-pane");
		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		getDialogPane().setPrefSize(900, 650);
		Node ok = getDialogPane().lookupButton(ButtonType.OK);
		ok.setDisable(true);

		setResultConverter(p -> p == ButtonType.OK ? result.getText() : null);

		Button add = UIFactory.button(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_ADD_RULE);
		add.setOnAction(this::addRule);

		GridPane input = new GridPane();
		input.getStyleClass().add("replace-blocks-builder-input");
		input.setHgap(8);
		input.setVgap(6);
		input.add(from, 0, 0);
		input.add(to, 1, 0);
		input.add(add, 2, 0);
		GridPane.setHgrow(from, Priority.ALWAYS);
		GridPane.setHgrow(to, Priority.ALWAYS);

		TableColumn<Rule, String> fromColumn = new TableColumn<>();
		fromColumn.textProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_FROM.getProperty());
		fromColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().from()));
		fromColumn.setSortable(false);

		TableColumn<Rule, String> toColumn = new TableColumn<>();
		toColumn.textProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_TO.getProperty());
		toColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().to()));
		toColumn.setSortable(false);

		rules.setItems(ruleItems);
		rules.setPlaceholder(new Label());
		rules.getColumns().add(fromColumn);
		rules.getColumns().add(toColumn);
		rules.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

		Button delete = UIFactory.button(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_DELETE_RULE);
		delete.disableProperty().bind(rules.getSelectionModel().selectedItemProperty().isNull());
		delete.setOnAction(e -> {
			ruleItems.remove(rules.getSelectionModel().getSelectedItem());
			updateResult();
		});

		result.setEditable(false);
		result.setFocusTraversable(false);

		validation.getStyleClass().add("replace-blocks-builder-validation");
		validation.textProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_EMPTY.getProperty());

		VBox content = new VBox();
		content.getStyleClass().add("replace-blocks-builder");
		content.setSpacing(6);
		content.setPadding(new Insets(4));
		Label advanced = UIFactory.label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_ADVANCED);
		Label rulesLabel = UIFactory.label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_RULES);
		Label resultLabel = UIFactory.label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_RESULT);
		content.getChildren().addAll(input, rulesLabel, rules, delete, resultLabel, result, validation, advanced);
		VBox.setVgrow(rules, Priority.ALWAYS);
		getDialogPane().setContent(content);

		loadSimpleRules(initialValue);
		if (initialValue == null || initialValue.isBlank()) {
			from.setText(DEFAULT_FROM_BLOCK);
			to.setText(DEFAULT_TO_BLOCK);
		}
		updateResult();
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
			validation.textProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_EMPTY.getProperty());
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
		return value.startsWith("literal(") || value.startsWith("regex(") || value.startsWith("props(");
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
		private final GridPane properties = new GridPane();
		private final Map<String, ComboBox<String>> propertyEditors = new LinkedHashMap<>();
		private boolean updatingItems;
		private boolean suppressSuggestions;

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
			block.setPromptText(source ? DEFAULT_FROM_BLOCK : DEFAULT_TO_BLOCK);
			block.setItems(blockNames);
			block.setCellFactory(v -> new HighlightedBlockCell());
			block.getEditor().setAlignment(Pos.CENTER);
			block.getEditor().setOnAction(ReplaceBlocksRuleBuilderDialog.this::addRule);
			block.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
			block.getEditor().textProperty().addListener((a, o, n) -> {
				if (!updatingItems) {
					updateBlockSuggestions(n);
					rebuildProperties(n);
					showBlockSuggestions(n);
				}
			});
			block.valueProperty().addListener((a, o, n) -> {
				if (!updatingItems && !suppressSuggestions && n != null) {
					if (blockNames.contains(n)) {
						completeSuggestion(n);
					} else {
						rebuildProperties(n);
					}
				}
			});

			properties.getStyleClass().add("replace-blocks-builder-properties");
			properties.setHgap(6);
			properties.setVgap(4);

			getChildren().addAll(label, block, properties);
			VBox.setVgrow(properties, Priority.NEVER);
			updateBlockSuggestions("");
			rebuildProperties("");
		}

		private void setText(String text) {
			suppressSuggestions = true;
			try {
				block.getEditor().setText(text);
				block.setValue(text);
				updateBlockSuggestions(text);
				rebuildProperties(text);
			} finally {
				suppressSuggestions = false;
			}
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
			if (text.isEmpty() || text.startsWith("{") || source && isSourceModeExpression(text)) {
				return text;
			}
			ReplaceBlocksDiagnostics.NameResult normalized = ReplaceBlocksDiagnostics.normalizeBuilderName(text, source);
			if (normalized.diagnostic().isError()) {
				return text;
			}
			String name = normalized.name();
			if (propertyEditors.isEmpty()) {
				return source ? "literal(" + formatWrapperArgument(name) + ")" : name;
			}
			Map<String, String> selectedProperties = new LinkedHashMap<>();
			for (Map.Entry<String, ComboBox<String>> property : propertyEditors.entrySet()) {
				String value = property.getValue().getValue();
				if (value == null || !catalog.isValidPropertyValue(name, property.getKey(), value)) {
					return null;
				}
				selectedProperties.put(property.getKey(), value);
			}
			String blockState = blockStateSNBT(name, selectedProperties);
			return source ? "props(" + blockState + ")" : blockState;
		}

		private void clear() {
			suppressSuggestions = true;
			try {
				block.setValue(null);
				block.getEditor().clear();
				propertyEditors.clear();
				properties.getChildren().clear();
				updateBlockSuggestions("");
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
			if ((text == null || text.isBlank()) && block.getValue() != null) {
				text = block.getValue();
			}
			return text == null ? "" : text.trim();
		}

		private void updateBlockSuggestions(String query) {
			String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
			ObservableList<String> suggestions;
			if (text.isEmpty()) {
				suggestions = blockNames;
			} else if (text.startsWith("{") || source && isSourceModeExpression(text)) {
				suggestions = FXCollections.observableArrayList();
			} else {
				suggestions = FXCollections.observableArrayList(blockNames.stream()
						.filter(name -> matchesBlockSearch(name, text))
						.collect(Collectors.toList()));
			}
			updatingItems = true;
			block.setItems(suggestions);
			updatingItems = false;
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

		private void handleKeyPressed(KeyEvent event) {
			if (event.getCode() != KeyCode.TAB || !block.isShowing()) {
				return;
			}
			String suggestion = selectedSuggestion();
			if (suggestion == null) {
				return;
			}
			completeSuggestion(suggestion);
			event.consume();
		}

		private String selectedSuggestion() {
			String selected = block.getSelectionModel().getSelectedItem();
			if (selected != null && block.getItems().contains(selected)) {
				return selected;
			}
			return block.getItems().isEmpty() ? null : block.getItems().get(0);
		}

		private void completeSuggestion(String suggestion) {
			suppressSuggestions = true;
			try {
				block.setValue(suggestion);
				block.getEditor().setText(suggestion);
				block.getEditor().positionCaret(suggestion.length());
				updateBlockSuggestions(suggestion);
				rebuildProperties(suggestion);
				block.hide();
			} finally {
				suppressSuggestions = false;
			}
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

			Map<String, String> defaults = catalog.defaultProperties(blockName);
			int row = 0;
			for (Map.Entry<String, List<String>> property : catalogProperties.entrySet()) {
				Label name = new Label(property.getKey());
				ComboBox<String> value = new ComboBox<>(FXCollections.observableArrayList(property.getValue()));
				value.setMaxWidth(Double.MAX_VALUE);
				value.setValue(defaults.getOrDefault(property.getKey(), property.getValue().isEmpty() ? null : property.getValue().get(0)));
				propertyEditors.put(property.getKey(), value);
				properties.add(name, 0, row);
				properties.add(value, 1, row);
				GridPane.setHgrow(value, Priority.ALWAYS);
				row++;
			}
		}

		private class HighlightedBlockCell extends ListCell<String> {

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
	}

	private record ValueResult(String value, ReplaceBlocksDiagnostics.Diagnostic diagnostic) {}

	private record Rule(String from, String to) {}
}
