package net.querz.mcaselector.ui.dialog;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplaceBlocksDiagnosticsTest {

	@Test
	void acceptsExplicitSourceModeSyntaxWithoutLegacyRegexWarning() {
		assertTrue(ReplaceBlocksDiagnostics
				.diagnoseValue("regex(minecraft:.*_log)=minecraft:stone", true)
				.isNone());
		assertTrue(ReplaceBlocksDiagnostics
				.diagnoseValue("literal(minecraft:stone)=minecraft:dirt", true)
				.isNone());
		assertTrue(ReplaceBlocksDiagnostics
				.diagnoseValue("props({Name:\"minecraft:oak_stairs\",Properties:{facing:\"north\"}})=minecraft:stone", true)
				.isNone());
		assertTrue(ReplaceBlocksDiagnostics
				.diagnoseValue("tile(literal(minecraft:chest))=minecraft:stone", true)
				.isNone());
		assertTrue(ReplaceBlocksDiagnostics
				.diagnoseValue("no_tile(props({Name:\"minecraft:oak_stairs\",Properties:{facing:\"north\"}}))=minecraft:stone", true)
				.isNone());
		assertTrue(ReplaceBlocksDiagnostics
				.diagnoseValue("y(-64..64, literal(minecraft:stone))=minecraft:dirt", true)
				.isNone());
		assertTrue(ReplaceBlocksDiagnostics
				.diagnoseValue("y(64.., tile(literal(minecraft:chest)))=minecraft:stone", true)
				.isNone());
		assertTrue(ReplaceBlocksDiagnostics
				.diagnoseValue("biome(minecraft:plains;minecraft:forest, y(64.., literal(minecraft:stone)))=minecraft:dirt", true)
				.isNone());
	}

	@Test
	void keepsLegacyRegexSourcesAsWarnings() {
		assertTrue(ReplaceBlocksDiagnostics
				.diagnoseValue("minecraft:.*_log=minecraft:stone", true)
				.isWarning());
	}

	@Test
	void reportsInvalidExplicitSourceModeSyntaxAsErrors() {
		assertTrue(ReplaceBlocksDiagnostics
				.diagnoseValue("regex(*)=minecraft:stone", false)
				.isError());
		assertTrue(ReplaceBlocksDiagnostics
				.diagnoseValue("literal(minecraft:not_a_block)=minecraft:stone", false)
				.isError());
		assertTrue(ReplaceBlocksDiagnostics
				.diagnoseValue("props({Name:\"minecraft:stone\"})=minecraft:dirt", false)
				.isError());
		assertTrue(ReplaceBlocksDiagnostics
				.diagnoseValue("tile(props({Name:\"minecraft:stone\"}))=minecraft:dirt", false)
				.isError());
		assertTrue(ReplaceBlocksDiagnostics
				.diagnoseValue("y(10..0, literal(minecraft:stone))=minecraft:dirt", false)
				.isError());
		assertTrue(ReplaceBlocksDiagnostics
				.diagnoseValue("y(foo..10, literal(minecraft:stone))=minecraft:dirt", false)
				.isError());
		assertTrue(ReplaceBlocksDiagnostics
				.diagnoseValue("biome(minecraft:not_a_biome, literal(minecraft:stone))=minecraft:dirt", false)
				.isError());
		assertTrue(ReplaceBlocksDiagnostics
				.diagnoseValue("biome(minecraft:plains)=minecraft:dirt", false)
				.isError());
	}
}
