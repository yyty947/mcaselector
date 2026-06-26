package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplaceBlocksFieldTest {

	@Test
	void keepsLegacyNameSourcesAsRegexMode() {
		ReplaceBlocksField field = parse("minecraft:stone=minecraft:dirt");
		ChunkFilter.BlockReplaceSource source = onlySource(field);

		assertEquals(ChunkFilter.BlockReplaceSourceType.LEGACY_REGEX_NAME, source.getType());
		assertEquals("minecraft:stone=minecraft:dirt", field.valueToString());
		assertTrue(source.matches(state("minecraft:stone")));
		assertFalse(source.matches(state("minecraft:deepslate")));
	}

	@Test
	void keepsQuotedCustomSourcesAsLegacyRegexMode() {
		ReplaceBlocksField field = parse("'custom:block'=minecraft:stone");
		ChunkFilter.BlockReplaceSource source = onlySource(field);

		assertEquals(ChunkFilter.BlockReplaceSourceType.LEGACY_REGEX_NAME, source.getType());
		assertEquals("'custom:block'=minecraft:stone", field.valueToString());
		assertTrue(source.matches(state("custom:block")));
	}

	@Test
	void parsesExplicitRegexSourceMode() {
		ReplaceBlocksField field = parse("regex(minecraft:.*_log)=minecraft:stone");
		ChunkFilter.BlockReplaceSource source = onlySource(field);

		assertEquals(ChunkFilter.BlockReplaceSourceType.REGEX_NAME, source.getType());
		assertEquals("regex(minecraft:.*_log)=minecraft:stone", field.valueToString());
		assertTrue(source.matches(state("minecraft:oak_log")));
		assertFalse(source.matches(state("minecraft:stone")));
	}

	@Test
	void supportsQuotedRegexWrapperArguments() {
		ReplaceBlocksField field = parse("regex('minecraft:(oak|birch)_log')=minecraft:stone");
		ChunkFilter.BlockReplaceSource source = onlySource(field);

		assertEquals(ChunkFilter.BlockReplaceSourceType.REGEX_NAME, source.getType());
		assertEquals("regex('minecraft:(oak|birch)_log')=minecraft:stone", field.valueToString());
		assertTrue(source.matches(state("minecraft:oak_log")));
		assertTrue(source.matches(state("minecraft:birch_log")));
		assertFalse(source.matches(state("minecraft:spruce_log")));
	}

	@Test
	void rejectsInvalidExplicitRegex() {
		ReplaceBlocksField field = new ReplaceBlocksField();

		assertFalse(field.parseNewValue("regex(*)=minecraft:stone"));
	}

	@Test
	void parsesExplicitLiteralSourceMode() {
		ReplaceBlocksField field = parse("literal(stone)=minecraft:dirt");
		ChunkFilter.BlockReplaceSource source = onlySource(field);

		assertEquals(ChunkFilter.BlockReplaceSourceType.LITERAL_NAME, source.getType());
		assertEquals("literal(minecraft:stone)=minecraft:dirt", field.valueToString());
		assertTrue(source.matches(state("minecraft:stone")));
		assertFalse(source.matches(state("minecraft:stone_slab")));
	}

	@Test
	void literalModeDoesNotInterpretRegexMetacharacters() {
		ReplaceBlocksField field = parse("literal(example:block.name)=minecraft:dirt");
		ChunkFilter.BlockReplaceSource source = onlySource(field);

		assertEquals(ChunkFilter.BlockReplaceSourceType.LITERAL_NAME, source.getType());
		assertTrue(source.matches(state("example:block.name")));
		assertFalse(source.matches(state("example:blockXname")));
	}

	@Test
	void keepsSourceStateMatchingExact() {
		ReplaceBlocksField field = parse("{Name:\"minecraft:oak_stairs\",Properties:{facing:\"north\"}}=minecraft:stone");
		ChunkFilter.BlockReplaceSource source = onlySource(field);

		assertEquals(ChunkFilter.BlockReplaceSourceType.EXACT_STATE, source.getType());
		assertTrue(source.matches(state("minecraft:oak_stairs", Map.of("facing", "north"))));
		assertFalse(source.matches(state("minecraft:oak_stairs", Map.of("facing", "north", "half", "bottom"))));
	}

	@Test
	void parsesSelectedPropertiesMode() {
		ReplaceBlocksField field = parse("props({Name:\"minecraft:oak_stairs\",Properties:{facing:\"north\"}})=minecraft:stone");
		ChunkFilter.BlockReplaceSource source = onlySource(field);

		assertEquals(ChunkFilter.BlockReplaceSourceType.SELECTED_PROPERTIES, source.getType());
		assertTrue(field.valueToString().startsWith("props("));
		assertTrue(new ReplaceBlocksField().parseNewValue(field.valueToString()));
		assertTrue(source.matches(state("minecraft:oak_stairs", Map.of(
				"facing", "north",
				"half", "bottom",
				"shape", "straight",
				"waterlogged", "false"))));
		assertFalse(source.matches(state("minecraft:oak_stairs", Map.of("facing", "south", "half", "bottom"))));
		assertFalse(source.matches(state("minecraft:oak_slab", Map.of("facing", "north"))));
		assertFalse(source.matches(state("minecraft:oak_stairs")));
	}

	@Test
	void rejectsEmptySelectedPropertiesMode() {
		ReplaceBlocksField field = new ReplaceBlocksField();

		assertFalse(field.parseNewValue("props({Name:\"minecraft:stone\"})=minecraft:dirt"));
		assertFalse(field.parseNewValue("props({Name:\"minecraft:stone\",Properties:{}})=minecraft:dirt"));
	}

	@Test
	void matchesAirUsesSourceModeSemantics() {
		assertTrue(onlySource(parse("literal(air)=minecraft:glass")).matchesAir());
		assertTrue(onlySource(parse("regex(minecraft:.*)=minecraft:glass")).matchesAir());
		assertFalse(onlySource(parse("literal(minecraft:stone)=minecraft:glass")).matchesAir());
	}

	private ReplaceBlocksField parse(String value) {
		ReplaceBlocksField field = new ReplaceBlocksField();
		assertTrue(field.parseNewValue(value));
		return field;
	}

	private ChunkFilter.BlockReplaceSource onlySource(ReplaceBlocksField field) {
		assertEquals(1, field.getNewValue().size());
		return field.getNewValue().keySet().iterator().next();
	}

	private CompoundTag state(String name) {
		CompoundTag state = new CompoundTag();
		state.putString("Name", name);
		return state;
	}

	private CompoundTag state(String name, Map<String, String> properties) {
		CompoundTag state = state(name);
		CompoundTag props = new CompoundTag();
		for (Map.Entry<String, String> property : properties.entrySet()) {
			props.putString(property.getKey(), property.getValue());
		}
		state.put("Properties", props);
		return state;
	}
}
