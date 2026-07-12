package net.querz.mcaselector.ui.dialog;

import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyCode;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.List;
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
	void permitsMarqueeFromEveryBlankRuleTableArea() {
		assertTrue(ReplaceBlocksRuleBuilderDialog.canStartRuleMarquee(true, false, false));
		assertFalse(ReplaceBlocksRuleBuilderDialog.canStartRuleMarquee(true, true, false));
		assertFalse(ReplaceBlocksRuleBuilderDialog.canStartRuleMarquee(true, false, true));
		assertFalse(ReplaceBlocksRuleBuilderDialog.canStartRuleMarquee(false, false, false));
	}
}
