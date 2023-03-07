package net.xavil.universal.common.universe.galaxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.util.Mth;
import net.xavil.universal.common.universe.DensityFields;
import net.xavil.universal.common.universe.Lazy;
import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.id.SectorId;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.universe.Universe;
import net.xavil.util.Units;
import net.xavil.util.math.Vec3i;

public class Galaxy {

	public static final double TM_PER_SECTOR = Units.Tm_PER_ly * 10;
	public static final double TM_PER_SECTOR_3 = TM_PER_SECTOR * TM_PER_SECTOR * TM_PER_SECTOR;

	public static class Info {
		public GalaxyType type;
		public double ageMya;
	}

	public final Universe parentUniverse;
	public final Info info;
	public final DensityFields densityFields;
	public final SectorId galaxyId;

	private final List<GalaxyGenerationLayer> generationLayers = new ArrayList<>();

	public final TicketedVolume<Lazy<StarSystem.Info, StarSystem>> volume;

	public Galaxy(Universe parentUniverse, SectorId galaxyId, Info info, DensityFields densityFields) {
		this.parentUniverse = parentUniverse;
		this.galaxyId = galaxyId;
		this.info = info;
		this.densityFields = densityFields;

		this.volume = new TicketedVolume<>() {
			@Override
			public Octree<Lazy<StarSystem.Info, StarSystem>> generateVolume(Vec3i sectorPos) {
				return Galaxy.this.generateVolume(sectorPos);
			}
		};

		generationLayers.add(parentUniverse.getStartingSystemGenerator());
		generationLayers.add(new BaseGalaxyGenerationLayer(this, densityFields));
	}

	private long volumeSeed(Vec3i volumeCoords) {
		var seed = Mth.murmurHash3Mixer(this.parentUniverse.getCommonUniverseSeed());
		seed ^= Mth.murmurHash3Mixer(seed ^ (long) volumeCoords.x);
		seed ^= Mth.murmurHash3Mixer(seed ^ (long) volumeCoords.y);
		seed ^= Mth.murmurHash3Mixer(seed ^ (long) volumeCoords.z);
		return seed;
	}

	public void tick() {
		this.volume.tick();
	}

	public Octree<Lazy<StarSystem.Info, StarSystem>> getVolumeAt(Vec3i volumePos) {
		return getVolumeAt(volumePos, true);
	}

	public Octree<Lazy<StarSystem.Info, StarSystem>> getVolumeAt(Vec3i volumePos, boolean create) {
		var volume = this.volume.get(volumePos);
		if (create) {
			this.volume.addTicket(volumePos, 0, 15);
		}
		if (volume == null && create) {
			volume = this.volume.get(volumePos);
		}
		return volume;
	}

	private Octree<Lazy<StarSystem.Info, StarSystem>> generateVolume(Vec3i volumeCoords) {
		final var volumeMin = volumeCoords.lowerCorner().mul(TM_PER_SECTOR);
		final var volumeMax = volumeMin.add(TM_PER_SECTOR, TM_PER_SECTOR, TM_PER_SECTOR);
		var volume = new Octree<Lazy<StarSystem.Info, StarSystem>>(volumeMin, volumeMax);

		var random = new Random(volumeSeed(volumeCoords));
		var ctx = new GalaxyGenerationLayer.Context(this, random, volumeCoords, volume);
		for (var layer : this.generationLayers) {
			layer.generateInto(ctx, (pos, system) -> {
				var layerElements = volume.elements.get(layer.layerId);
				var elementIndex = layerElements == null ? 0 : layerElements.size();
				var thisId = new Octree.Id(layer.layerId, elementIndex);
				system.evaluationHook = () -> volume.markedElements.add(thisId);
				volume.insert(pos, layer.layerId, system);
			});
		}

		return volume;
	}

}
