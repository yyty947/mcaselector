package net.querz.mcaselector.version.mapping.blockstate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.querz.mcaselector.io.FileHelper;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public final class BlockStateCatalog {

	private static final Gson GSON = new GsonBuilder().create();
	private static final String JAVA_1_21_9_RESOURCE = "mapping/block_states/java_1_21_9.json";
	private static final BlockStateCatalog JAVA_1_21_9 = FileHelper.loadFromResource(JAVA_1_21_9_RESOURCE, BlockStateCatalog::load);
	private static final List<BlockStateCatalog> AVAILABLE = List.of(JAVA_1_21_9);

	private final String version;
	private final int dataVersion;
	private final String source;
	private final Map<String, Block> blocks;

	private BlockStateCatalog(String version, int dataVersion, String source, Map<String, Block> blocks) {
		this.version = version;
		this.dataVersion = dataVersion;
		this.source = source;
		this.blocks = Collections.unmodifiableMap(blocks);
	}

	public static BlockStateCatalog load(Reader reader) {
		CatalogData data = GSON.fromJson(reader, new TypeToken<CatalogData>() {}.getType());
		Map<String, Block> blocks = new TreeMap<>();
		data.blocks.forEach((name, block) -> blocks.put(normalizeName(name), block.normalized()));
		return new BlockStateCatalog(data.version, data.dataVersion, data.source, blocks);
	}

	public static List<BlockStateCatalog> available() {
		return AVAILABLE;
	}

	public static BlockStateCatalog latestJava() {
		return JAVA_1_21_9;
	}

	public static Optional<BlockStateCatalog> findByVersion(String version) {
		return AVAILABLE.stream()
				.filter(catalog -> catalog.version().equals(version))
				.findFirst();
	}

	public String version() {
		return version;
	}

	public int dataVersion() {
		return dataVersion;
	}

	public String source() {
		return source;
	}

	public Set<String> blockNames() {
		return blocks.keySet();
	}

	public boolean containsBlock(String blockName) {
		return blocks.containsKey(normalizeName(blockName));
	}

	public Optional<Block> getBlock(String blockName) {
		return Optional.ofNullable(blocks.get(normalizeName(blockName)));
	}

	public Map<String, List<String>> properties(String blockName) {
		return getBlock(blockName)
				.map(Block::properties)
				.orElseGet(Collections::emptyMap);
	}

	public Map<String, String> defaultProperties(String blockName) {
		return getBlock(blockName)
				.map(Block::defaultProperties)
				.orElseGet(Collections::emptyMap);
	}

	public boolean hasProperty(String blockName, String propertyName) {
		return properties(blockName).containsKey(propertyName);
	}

	public boolean isValidPropertyValue(String blockName, String propertyName, String value) {
		List<String> values = properties(blockName).get(propertyName);
		return values != null && values.contains(value);
	}

	public static String normalizeName(String blockName) {
		if (blockName == null || blockName.isBlank()) {
			return "";
		}
		String trimmed = blockName.trim();
		if (trimmed.indexOf(':') < 0) {
			return "minecraft:" + trimmed;
		}
		return trimmed;
	}

	public record Block(
			Map<String, List<String>> properties,
			Map<String, String> defaultProperties,
			int stateCount) {

		private Block normalized() {
			Map<String, List<String>> normalizedProperties = new TreeMap<>();
			(properties == null ? Collections.<String, List<String>>emptyMap() : properties)
					.forEach((name, values) -> normalizedProperties.put(name, values == null ? List.of() : List.copyOf(values)));
			return new Block(
					Collections.unmodifiableMap(normalizedProperties),
					Collections.unmodifiableMap(new TreeMap<>(defaultProperties == null ? Collections.emptyMap() : defaultProperties)),
					stateCount);
		}
	}

	private record CatalogData(
			String version,
			int dataVersion,
			String source,
			Map<String, Block> blocks) {}
}
