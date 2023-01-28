package net.xavil.universal.common.universe.universe;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.common.universe.Lazy;
import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.UniverseId;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.GalaxyType;

public abstract class Universe {

	// ~388 galaxies per 100 Zm^3
	public static final double VOLUME_LENGTH_ZM = 10;
	public static final int ATTEMPT_COUNT = 10000;

	protected final Map<Vec3i, Octree<Lazy<Galaxy.Info, Galaxy>>> loadedVolumes = new HashMap<>();

	public abstract long getCommonUniverseSeed();

	public abstract long getUniqueUniverseSeed();

	public UniverseId.SectorId getStartingGalaxyId() {
		var random = new Random(getUniqueUniverseSeed());
		var x = (int) (1000 * random.nextGaussian());
		var y = (int) (1000 * random.nextGaussian());
		var z = (int) (1000 * random.nextGaussian());
		var pos = new Vec3i(x, y, z);
		var volume = getOrGenerateGalaxyVolume(pos);
		var ids = volume.streamIds().toArray();
		var id = ids[random.nextInt(ids.length)];
		return new UniverseId.SectorId(pos, id);
	}

	public UniverseId.SectorId getStartingSystemId(Galaxy galaxy) {
		// var galaxyId = getStartingGalaxyId();
		// var galaxyVolume = getOrGenerateGalaxyVolume(galaxyId.sectorPos());
		// var galaxy = galaxyVolume.fullById(galaxyId.sectorId());
		var random = new Random(getUniqueUniverseSeed() + 1);
		var x = (int) (1000 * random.nextGaussian());
		var y = (int) (1000 * random.nextGaussian());
		var z = (int) (1000 * random.nextGaussian());
		var pos = new Vec3i(x, y, z);
		var volume = galaxy.getOrGenerateVolume(pos);
		var ids = volume.streamIds().toArray();
		var id = ids[random.nextInt(ids.length)];
		return new UniverseId.SectorId(pos, id);
	}

	private static Vec3 randomVec(Random random) {
		var x = random.nextDouble(0, VOLUME_LENGTH_ZM);
		var y = random.nextDouble(0, VOLUME_LENGTH_ZM);
		var z = random.nextDouble(0, VOLUME_LENGTH_ZM);
		return new Vec3(x, y, z);
	}

	// galaxies per Zm^3
	private static double sampleDensity(Vec3 volumeOffsetZm) {
		// TODO: use a noise field or something
		return 3.88;
	}

	public final Octree<Lazy<Galaxy.Info, Galaxy>> getOrGenerateGalaxyVolume(Vec3i volumeCoords) {
		if (this.loadedVolumes.containsKey(volumeCoords)) {
			return this.loadedVolumes.get(volumeCoords);
		}

		var volumeMin = Vec3.atLowerCornerOf(volumeCoords).scale(VOLUME_LENGTH_ZM);
		var volumeMax = volumeMin.add(VOLUME_LENGTH_ZM, VOLUME_LENGTH_ZM, VOLUME_LENGTH_ZM);

		var octree = new Octree<Lazy<Galaxy.Info, Galaxy>>(volumeMin, volumeMax);

		var randomSeed = getCommonUniverseSeed();
		randomSeed ^= Mth.murmurHash3Mixer((long) volumeCoords.getX());
		randomSeed ^= Mth.murmurHash3Mixer((long) volumeCoords.getY());
		randomSeed ^= Mth.murmurHash3Mixer((long) volumeCoords.getZ());
		var random = new Random(randomSeed);

		var maxDensity = ATTEMPT_COUNT / (VOLUME_LENGTH_ZM * VOLUME_LENGTH_ZM * VOLUME_LENGTH_ZM);
		for (var i = 0; i < ATTEMPT_COUNT; ++i) {
			var galaxyPos = volumeMin.add(randomVec(random));

			var density = sampleDensity(galaxyPos);
			if (density >= random.nextDouble(0, maxDensity)) {
				Function<Galaxy.Info, Galaxy> lazyFunction = info -> generateGalaxy(galaxyPos, info);
				var lazy = new Lazy<>(generateGalaxyInfo(galaxyPos), lazyFunction);
				octree.insert(galaxyPos, lazy);
			}
		}

		this.loadedVolumes.put(volumeCoords, octree);
		return octree;
	}

	public final Galaxy.Info generateGalaxyInfo(Vec3 galaxyOffsetZm) {
		var random = new Random(getCommonUniverseSeed());

		var info = new Galaxy.Info();
		var typeIndex = random.nextInt(GalaxyType.values().length);
		info.type = GalaxyType.values()[typeIndex];
		info.ageMya = random.nextInt(100, 10000);

		return info;
	}

	public final Galaxy generateGalaxy(Vec3 galaxyOffsetZm, Galaxy.Info info) {
		var random = new Random(getCommonUniverseSeed());
		return new Galaxy(this, info, info.type.createDensityField(random));
	}

}
