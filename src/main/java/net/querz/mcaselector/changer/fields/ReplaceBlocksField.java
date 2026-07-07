package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.mcaselector.version.mapping.registry.BlockRegistry;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.Tag;
import net.querz.nbt.io.snbt.ParseException;
import net.querz.nbt.io.snbt.SNBTParser;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ReplaceBlocksField extends Field<Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData>> {

	private static final Pattern RESOURCE_LOCATION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

	public ReplaceBlocksField() {
		super(FieldType.REPLACE_BLOCKS);
	}

	@Override
	public boolean parseNewValue(String s) {

		// format: <from=to>[,<from=to>,...]
		// from format: minecraft:<block-name>
		//              <block-name>
		//              '<custom-block-name-with-namespace>'
		//              <snbt-string-block-state>
		//              literal(<block-name>)
		//              regex(<java-regex>)
		//              props(<snbt-string-block-state-with-properties>)
		//              tile(<source>)
		//              no_tile(<source>)
		//              y(<min>..<max>, <source>)
		// to format:   minecraft:<block-name>
		//              <block-name>
		//              '<custom-block-name-with-namespace>'
		//              <snbt-string-block-state>
		//              <to>;<snbt-string-tile-entity>

		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> newValue = new LinkedHashMap<>();

		String trimmed = s.trim();

		while (!trimmed.isEmpty()) {
			SourceReadResult source = readSource(trimmed);
			if (source == null) {
				return super.parseNewValue(s);
			}

			String to = trimmed.substring(source.read()).trim();
			if (!to.startsWith("=")) {
				return super.parseNewValue(s);
			}
			to = to.substring(1).trim();

			int read = 0;
			CompoundTag toState = null;
			String toName = null;
			if (to.startsWith("{")) {
				// block state
				try {
					SNBTParser parser = new SNBTParser(to);
					Tag parsed = parser.parse(true);
					if (!(parsed instanceof CompoundTag compound) || compound.getString("Name") == null) {
						return super.parseNewValue(s);
					}
					toState = compound;
					read += parser.getReadChars() - 1;
				} catch (ParseException | RuntimeException ex) {
					return super.parseNewValue(s);
				}
			} else if (to.startsWith("'")) {
				// quoted
				int i = 1;
				while (i < to.length() && to.charAt(i) != '\'') {
					i++;
				}
				toName = to.substring(1, Math.max(i, to.length()));
				if (!toName.endsWith("'")) {
					return super.parseNewValue(s);
				}
				toName = toName.substring(0, toName.length() - 1);
				if (toName.isEmpty()) {
					return super.parseNewValue(s);
				}
				read += i + 1;
			} else {
				// minecraft block
				// read everything until , or ;
				int i = 0;
				while (i < to.length()) {
					if (to.charAt(i) == ',' || to.charAt(i) == ';') {
						break;
					}
					i++;
				}
				toName = to.substring(0, i);
				if (!toName.startsWith("minecraft:")) {
					toName = "minecraft:" + toName;
				}
				if (!BlockRegistry.isValidName(toName)) {
					return super.parseNewValue(s);
				}
				read += i;
			}

			to = to.substring(read).trim();

			CompoundTag toTile = null;
			if (to.startsWith(";")) {
				to = to.substring(1).trim();
				if (to.isEmpty()) {
					return super.parseNewValue(s);
				}
				try {
					SNBTParser parser = new SNBTParser(to);
					toTile = (CompoundTag) parser.parse(true);
					int readTile = parser.getReadChars();
					to = to.substring(readTile - 1);
				} catch (ParseException ex) {
					return super.parseNewValue(s);
				}
			}

			ChunkFilter.BlockReplaceData data;
			if (toName != null && toTile != null) {
				data = new ChunkFilter.BlockReplaceData(toName, toTile);
			} else if (toName != null) {
				data = new ChunkFilter.BlockReplaceData(toName);
			} else if (toState != null && toTile != null) {
				data = new ChunkFilter.BlockReplaceData(toState, toTile);
			} else if (toState != null) {
				data = new ChunkFilter.BlockReplaceData(toState);
			} else {
				return super.parseNewValue(s);
			}
			newValue.put(source.source(), data);

			to = to.trim();

			if (to.startsWith(",")) {
				trimmed = to.substring(1).trim();
			} else if (!to.isEmpty()) {
				return super.parseNewValue(s);
			} else {
				break;
			}
		}

		if (newValue.isEmpty()) {
			return super.parseNewValue(s);
		}

		setNewValue(newValue);
		return true;
	}

	private SourceReadResult readSource(String s) {
		if (s.startsWith("{")) {
			try {
				SNBTParser parser = new SNBTParser(s);
				Tag parsed = parser.parse(true);
				if (!(parsed instanceof CompoundTag state) || state.getString("Name") == null) {
					return null;
				}
				return new SourceReadResult(new ChunkFilter.BlockReplaceSource(state), parser.getReadChars() - 1);
			} catch (ParseException | RuntimeException ex) {
				return null;
			}
		}

		if (startsWithSourceWrapper(s)) {
			return readWrappedSource(s);
		}

		int equals = s.indexOf('=');
		if (equals < 0) {
			return null;
		}

		String from = s.substring(0, equals).trim();
		if (from.startsWith("'") && from.endsWith("'") && from.length() > 2) {
			from = from.substring(1, from.length() - 1);
		} else if (!from.startsWith("minecraft:")) {
			from = "minecraft:" + from;
			if (!BlockRegistry.isValidName(from)) {
				return null;
			}
		}
		if (from.isEmpty()) {
			return null;
		}
		return new SourceReadResult(new ChunkFilter.BlockReplaceSource(from), equals);
	}

	private boolean startsWithSourceWrapper(String s) {
		return s.startsWith("literal(") || s.startsWith("regex(") || s.startsWith("props(")
				|| s.startsWith("tile(") || s.startsWith("no_tile(") || s.startsWith("y(");
	}

	private SourceReadResult readWrappedSource(String s) {
		if (s.startsWith("y(")) {
			return readYRangeWrapper(s);
		}
		if (s.startsWith("tile(")) {
			return readTileEntityModeWrapper(s, "tile(", ChunkFilter.BlockReplaceTileEntityMode.REQUIRE_TILE_ENTITY);
		}
		if (s.startsWith("no_tile(")) {
			return readTileEntityModeWrapper(s, "no_tile(", ChunkFilter.BlockReplaceTileEntityMode.EXCLUDE_TILE_ENTITY);
		}
		if (s.startsWith("literal(")) {
			return readNameWrapper(s, "literal(", true);
		}
		if (s.startsWith("regex(")) {
			return readNameWrapper(s, "regex(", false);
		}
		if (s.startsWith("props(")) {
			int end = findWrapperEnd(s, "props(".length());
			if (end < 0) {
				return null;
			}
			CompoundTag state = parseStateSelector(unwrapWrapperArgument(s.substring("props(".length(), end)));
			if (state == null || state.getCompoundTag("Properties") == null || state.getCompoundTag("Properties").isEmpty()) {
				return null;
			}
			return new SourceReadResult(ChunkFilter.BlockReplaceSource.selectedProperties(state), end + 1);
		}
		return null;
	}

	private SourceReadResult readTileEntityModeWrapper(String s, String prefix, ChunkFilter.BlockReplaceTileEntityMode mode) {
		int end = findWrapperEnd(s, prefix.length());
		if (end < 0) {
			return null;
		}
		SourceReadResult source = readSourceExpression(unwrapWrapperArgument(s.substring(prefix.length(), end)));
		if (source == null) {
			return null;
		}
		return new SourceReadResult(source.source().withTileEntityMode(mode), end + 1);
	}

	private SourceReadResult readYRangeWrapper(String s) {
		int end = findWrapperEnd(s, "y(".length());
		if (end < 0) {
			return null;
		}
		String argument = s.substring("y(".length(), end).trim();
		int comma = argument.indexOf(',');
		if (comma < 0) {
			return null;
		}
		YRange yRange = parseYRange(argument.substring(0, comma));
		if (yRange == null) {
			return null;
		}
		SourceReadResult source = readSourceExpression(argument.substring(comma + 1));
		if (source == null) {
			return null;
		}
		return new SourceReadResult(source.source().withYRange(yRange.minY(), yRange.maxY()), end + 1);
	}

	private SourceReadResult readSourceExpression(String raw) {
		String source = raw.trim();
		if (source.isEmpty()) {
			return null;
		}
		SourceReadResult result = readSource(source + "=minecraft:stone");
		if (result == null || result.read() != source.length()) {
			return null;
		}
		return result;
	}

	private YRange parseYRange(String raw) {
		String[] parts = raw.trim().split("\\.\\.", -1);
		if (parts.length != 2) {
			return null;
		}
		String minRaw = parts[0].trim();
		String maxRaw = parts[1].trim();
		if (minRaw.isEmpty() && maxRaw.isEmpty()) {
			return null;
		}
		try {
			Integer minY = minRaw.isEmpty() ? null : Integer.parseInt(minRaw);
			Integer maxY = maxRaw.isEmpty() ? null : Integer.parseInt(maxRaw);
			if (minY != null && maxY != null && minY > maxY) {
				return null;
			}
			return new YRange(minY, maxY);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private SourceReadResult readNameWrapper(String s, String prefix, boolean literal) {
		int end = findWrapperEnd(s, prefix.length());
		if (end < 0) {
			return null;
		}
		String argument = unwrapWrapperArgument(s.substring(prefix.length(), end));
		if (argument.isEmpty()) {
			return null;
		}
		if (literal) {
			String name = normalizeLiteralName(argument);
			if (name == null) {
				return null;
			}
			return new SourceReadResult(ChunkFilter.BlockReplaceSource.literalName(name), end + 1);
		}
		try {
			Pattern.compile(argument);
		} catch (PatternSyntaxException ex) {
			return null;
		}
		return new SourceReadResult(ChunkFilter.BlockReplaceSource.regexName(argument), end + 1);
	}

	private int findWrapperEnd(String s, int start) {
		boolean singleQuoted = false;
		boolean doubleQuoted = false;
		boolean escaped = false;
		int nestedParens = 0;
		for (int i = start; i < s.length(); i++) {
			char c = s.charAt(i);
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

	private String unwrapWrapperArgument(String argument) {
		String trimmed = argument.trim();
		if (trimmed.length() >= 2 && trimmed.startsWith("'") && trimmed.endsWith("'")) {
			return trimmed.substring(1, trimmed.length() - 1);
		}
		return trimmed;
	}

	private String normalizeLiteralName(String name) {
		if (!name.contains(":")) {
			name = "minecraft:" + name;
		}
		if (name.startsWith("minecraft:")) {
			return BlockRegistry.isValidName(name) ? name : null;
		}
		return RESOURCE_LOCATION.matcher(name).matches() ? name : null;
	}

	private CompoundTag parseStateSelector(String raw) {
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

	@Override
	public Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> getOldValue(ChunkData data) {
		return null;
	}

	@Override
	public void change(ChunkData data) {
		VersionHandler.getImpl(data, ChunkFilter.Blocks.class).replaceBlocks(data, getNewValue());
		ChunkFilter.Heightmap heightmap = VersionHandler.getImpl(data, ChunkFilter.Heightmap.class);
		heightmap.worldSurface(data);
		heightmap.oceanFloor(data);
		heightmap.motionBlocking(data);
		heightmap.motionBlockingNoLeaves(data);
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}

	@Override
	public String toString() {
		return getType().toString() + " = \"" + escapeString(valueToString()) + "\"";
	}

	@Override
	public String valueToString() {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Map.Entry<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> entry : getNewValue().entrySet()) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(entry.getValue());
		}
		return sb.toString();
	}

	private String escapeString(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private record SourceReadResult(ChunkFilter.BlockReplaceSource source, int read) {}

	private record YRange(Integer minY, Integer maxY) {}
}
