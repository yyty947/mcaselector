package net.querz.mcaselector.ui.dialog;

import net.querz.mcaselector.changer.fields.ReplaceBlocksField;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.version.mapping.registry.BlockRegistry;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.NBTUtil;
import net.querz.nbt.Tag;
import net.querz.nbt.io.snbt.ParseException;
import net.querz.nbt.io.snbt.SNBTParser;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

final class ReplaceBlocksDiagnostics {

	private static final Pattern RESOURCE_LOCATION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
	private static final Pattern REGEX_META = Pattern.compile("[\\\\.^$|?*+()\\[\\]{}]");

	private ReplaceBlocksDiagnostics() {}

	static NameResult normalizeBuilderName(String raw, boolean source) {
		if (raw == null || raw.trim().isEmpty()) {
			return new NameResult(null, error(source
					? Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_EMPTY.toString()
					: Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_TARGET_EMPTY.toString()));
		}
		String name = stripOuterSingleQuotes(raw.trim());
		if (!name.contains(":")) {
			name = "minecraft:" + name;
		}
		if (name.startsWith("minecraft:")) {
			if (BlockRegistry.isValidName(name)) {
				return new NameResult(name, none());
			}
			return new NameResult(null, error((source
					? Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_UNKNOWN
					: Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_TARGET_UNKNOWN).format(name)));
		}
		if (RESOURCE_LOCATION.matcher(name).matches()) {
			return new NameResult(name, none());
		}
		return new NameResult(null, error(source
				? Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_FORMAT.toString()
				: Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_TARGET_FORMAT.toString()));
	}

	static NameResult normalizeBuilderValue(String raw, boolean source) {
		if (raw == null || raw.trim().isEmpty()) {
			return normalizeBuilderName(raw, source);
		}
		String value = raw.trim();
		if (!value.startsWith("{")) {
			return normalizeBuilderName(raw, source);
		}
		try {
			SNBTParser parser = new SNBTParser(value);
			Tag parsed = parser.parse(true);
			if (!(parsed instanceof CompoundTag state) || state.getString("Name") == null) {
				return new NameResult(null, error(source
						? Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_STATE_SNBT.toString()
						: Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_STATE_SNBT.toString()));
			}
			return new NameResult(NBTUtil.toSNBT(state), none());
		} catch (ParseException | RuntimeException ex) {
			return new NameResult(null, error(source
					? Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_STATE_SNBT.toString()
					: Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_STATE_SNBT.toString()));
		}
	}

	static boolean parseReplaceBlocksValue(ReplaceBlocksField field, String value) {
		try {
			return field.parseNewValue(value);
		} catch (RuntimeException ex) {
			field.setNewValue(null);
			return false;
		}
	}

	static Diagnostic builderInvalid() {
		return error(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_INVALID.toString());
	}

	static Diagnostic builderDuplicate() {
		return error(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_DUPLICATE.toString());
	}

	static Diagnostic diagnoseValue(String raw, boolean parserAccepted) {
		if (raw == null || raw.trim().isEmpty()) {
			return none();
		}
		Diagnostic parsed = diagnoseSyntax(raw);
		if (parsed.isError()) {
			return parsed;
		}
		if (!parserAccepted) {
			return error(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_INVALID.toString());
		}
		return parsed;
	}

	static boolean needsQueryQuoteHint(String query) {
		if (query == null) {
			return false;
		}
		String fieldName = "ReplaceBlocks";
		int fieldIndex = query.indexOf(fieldName);
		if (fieldIndex < 0) {
			return false;
		}
		int equalsIndex = query.indexOf('=', fieldIndex + fieldName.length());
		if (equalsIndex < 0) {
			return false;
		}
		int valueStart = equalsIndex + 1;
		while (valueStart < query.length() && Character.isWhitespace(query.charAt(valueStart))) {
			valueStart++;
		}
		return valueStart < query.length() && query.charAt(valueStart) != '"';
	}

