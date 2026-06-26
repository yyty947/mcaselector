package net.querz.mcaselector.version.mapping.blockstate;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockStateCatalogTest {

	@Test
	void loadsJavaOneTwentyOneNineCatalog() {
		BlockStateCatalog catalog = BlockStateCatalog.latestJava();

		assertEquals("1.21.9", catalog.version());
		assertEquals(4554, catalog.dataVersion());
		assertTrue(catalog.blockNames().contains("minecraft:acacia_trapdoor"));
		assertTrue(catalog.blockNames().contains("minecraft:blue_ice"));
	}

	@Test
	void exposesPropertiesForStatefulBlocks() {
		BlockStateCatalog catalog = BlockStateCatalog.latestJava();

		assertTrue(catalog.containsBlock("acacia_trapdoor"));
		assertTrue(catalog.hasProperty("minecraft:acacia_trapdoor", "facing"));
		assertTrue(catalog.isValidPropertyValue("minecraft:acacia_trapdoor", "facing", "north"));
		assertTrue(catalog.isValidPropertyValue("minecraft:acacia_trapdoor", "open", "false"));
		assertFalse(catalog.isValidPropertyValue("minecraft:acacia_trapdoor", "facing", "up"));

		assertEquals(Map.of(
				"facing", "north",
				"half", "bottom",
				"open", "false",
				"powered", "false",
				"waterlogged", "false"), catalog.defaultProperties("minecraft:acacia_trapdoor"));
	}

	@Test
	void keepsColorBlocksAsBlockIdsWithoutProperties() {
		BlockStateCatalog catalog = BlockStateCatalog.latestJava();

		assertTrue(catalog.containsBlock("yellow_wool"));
		assertTrue(catalog.containsBlock("blue_wool"));
		assertTrue(catalog.properties("minecraft:yellow_wool").isEmpty());
		assertTrue(catalog.properties("minecraft:blue_wool").isEmpty());
	}
}
