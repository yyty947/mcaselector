package net.querz.mcaselector.io.job;

import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.util.point.Point2i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldChangerTest {

	@Test
	void expandsSingleChunkToItsEightNeighbors() {
		Selection selection = new Selection();
		selection.addChunk(new Point2i(10, 20));

		Selection expanded = FieldChanger.expandSelectionByOne(selection);

		assertEquals(9, expanded.count());
		for (int x = 9; x <= 11; x++) {
			for (int z = 19; z <= 21; z++) {
				assertTrue(expanded.isChunkSelected(x, z));
			}
		}
	}

	@Test
	void expandsAcrossRegionBoundaries() {
		Selection selection = new Selection();
		selection.addChunk(new Point2i(31, 31));

		Selection expanded = FieldChanger.expandSelectionByOne(selection);

		assertEquals(9, expanded.count());
		assertTrue(expanded.isChunkSelected(32, 31));
		assertTrue(expanded.isChunkSelected(31, 32));
		assertTrue(expanded.isChunkSelected(32, 32));
	}

	@Test
	void expandsRectangularSelectionByOneChunkRing() {
		Selection selection = new Selection();
		for (int x = 4; x <= 12; x++) {
			for (int z = 8; z <= 16; z++) {
				selection.addChunk(new Point2i(x, z));
			}
		}

		Selection expanded = FieldChanger.expandSelectionByOne(selection);

		assertEquals(121, expanded.count());
		assertTrue(expanded.isChunkSelected(3, 7));
		assertTrue(expanded.isChunkSelected(13, 17));
	}
}