	private static Diagnostic diagnoseSyntax(String raw) {
		String trimmed = raw.trim();
		Diagnostic warning = none();

		while (!trimmed.isEmpty()) {
			SourceResult source = readSource(trimmed);
			if (source.diagnostic().isError()) {
				return source.diagnostic();
			}
			if (warning.isNone() && source.diagnostic().isWarning()) {
				warning = source.diagnostic();
			}

			String to = trimmed.substring(source.read()).trim();
			if (!to.startsWith("=")) {
				return error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_MISSING_EQUALS.toString());
			}
			to = to.substring(1).trim();

			if (to.isEmpty()) {
				return error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_TARGET_EMPTY.toString());
			}
			TargetResult target = readTarget(to);
			if (target.diagnostic().isError()) {
				return target.diagnostic();
			}

			to = to.substring(target.read()).trim();
			if (to.startsWith(";")) {
				to = to.substring(1).trim();
				if (to.isEmpty()) {
					return error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_TILE_EMPTY.toString());
				}
				Tag tile;
				try {
					SNBTParser parser = new SNBTParser(to);
					tile = parser.parse(true);
					if (!(tile instanceof CompoundTag)) {
						return error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_TILE_SNBT.toString());
					}
					int readTile = parser.getReadChars();
					to = to.substring(readTile - 1).trim();
				} catch (ParseException | RuntimeException ex) {
					return error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_TILE_SNBT.toString());
				}
			}

			if (to.startsWith(",")) {
				trimmed = to.substring(1).trim();
			} else if (to.isEmpty()) {
				trimmed = "";
			} else {
				return error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_TRAILING.toString());
			}
		}

		return warning;
	}

	private static SourceResult readSource(String raw) {
		if (raw.startsWith("{")) {
			try {
				SNBTParser parser = new SNBTParser(raw);
				Tag state = parser.parse(true);
				if (!(state instanceof CompoundTag compound) || compound.getString("Name") == null) {
					return new SourceResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_STATE_SNBT.toString()));
				}
				return new SourceResult(parser.getReadChars() - 1, none());
			} catch (ParseException | RuntimeException ex) {
				return new SourceResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_STATE_SNBT.toString()));
			}
		}
		if (startsWithSourceWrapper(raw)) {
			return readWrappedSource(raw);
		}
		int equals = raw.indexOf('=');
		if (equals < 0) {
			return new SourceResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_MISSING_EQUALS.toString()));
		}

		String from = raw.substring(0, equals).trim();
		if (from.isEmpty()) {
			return new SourceResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_EMPTY.toString()));
		}
		return new SourceResult(equals, validateSource(from));
	}

	private static boolean startsWithSourceWrapper(String raw) {
		return raw.startsWith("literal(") || raw.startsWith("regex(") || raw.startsWith("props(")
				|| raw.startsWith("tile(") || raw.startsWith("no_tile(") || raw.startsWith("y(");
	}

	private static SourceResult readWrappedSource(String raw) {
		if (raw.startsWith("y(")) {
			return readYRangeWrapper(raw);
		}
		if (raw.startsWith("tile(")) {
			return readTileEntityModeWrapper(raw, "tile(");
		}
		if (raw.startsWith("no_tile(")) {
			return readTileEntityModeWrapper(raw, "no_tile(");
		}
		if (raw.startsWith("literal(")) {
			return readNameWrapper(raw, "literal(", true);
		}
		if (raw.startsWith("regex(")) {
			return readNameWrapper(raw, "regex(", false);
		}
		int end = findWrapperEnd(raw, "props(".length());
		if (end < 0) {
			return new SourceResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_MODE_WRAPPER.toString()));
		}
		CompoundTag state = parseStateSelector(unwrapWrapperArgument(raw.substring("props(".length(), end)));
		if (state == null || state.getCompoundTag("Properties") == null || state.getCompoundTag("Properties").isEmpty()) {
			return new SourceResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_PROPS.toString()));
		}
		return new SourceResult(end + 1, none());
	}

	private static SourceResult readTileEntityModeWrapper(String raw, String prefix) {
		int end = findWrapperEnd(raw, prefix.length());
		if (end < 0) {
			return new SourceResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_MODE_WRAPPER.toString()));
		}
		SourceResult source = readSourceExpression(unwrapWrapperArgument(raw.substring(prefix.length(), end)));
		if (source.diagnostic().isError()) {
			return source;
		}
		return new SourceResult(end + 1, source.diagnostic());
	}

	private static SourceResult readYRangeWrapper(String raw) {
		int end = findWrapperEnd(raw, "y(".length());
		if (end < 0) {
			return new SourceResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_MODE_WRAPPER.toString()));
		}
		String argument = raw.substring("y(".length(), end).trim();
		int comma = argument.indexOf(',');
		if (comma < 0 || !isValidYRange(argument.substring(0, comma))) {
			return new SourceResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_Y_RANGE.toString()));
		}
		SourceResult source = readSourceExpression(argument.substring(comma + 1));
		if (source.diagnostic().isError()) {
			return source;
		}
		return new SourceResult(end + 1, source.diagnostic());
	}

	private static SourceResult readSourceExpression(String raw) {
		String source = raw.trim();
		if (source.isEmpty()) {
			return new SourceResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_EMPTY.toString()));
		}
		SourceResult result = readSource(source + "=minecraft:stone");
		if (result.diagnostic().isError()) {
			return result;
		}
		if (result.read() != source.length()) {
			return new SourceResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_MODE_WRAPPER.toString()));
		}
		return result;
	}

	private static boolean isValidYRange(String raw) {
		String[] parts = raw.trim().split("\\.\\.", -1);
		if (parts.length != 2) {
			return false;
		}
		String minRaw = parts[0].trim();
		String maxRaw = parts[1].trim();
		if (minRaw.isEmpty() && maxRaw.isEmpty()) {
			return false;
		}
		try {
			Integer minY = minRaw.isEmpty() ? null : Integer.parseInt(minRaw);
			Integer maxY = maxRaw.isEmpty() ? null : Integer.parseInt(maxRaw);
			return minY == null || maxY == null || minY <= maxY;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	private static SourceResult readNameWrapper(String raw, String prefix, boolean literal) {
		int end = findWrapperEnd(raw, prefix.length());
		if (end < 0) {
			return new SourceResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_MODE_WRAPPER.toString()));
		}
		String argument = unwrapWrapperArgument(raw.substring(prefix.length(), end));
		if (argument.isEmpty()) {
			return new SourceResult(0, error(literal
					? Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_LITERAL.toString()
					: Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_REGEX_SYNTAX.toString()));
		}
		if (literal) {
			return new SourceResult(end + 1, validateLiteralSource(argument));
		}
		try {
			Pattern.compile(argument);
		} catch (PatternSyntaxException ex) {
			return new SourceResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_REGEX_SYNTAX.toString()));
		}
		return new SourceResult(end + 1, none());
	}

	private static int findWrapperEnd(String raw, int start) {
		boolean singleQuoted = false;
		boolean doubleQuoted = false;
		boolean escaped = false;
		int nestedParens = 0;
		for (int i = start; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (escaped) {
				escaped = false;
				continue;
			}
			if (doubleQuoted && c == '\\') {
				escaped = true;
				continue;
			}
			if (c == '\'' && !doubleQuoted) {
				singleQuoted = !singleQuoted;
				continue;
			}
			if (c == '"' && !singleQuoted) {
				doubleQuoted = !doubleQuoted;
				continue;
			}
			if (c == '(' && !singleQuoted && !doubleQuoted) {
				nestedParens++;
				continue;
			}
			if (c == ')' && !singleQuoted && !doubleQuoted) {
				if (nestedParens == 0) {
					return i;
				}
				nestedParens--;
			}
		}
		return -1;
	}

	private static String unwrapWrapperArgument(String argument) {
		String trimmed = argument.trim();
		if (trimmed.length() >= 2 && trimmed.startsWith("'") && trimmed.endsWith("'")) {
			return trimmed.substring(1, trimmed.length() - 1);
		}
		return trimmed;
	}

	private static Diagnostic validateLiteralSource(String raw) {
		String source = raw;
		if (!source.contains(":")) {
			source = "minecraft:" + source;
		}
		if (source.startsWith("minecraft:")) {
			return BlockRegistry.isValidName(source)
					? none()
					: error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_LITERAL.format(source));
		}
		return RESOURCE_LOCATION.matcher(source).matches()
				? none()
				: error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_LITERAL.format(source));
	}

	private static CompoundTag parseStateSelector(String raw) {
		try {
			SNBTParser parser = new SNBTParser(raw);
			Tag parsed = parser.parse(true);
			if (!(parsed instanceof CompoundTag state) || state.getString("Name") == null) {
				return null;
			}
			return state;
		} catch (ParseException | RuntimeException ex) {
			return null;
		}
	}

	private static Diagnostic validateSource(String raw) {
		String source = stripOuterSingleQuotes(raw);
		if (source.isEmpty()) {
			return error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_EMPTY.toString());
		}
		if (raw.equals(source) && !source.startsWith("minecraft:")) {
			String vanillaSource = "minecraft:" + source;
			if (!BlockRegistry.isValidName(vanillaSource)) {
				return error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_UNKNOWN.format(vanillaSource));
			}
			source = vanillaSource;
		}
		if (REGEX_META.matcher(source).find()) {
			return warning(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_REGEX.format(source));
		}
		if (source.startsWith("minecraft:") && !BlockRegistry.isValidName(source)) {
			return warning(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_REGEX.format(source));
		}
		return none();
	}

	private static TargetResult readTarget(String to) {
		if (to.startsWith("{")) {
			try {
				SNBTParser parser = new SNBTParser(to);
				Tag state = parser.parse(true);
				if (!(state instanceof CompoundTag compound) || compound.getString("Name") == null) {
					return new TargetResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_STATE_SNBT.toString()));
				}
				return new TargetResult(parser.getReadChars() - 1, none());
			} catch (ParseException | RuntimeException ex) {
				return new TargetResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_STATE_SNBT.toString()));
			}
		}
		if (to.startsWith("'")) {
			int end = to.indexOf('\'', 1);
			if (end < 0) {
				return new TargetResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_TARGET_QUOTE.toString()));
			}
			if (end == 1) {
				return new TargetResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_TARGET_EMPTY.toString()));
			}
			String remaining = to.substring(end + 1).trim();
			if (!remaining.isEmpty()) {
				return new TargetResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_QUOTED_TARGET_AMBIGUOUS.toString()));
			}
			return new TargetResult(end + 1, none());
		}

		int read = 0;
		while (read < to.length() && to.charAt(read) != ',' && to.charAt(read) != ';') {
			read++;
		}
		String target = to.substring(0, read).trim();
		if (target.isEmpty()) {
			return new TargetResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_TARGET_EMPTY.toString()));
		}
		if (!target.startsWith("minecraft:")) {
			target = "minecraft:" + target;
		}
		if (!BlockRegistry.isValidName(target)) {
			return new TargetResult(0, error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_TARGET_UNKNOWN.format(target)));
		}
		return new TargetResult(read, none());
	}

	private static String stripOuterSingleQuotes(String value) {
		if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}

	static Diagnostic none() {
		return new Diagnostic(Severity.NONE, "");
	}

	private static Diagnostic error(String message) {
		return new Diagnostic(Severity.ERROR, message);
	}

	private static Diagnostic warning(String message) {
		return new Diagnostic(Severity.WARNING, message);
	}

	enum Severity {
		NONE,
		ERROR,
		WARNING
	}

	record Diagnostic(Severity severity, String message) {
		boolean isNone() {
			return severity == Severity.NONE;
		}

		boolean isError() {
			return severity == Severity.ERROR;
		}

		boolean isWarning() {
			return severity == Severity.WARNING;
		}
	}

	record NameResult(String name, Diagnostic diagnostic) {}

	private record SourceResult(int read, Diagnostic diagnostic) {}

	private record TargetResult(int read, Diagnostic diagnostic) {}
}
