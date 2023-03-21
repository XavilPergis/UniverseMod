package net.xavil.universal.common.universe.universe;

import java.util.Random;

import net.minecraft.util.Mth;
import net.xavil.universal.common.universe.Lazy;
import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.GalaxyType;
import net.xavil.universal.common.universe.galaxy.StartingSystemGalaxyGenerationLayer;
import net.xavil.universal.common.universe.galaxy.TicketedVolume;
import net.xavil.universal.common.universe.id.SectorId;
import net.xavil.universal.common.universe.id.SystemId;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.util.math.Vec3;
import net.xavil.util.math.Vec3i;

public abstract class Universe {

	// ~388 galaxies per 100 Zm^3
	public static final double VOLUME_LENGTH_ZM = 10;
	public static final int ATTEMPT_COUNT = 10000;

	public final TicketedVolume<Lazy<Galaxy.Info, Galaxy>> volume = new TicketedVolume<>() {
		@Override
		public Octree<Lazy<Galaxy.Info, Galaxy>> generateVolume(Vec3i sectorPos) {
			return Universe.this.generateGalaxyVolume(sectorPos);
		}
	};

	public long celestialTimeTicks = 0;

	public abstract long getCommonUniverseSeed();

	public abstract long getUniqueUniverseSeed();

	public abstract StartingSystemGalaxyGenerationLayer getStartingSystemGenerator();

	public void tick() {
		this.celestialTimeTicks += 1;
		this.volume.tick();
		this.volume.streamLoadedSectors().forEach(pos -> {
			var volume = this.volume.get(pos);
			for (var id : volume.markedElements) {
				var galaxy = volume.getById(id);
				if (galaxy.hasFull()) {
					galaxy.getFull().tick();
				}
			}
		});
	}

	public double getCelestialTime(float partialTick) {
		return (this.celestialTimeTicks + partialTick) / 20.0;
		// return 60.0 * (this.celestialTimeTicks + partialTick) / 20.0;
		// return 60.0 * 60.0 * (this.celestialTimeTicks + partialTick) / 20.0;
	}

	public StarSystem getSystem(SystemId id) {
		var galaxyVolume = getVolumeAt(id.galaxySector().sectorPos());
		var galaxy = galaxyVolume.getById(id.galaxySector().sectorId());
		if (galaxy == null)
			return null;
		var systemVolume = galaxy.getFull().getVolumeAt(id.systemSector().sectorPos());
		var system = systemVolume.getById(id.systemSector().sectorId());
		if (system == null)
			return null;
		return system.getFull();
	}

	public CelestialNode getSystemNode(SystemNodeId id) {
		var system = getSystem(id.system());
		if (system == null)
			return null;
		return system.rootNode.lookup(id.nodeId());
	}

	public Octree<Lazy<Galaxy.Info, Galaxy>> getVolumeAt(Vec3i volumePos) {
		return getVolumeAt(volumePos, true);
	}

	public Octree<Lazy<Galaxy.Info, Galaxy>> getVolumeAt(Vec3i volumePos, boolean create) {
		var volume = this.volume.get(volumePos);
		if (create) {
			this.volume.addTicket(volumePos, 0, 15);
		}
		if (volume == null && create) {
			volume = this.volume.get(volumePos);
		}
		return volume;
	}

	protected static Vec3 randomVec(Random random) {
		var x = random.nextDouble(0, VOLUME_LENGTH_ZM);
		var y = random.nextDouble(0, VOLUME_LENGTH_ZM);
		var z = random.nextDouble(0, VOLUME_LENGTH_ZM);
		return Vec3.from(x, y, z);
	}

	// galaxies per Zm^3
	private static double sampleDensity(Vec3 volumeOffsetZm) {
		// TODO: use a noise field or something
		return 3.88;
	}

	private final Octree<Lazy<Galaxy.Info, Galaxy>> generateGalaxyVolume(Vec3i volumeCoords) {
		var volumeMin = volumeCoords.lowerCorner().mul(VOLUME_LENGTH_ZM);
		var volumeMax = volumeMin.add(VOLUME_LENGTH_ZM, VOLUME_LENGTH_ZM, VOLUME_LENGTH_ZM);

		var octree = new Octree<Lazy<Galaxy.Info, Galaxy>>(volumeMin, volumeMax);

		var randomSeed = getCommonUniverseSeed();
		randomSeed ^= Mth.murmurHash3Mixer((long) volumeCoords.x);
		randomSeed ^= Mth.murmurHash3Mixer((long) volumeCoords.y);
		randomSeed ^= Mth.murmurHash3Mixer((long) volumeCoords.z);
		var random = new Random(randomSeed);

		int currentId = 0;
		var maxDensity = ATTEMPT_COUNT / (VOLUME_LENGTH_ZM * VOLUME_LENGTH_ZM * VOLUME_LENGTH_ZM);
		for (var i = 0; i < ATTEMPT_COUNT; ++i) {
			var galaxyPos = volumeMin.add(randomVec(random));

			var density = sampleDensity(galaxyPos);
			if (density >= random.nextDouble(0, maxDensity)) {
				var id = new SectorId(volumeCoords, new Octree.Id(0, currentId));
				var lazy = new Lazy<>(generateGalaxyInfo(), info -> generateGalaxy(id, info));
				final var currentId2 = currentId; // java moment
				lazy.evaluationHook = () -> octree.markedElements.add(new Octree.Id(0, currentId2));
				octree.insert(galaxyPos, 0, lazy);
				currentId += 1;
			}
		}

		return octree;
	}

	public final Galaxy.Info generateGalaxyInfo() {
		var random = new Random(getCommonUniverseSeed());

		var info = new Galaxy.Info();
		var typeIndex = random.nextInt(GalaxyType.values().length);
		info.type = GalaxyType.values()[typeIndex];
		info.ageMya = random.nextInt(100, 10000);

		return info;
	}

	public final Galaxy generateGalaxy(SectorId galaxyId, Galaxy.Info info) {
		var random = new Random(getCommonUniverseSeed());
		return new Galaxy(this, galaxyId, info, info.type.createDensityFields(random));
	}

}
