package net.xavil.universal.common.universe.galaxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.common.universe.DensityField3;
import net.xavil.universal.common.universe.Lazy;
import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.UniverseId;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.universe.BaseGalaxyGenerationLayer;
import net.xavil.universal.common.universe.universe.GalaxyGenerationLayer;
import net.xavil.universal.common.universe.universe.Universe;

public class Galaxy {

	public static final double TM_PER_SECTOR = Units.TM_PER_LY * 10;
	public static final double TM_PER_SECTOR_3 = TM_PER_SECTOR * TM_PER_SECTOR * TM_PER_SECTOR;

	public static class Info {
		public GalaxyType type;
		public double ageMya;
	}

	public final Universe parentUniverse;
	public final Info info;
	public final UniverseId.SectorId galaxyId;

	private final List<GalaxyGenerationLayer> generationLayers = new ArrayList<>();

	public final TicketedVolume<Lazy<StarSystem.Info, StarSystem>> volume;

	public Galaxy(Universe parentUniverse, UniverseId.SectorId galaxyId, Info info, DensityField3 densityField) {
		this.parentUniverse = parentUniverse;
		this.galaxyId = galaxyId;
		this.info = info;

		this.volume = new TicketedVolume<>() {
			@Override
			public Octree<Lazy<StarSystem.Info, StarSystem>> generateVolume(Vec3i sectorPos) {
				return Galaxy.this.generateVolume(sectorPos);
			}
		};

		generationLayers.add(parentUniverse.getStartingSystemGenerator());
		generationLayers.add(new BaseGalaxyGenerationLayer(this, densityField));
	}

	private long volumeSeed(Vec3i volumeCoords) {
		var seed = Mth.murmurHash3Mixer(this.parentUniverse.getCommonUniverseSeed());
		seed ^= Mth.murmurHash3Mixer(seed ^ (long) volumeCoords.getX());
		seed ^= Mth.murmurHash3Mixer(seed ^ (long) volumeCoords.getY());
		seed ^= Mth.murmurHash3Mixer(seed ^ (long) volumeCoords.getZ());
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
		if (volume == null && create) {
			this.volume.addTicket(volumePos, 0, 1);
			volume = this.volume.get(volumePos);
		}
		return volume;
	}

	private Octree<Lazy<StarSystem.Info, StarSystem>> generateVolume(Vec3i volumeCoords) {
		final var volumeMin = Vec3.atLowerCornerOf(volumeCoords).scale(TM_PER_SECTOR);
		final var volumeMax = volumeMin.add(TM_PER_SECTOR, TM_PER_SECTOR, TM_PER_SECTOR);
		var volume = new Octree<Lazy<StarSystem.Info, StarSystem>>(volumeMin, volumeMax);

		var random = new Random(volumeSeed(volumeCoords));
		var ctx = new GalaxyGenerationLayer.Context(this, random, volumeCoords, volume);
		for (var layer : this.generationLayers) {
			layer.generateInto(ctx);
		}

		return volume;
	}

}
