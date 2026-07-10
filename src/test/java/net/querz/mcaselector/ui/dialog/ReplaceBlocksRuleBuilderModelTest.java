package net.querz.mcaselector.ui.dialog;

import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ReplaceBlocksRuleBuilderModelTest {

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
}
