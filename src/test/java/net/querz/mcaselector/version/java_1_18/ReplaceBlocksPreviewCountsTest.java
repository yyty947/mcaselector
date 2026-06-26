package net.querz.mcaselector.version.java_1_18;

import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReplaceBlocksPreviewCountsTest {

	@Test
	void countsRulesSeparatelyWhileAggregateCountsBlockPositionsOnce() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(ChunkFilter.BlockReplaceSource.literalName("minecraft:stone"), new ChunkFilter.BlockReplaceData("minecraft:dirt"));
		replace.put(ChunkFilter.BlockReplaceSource.regexName("minecraft:.*"), new ChunkFilter.BlockReplaceData("minecraft:gold_block"));

		ChunkFilter.BlockReplacePreviewData preview = new PreviewBlocks().preview(root(), sections(), replace);

		assertEquals(4096, preview.getBlocks());
		assertEquals(1, preview.getSections());
		assertEquals(4096, preview.getOverlappingBlocks());
		assertEquals(2, preview.getRules().size());
		assertEquals(1, preview.getRules().get(0).getIndex());
		assertEquals(ChunkFilter.BlockReplaceSourceType.LITERAL_NAME, preview.getRules().get(0).getSourceType());
		assertEquals("literal(minecraft:stone)", preview.getRules().get(0).getSourceText());
		assertEquals("minecraft:dirt", preview.getRules().get(0).getTargetText());
		assertEquals(4096, preview.getRules().get(0).getBlocks());
		assertEquals(2, preview.getRules().get(1).getIndex());
		assertEquals(ChunkFilter.BlockReplaceSourceType.REGEX_NAME, preview.getRules().get(1).getSourceType());
		assertEquals("regex(minecraft:.*)", preview.getRules().get(1).getSourceText());
		assertEquals("minecraft:gold_block", preview.getRules().get(1).getTargetText());
		assertEquals(4096, preview.getRules().get(1).getBlocks());
	}

	private CompoundTag root() {
		CompoundTag root = new CompoundTag();
		root.putInt("xPos", 0);
		root.putInt("zPos", 0);
		return root;
	}

	private ListTag sections() {
		ListTag sections = new ListTag();
		CompoundTag section = new CompoundTag();
		section.putByte("Y", (byte) 0);

		CompoundTag blockStates = new CompoundTag();
		ListTag palette = new ListTag();
		CompoundTag stone = new CompoundTag();
		stone.putString("Name", "minecraft:stone");
		palette.add(stone);
		blockStates.put("palette", palette);
		section.put("block_states", blockStates);

		sections.add(section);
		return sections;
	}

	private static class PreviewBlocks extends ChunkFilter_21w37a.Blocks {

		private ChunkFilter.BlockReplacePreviewData preview(CompoundTag root, ListTag sections, Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace) {
			return previewReplaceBlocks(root, sections, "block_entities", replace);
		}
	}
}
