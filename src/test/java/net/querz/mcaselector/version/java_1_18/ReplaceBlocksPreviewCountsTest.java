package net.querz.mcaselector.version.java_1_18;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.mca.RegionChunk;
import net.querz.mcaselector.util.point.Point2i;
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

	@Test
	void sourceTileFiltersUseOriginalBlockEntityPresence() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:stone")
						.withTileEntityMode(ChunkFilter.BlockReplaceTileEntityMode.REQUIRE_TILE_ENTITY),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:stone")
						.withTileEntityMode(ChunkFilter.BlockReplaceTileEntityMode.EXCLUDE_TILE_ENTITY),
				new ChunkFilter.BlockReplaceData("minecraft:gold_block"));

		CompoundTag root = root();
		root.put("block_entities", tileEntities(tile("minecraft:chest", 0, 0, 0)));

		ChunkFilter.BlockReplacePreviewData preview = new PreviewBlocks().preview(root, sections(), replace);

		assertEquals(4096, preview.getBlocks());
		assertEquals(1, preview.getTileEntityRemovals());
		assertEquals(0, preview.getTileEntityAdditions());
		assertEquals(0, preview.getTileEntityUpdates());
		assertEquals(1, preview.getRules().get(0).getBlocks());
		assertEquals(4095, preview.getRules().get(1).getBlocks());
	}

	@Test
	void tileToTilePreviewCountsUpdates() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:stone")
						.withTileEntityMode(ChunkFilter.BlockReplaceTileEntityMode.REQUIRE_TILE_ENTITY),
				new ChunkFilter.BlockReplaceData("minecraft:barrel", tile("minecraft:barrel", 0, 0, 0)));

		CompoundTag root = root();
		root.put("block_entities", tileEntities(tile("minecraft:chest", 0, 0, 0)));

		ChunkFilter.BlockReplacePreviewData preview = new PreviewBlocks().preview(root, sections(), replace);

		assertEquals(1, preview.getBlocks());
		assertEquals(0, preview.getTileEntityAdditions());
		assertEquals(0, preview.getTileEntityRemovals());
		assertEquals(1, preview.getTileEntityUpdates());
		assertEquals(1, preview.getRules().get(0).getBlocks());
	}

	@Test
	void tileTargetReplacementRemovesExistingBlockEntitiesBeforeAddingNewOne() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:stone")
						.withTileEntityMode(ChunkFilter.BlockReplaceTileEntityMode.REQUIRE_TILE_ENTITY),
				new ChunkFilter.BlockReplaceData("minecraft:barrel", tile("minecraft:barrel", 0, 0, 0)));

		CompoundTag root = root();
		root.putInt("DataVersion", 2844);
		root.put("sections", sections());
		root.put("block_entities", tileEntities(
				tile("minecraft:chest", 0, 0, 0),
				tile("minecraft:chest", 0, 0, 0)));
		RegionChunk region = new RegionChunk(new Point2i(0, 0));
		region.setData(root);

		new ChunkFilter_21w43a.Blocks().replaceBlocks(new ChunkData(new Point2i(0, 0), region, null, null, true), replace);

		ListTag blockEntities = (ListTag) root.get("block_entities");
		assertEquals(1, blockEntities.size());
		CompoundTag tile = blockEntities.getCompound(0);
		assertEquals("minecraft:barrel", tile.getString("id"));
		assertEquals(0, tile.getInt("x"));
		assertEquals(0, tile.getInt("y"));
		assertEquals(0, tile.getInt("z"));
	}

	@Test
	void previewCountsOnlyBlocksInsideYRange() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:stone").withYRange(0, 0),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));

		ChunkFilter.BlockReplacePreviewData preview = new PreviewBlocks().preview(root(), sections(), replace);

		assertEquals(256, preview.getBlocks());
		assertEquals(1, preview.getSections());
		assertEquals(256, preview.getRules().get(0).getBlocks());
	}

	@Test
	void previewCompletesSyntheticAirSectionsOnlyInsideYRange() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:air").withYRange(16, 16),
				new ChunkFilter.BlockReplaceData("minecraft:glass"));

		ListTag sections = sections();
		sections.add(incompleteSection(1));

		ChunkFilter.BlockReplacePreviewData preview = new PreviewBlocks().preview(root(), sections, replace);

		assertEquals(256, preview.getBlocks());
		assertEquals(1, preview.getSections());
		assertEquals(1, preview.getCompletedAirSections());
		assertEquals(256, preview.getRules().get(0).getBlocks());
	}

	@Test
	void replaceBlocksChangesOnlyBlocksInsideYRange() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:stone").withYRange(0, 0),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));

		CompoundTag root = root();
		root.putInt("DataVersion", 2844);
		root.put("sections", sections());
		RegionChunk region = new RegionChunk(new Point2i(0, 0));
		region.setData(root);
		PreviewBlocks blocks = new PreviewBlocks();

		blocks.replaceBlocks(new ChunkData(new Point2i(0, 0), region, null, null, true), replace);

		assertEquals("minecraft:dirt", blocks.blockAt(root, 0));
		assertEquals("minecraft:stone", blocks.blockAt(root, 256));
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

	private CompoundTag incompleteSection(int y) {
		CompoundTag section = new CompoundTag();
		section.putByte("Y", (byte) y);
		section.put("biomes", new CompoundTag());
		return section;
	}

	private ListTag tileEntities(CompoundTag... tiles) {
		ListTag tileEntities = new ListTag();
		for (CompoundTag tile : tiles) {
			tileEntities.add(tile);
		}
		return tileEntities;
	}

	private CompoundTag tile(String id, int x, int y, int z) {
		CompoundTag tile = new CompoundTag();
		tile.putString("id", id);
		tile.putInt("x", x);
		tile.putInt("y", y);
		tile.putInt("z", z);
		return tile;
	}

	private static class PreviewBlocks extends ChunkFilter_21w43a.Blocks {

		private ChunkFilter.BlockReplacePreviewData preview(CompoundTag root, ListTag sections, Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace) {
			return previewReplaceBlocks(root, sections, "block_entities", replace);
		}

		private String blockAt(CompoundTag root, int index) {
			CompoundTag section = ((ListTag) root.get("sections")).getCompound(0);
			CompoundTag blockStates = section.getCompound("block_states");
			return getBlockAt(index, blockStates.getLongArray("data"), blockStates.getListTag("palette")).getString("Name");
		}
	}
}
