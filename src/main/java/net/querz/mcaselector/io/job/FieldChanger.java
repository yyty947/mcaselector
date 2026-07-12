package net.querz.mcaselector.io.job;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.progress.Progress;
import net.querz.mcaselector.util.progress.Timer;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.text.Translation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.List;
import java.util.function.Consumer;

public final class FieldChanger {

	private static final Logger LOGGER = LogManager.getLogger(FieldChanger.class);

	private FieldChanger() {}

	public static void changeNBTFields(List<Field<?>> fields, boolean force, Selection selection, Progress progressChannel, boolean headless) {
		WorldDirectories wd = ConfigProvider.WORLD.getWorldDirs();
		Selection relightSelection = fields.stream().anyMatch(field -> field.getType() == FieldType.REPLACE_BLOCKS) && selection != null
				? expandSelectionByOne(selection.getTrueSelection(wd))
				: null;
		RegionDirectories[] rd = wd.listRegions(relightSelection == null ? selection : relightSelection);
		if (rd == null || rd.length == 0) {
			if (headless) {
				progressChannel.done("no files");
			} else {
				progressChannel.done(Translation.DIALOG_PROGRESS_NO_FILES.toString());
			}
			return;
		}

		JobHandler.clearQueues();

		progressChannel.setMax(rd.length);
		progressChannel.updateProgress(rd[0].getLocationAsFileName(), 0);

		Consumer<Throwable> errorHandler = t -> progressChannel.incrementProgress("error");

		for (RegionDirectories r : rd) {
			MCAFieldChangeProcessJob job = new MCAFieldChangeProcessJob(r, fields, force, selection, relightSelection, progressChannel);
			job.errorHandler = errorHandler;
			JobHandler.addJob(job);
		}
	}

	static Selection expandSelectionByOne(Selection selection) {
		Selection expanded = new Selection();
		for (var entry : selection) {
			Point2i region = new Point2i(entry.getLongKey());
			if (entry.getValue() == null) {
				for (int index = 0; index < 1024; index++) {
					addSquare(expanded, new Point2i(index).add(region.regionToChunk()));
				}
			} else {
				for (int index : entry.getValue()) {
					addSquare(expanded, new Point2i(index).add(region.regionToChunk()));
				}
			}
		}
		return expanded;
	}

	private static void addSquare(Selection selection, Point2i center) {
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				selection.addChunk(center.add(x, z));
			}
		}
	}

	public static class MCAFieldChangeProcessJob extends ProcessDataJob {

		private final Progress progressChannel;
		private final List<Field<?>> fields;
		private final boolean force;
		private final Selection selection;
		private final Selection relightSelection;

		private MCAFieldChangeProcessJob(RegionDirectories dirs, List<Field<?>> fields, boolean force, Selection selection,
				Selection relightSelection, Progress progressChannel) {
			super(dirs, PRIORITY_LOW);
			this.fields = fields;
			this.force = force;
			this.selection = selection;
			this.relightSelection = relightSelection;
			this.progressChannel = progressChannel;
		}

		@Override
		public boolean execute() {
			Selection processingSelection = relightSelection == null ? selection : relightSelection;
			if (processingSelection != null) {
				Point2i location = getRegionDirectories().getLocation();
				if (!processingSelection.isAnyChunkInRegionSelected(location)) {
					LOGGER.debug("will not apply nbt changes to {}", getRegionDirectories().getLocationAsFileName());
					progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
					return true;
				}
			}

			//load MCAFile
			try {
				Region region = Region.loadRegion(getRegionDirectories());

				region.applyFieldChanges(fields, force, selection, relightSelection);

				MCAFieldChangeSaveJob job = new MCAFieldChangeSaveJob(getRegionDirectories(), region, progressChannel);
				job.errorHandler = errorHandler;
				JobHandler.executeSaveData(job);
				return false;
			} catch (Exception ex) {
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				LOGGER.warn("error changing fields in {}", getRegionDirectories().getLocationAsFileName(), ex);
			}
			return true;
		}
	}

	public static class MCAFieldChangeSaveJob extends SaveDataJob<Region> {

		private final Progress progressChannel;

		private MCAFieldChangeSaveJob(RegionDirectories file, Region region, Progress progressChannel) {
			super(file, region);
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Timer t = new Timer();
			try {
				getData().saveWithTempFiles();
			} catch (Exception ex) {
				LOGGER.warn("failed to save changed fields for {}", getRegionDirectories().getLocationAsFileName(), ex);
			}
			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			LOGGER.debug("took {} to save data for {}", t, getRegionDirectories().getLocationAsFileName());
		}
	}
}
