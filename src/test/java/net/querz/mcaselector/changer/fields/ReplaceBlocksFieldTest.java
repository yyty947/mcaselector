package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

	@Test
	void parsesTileEntitySourceFilters() {
		ReplaceBlocksField tileOnly = parse("tile(literal(chest))=minecraft:stone");
		ChunkFilter.BlockReplaceSource tileSource = onlySource(tileOnly);

		assertEquals(ChunkFilter.BlockReplaceSourceType.LITERAL_NAME, tileSource.getType());
		assertEquals(ChunkFilter.BlockReplaceTileEntityMode.REQUIRE_TILE_ENTITY, tileSource.getTileEntityMode());
		assertEquals("tile(literal(minecraft:chest))=minecraft:stone", tileOnly.valueToString());
		assertTrue(tileSource.matches(state("minecraft:chest"), true));
		assertFalse(tileSource.matches(state("minecraft:chest"), false));

		ReplaceBlocksField noTile = parse("no_tile(props({Name:\"minecraft:oak_stairs\",Properties:{facing:\"north\"}}))=minecraft:stone");
		ChunkFilter.BlockReplaceSource noTileSource = onlySource(noTile);

		assertEquals(ChunkFilter.BlockReplaceSourceType.SELECTED_PROPERTIES, noTileSource.getType());
		assertEquals(ChunkFilter.BlockReplaceTileEntityMode.EXCLUDE_TILE_ENTITY, noTileSource.getTileEntityMode());
		assertTrue(noTileSource.matches(state("minecraft:oak_stairs", Map.of("facing", "north", "half", "bottom")), false));
		assertFalse(noTileSource.matches(state("minecraft:oak_stairs", Map.of("facing", "north", "half", "bottom")), true));
		assertTrue(new ReplaceBlocksField().parseNewValue(noTile.valueToString()));
	}

	@Test
	void tileEntitySourceFiltersParticipateInAirMatching() {
		assertFalse(onlySource(parse("tile(literal(air))=minecraft:glass")).matchesAir());
		assertTrue(onlySource(parse("no_tile(literal(air))=minecraft:glass")).matchesAir());
	}

	@Test
	void parsesBuilderPresetGeneratedRules() {
		assertEquals("literal(minecraft:air)=minecraft:stone", parse("literal(minecraft:air)=minecraft:stone").valueToString());

		ChunkFilter.BlockReplaceSource fluids = onlySource(parse("regex(minecraft:(water|lava))=minecraft:air"));
		assertTrue(fluids.matches(state("minecraft:water")));
		assertTrue(fluids.matches(state("minecraft:lava")));
		assertFalse(fluids.matches(state("minecraft:stone")));

		ChunkFilter.BlockReplaceSource logsLeaves = onlySource(parse("regex(minecraft:.*_(log|wood|stem|hyphae|leaves))=minecraft:air"));
		assertTrue(logsLeaves.matches(state("minecraft:oak_log")));
		assertTrue(logsLeaves.matches(state("minecraft:crimson_stem")));
		assertTrue(logsLeaves.matches(state("minecraft:oak_leaves")));
		assertFalse(logsLeaves.matches(state("minecraft:stone")));

		ChunkFilter.BlockReplaceSource ores = onlySource(parse("regex(minecraft:.*_ore)=minecraft:stone"));
		assertTrue(ores.matches(state("minecraft:diamond_ore")));
		assertTrue(ores.matches(state("minecraft:deepslate_diamond_ore")));
		assertFalse(ores.matches(state("minecraft:stone")));

		ChunkFilter.BlockReplaceSource containers = onlySource(parse("tile(regex(minecraft:.*(chest|barrel|furnace|smoker|hopper|dispenser|dropper|shulker_box)))=minecraft:air"));
		assertEquals(ChunkFilter.BlockReplaceTileEntityMode.REQUIRE_TILE_ENTITY, containers.getTileEntityMode());
		assertTrue(containers.matches(state("minecraft:chest"), true));
		assertTrue(containers.matches(state("minecraft:white_shulker_box"), true));
		assertFalse(containers.matches(state("minecraft:chest"), false));
	}

	@Test
	void parsesYRangeSourceFilter() {
		ReplaceBlocksField field = parse("y(-64..64, literal(stone))=minecraft:dirt");
		ChunkFilter.BlockReplaceSource source = onlySource(field);

		assertEquals("y(-64..64, literal(minecraft:stone))=minecraft:dirt", field.valueToString());
		assertEquals(-64, source.getMinY());
		assertEquals(64, source.getMaxY());
		assertTrue(source.matches(state("minecraft:stone"), false, -64));
		assertTrue(source.matches(state("minecraft:stone"), false, 64));
		assertFalse(source.matches(state("minecraft:stone"), false, 65));
		assertFalse(source.matches(state("minecraft:dirt"), false, 0));
	}

	@Test
	void parsesOpenEndedYRangeSourceFilters() {
		ChunkFilter.BlockReplaceSource minOnly = onlySource(parse("y(64.., literal(stone))=minecraft:dirt"));
		assertEquals(64, minOnly.getMinY());
		assertNull(minOnly.getMaxY());
		assertFalse(minOnly.matches(state("minecraft:stone"), false, 63));
		assertTrue(minOnly.matches(state("minecraft:stone"), false, 64));

		ChunkFilter.BlockReplaceSource maxOnly = onlySource(parse("y(..-1, literal(stone))=minecraft:dirt"));
		assertNull(maxOnly.getMinY());
		assertEquals(-1, maxOnly.getMaxY());
		assertTrue(maxOnly.matches(state("minecraft:stone"), false, -1));
		assertFalse(maxOnly.matches(state("minecraft:stone"), false, 0));
	}

	@Test
	void combinesYRangeWithTileSourceFilters() {
		ReplaceBlocksField field = parse("tile(y(0..15, literal(chest)))=minecraft:stone");
		ChunkFilter.BlockReplaceSource source = onlySource(field);

		assertEquals(ChunkFilter.BlockReplaceTileEntityMode.REQUIRE_TILE_ENTITY, source.getTileEntityMode());
		assertEquals("y(0..15, tile(literal(minecraft:chest)))=minecraft:stone", field.valueToString());
		assertTrue(source.matches(state("minecraft:chest"), true, 0));
		assertFalse(source.matches(state("minecraft:chest"), false, 0));
		assertFalse(source.matches(state("minecraft:chest"), true, 16));
	}

	@Test
	void parsesBiomeSourceFilter() {
		ReplaceBlocksField field = parse("biome(plains;minecraft:forest, literal(stone))=minecraft:dirt");
		ChunkFilter.BlockReplaceSource source = onlySource(field);

		assertEquals(Set.of("minecraft:plains", "minecraft:forest"), source.getBiomes());
		assertEquals("biome(minecraft:plains;minecraft:forest, literal(minecraft:stone))=minecraft:dirt", field.valueToString());
		assertTrue(source.matches(state("minecraft:stone"), false, 0, "minecraft:plains"));
		assertTrue(source.matches(state("minecraft:stone"), false, 0, "minecraft:forest"));
		assertFalse(source.matches(state("minecraft:stone"), false, 0, "minecraft:desert"));
		assertFalse(source.matches(state("minecraft:stone"), false, 0));

		ReplaceBlocksField custom = parse("biome(example:glowing_grove, literal(stone))=minecraft:dirt");
		assertEquals(Set.of("example:glowing_grove"), onlySource(custom).getBiomes());
	}

	@Test
	void combinesBiomeWithYRangeAndTileSourceFilters() {
		ReplaceBlocksField field = parse("biome(minecraft:plains, y(0..15, tile(literal(chest))))=minecraft:stone");
		ChunkFilter.BlockReplaceSource source = onlySource(field);

		assertEquals("biome(minecraft:plains, y(0..15, tile(literal(minecraft:chest))))=minecraft:stone", field.valueToString());
		assertTrue(source.matches(state("minecraft:chest"), true, 0, "minecraft:plains"));
		assertFalse(source.matches(state("minecraft:chest"), false, 0, "minecraft:plains"));
		assertFalse(source.matches(state("minecraft:chest"), true, 16, "minecraft:plains"));
		assertFalse(source.matches(state("minecraft:chest"), true, 0, "minecraft:forest"));
	}

	@Test
	void conditionalSourcesFailClosedWithoutRequiredContext() {
		CompoundTag chest = state("minecraft:chest");
		ChunkFilter.BlockReplaceSource tile = ChunkFilter.BlockReplaceSource.literalName("minecraft:chest")
				.withTileEntityMode(ChunkFilter.BlockReplaceTileEntityMode.EXCLUDE_TILE_ENTITY);
		ChunkFilter.BlockReplaceSource yRange = ChunkFilter.BlockReplaceSource.literalName("minecraft:chest")
				.withYRange(0, 15);
		ChunkFilter.BlockReplaceSource biome = ChunkFilter.BlockReplaceSource.literalName("minecraft:chest")
				.withBiomes(Set.of("minecraft:plains"));

		assertTrue(tile.requiresLocationContext());
		assertFalse(tile.matches(chest));
		assertFalse(yRange.matches(chest));
		assertFalse(biome.matches(chest));
		assertTrue(tile.matches(chest, false));
		assertTrue(yRange.matches(chest, false, 8));
		assertTrue(biome.matches(chest, false, 8, "minecraft:plains"));
	}

	@Test
	void rejectsInvalidYRangeSourceFilters() {
		ReplaceBlocksField field = new ReplaceBlocksField();

		assertFalse(field.parseNewValue("y(.., literal(stone))=minecraft:dirt"));
		assertFalse(field.parseNewValue("y(10..0, literal(stone))=minecraft:dirt"));
		assertFalse(field.parseNewValue("y(foo..10, literal(stone))=minecraft:dirt"));
		assertFalse(field.parseNewValue("y(0..10)=minecraft:dirt"));
	}

	@Test
	void rejectsInvalidBiomeSourceFilters() {
		ReplaceBlocksField field = new ReplaceBlocksField();

		assertFalse(field.parseNewValue("biome(, literal(stone))=minecraft:dirt"));
		assertFalse(field.parseNewValue("biome(minecraft:not_a_biome, literal(stone))=minecraft:dirt"));
		assertFalse(field.parseNewValue("biome(minecraft:plains)=minecraft:dirt"));
		assertFalse(field.parseNewValue("biome(minecraft:plains,)=minecraft:dirt"));
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
