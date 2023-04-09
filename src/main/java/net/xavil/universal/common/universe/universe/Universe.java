package net.xavil.universal.common.universe.universe;

import java.util.Random;

import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.GalaxyType;
import net.xavil.universal.common.universe.galaxy.StartingSystemGalaxyGenerationLayer;
import net.xavil.universal.common.universe.id.SystemId;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.common.universe.id.UniverseSectorId;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.util.Disposable;
import net.xavil.util.FastHasher;
import net.xavil.util.Option;
import net.xavil.util.collections.Vector;
import net.xavil.util.collections.interfaces.ImmutableList;
import net.xavil.util.math.Vec3;
import net.xavil.util.math.Vec3i;

public abstract class Universe implements Disposable {

	// ~388 galaxies per 100 Zm^3
	public static final double VOLUME_LENGTH_ZM = 10;
	public static final int ATTEMPT_COUNT = 10000;

	public static final boolean IS_UNIVERSE_GEN_ASYNC = false;

	public long celestialTimeTicks = 0;
	public final UniverseSectorManager sectorManager = new UniverseSectorManager(this);
	public final Disposable.Multi disposer = new Disposable.Multi();

	@Override
	public void dispose() {
		this.disposer.dispose();
	}

	public abstract long getCommonUniverseSeed();

	public abstract long getUniqueUniverseSeed();

	public abstract StartingSystemGalaxyGenerationLayer getStartingSystemGenerator();

	public void tick(ProfilerFiller profiler) {
		this.celestialTimeTicks += 1;
		this.sectorManager.tick(profiler);
	}

	public double getCelestialTime(float partialTick) {
		return (this.celestialTimeTicks + partialTick) / 20.0;
		// return 60.0 * (this.celestialTimeTicks + partialTick) / 20.0;
		// return 60.0 * 60.0 * (this.celestialTimeTicks + partialTick) / 20.0;
	}

	public Option<StarSystem> loadSystem(Disposable.Multi disposer, SystemId id) {
		final var galaxyTicket = this.sectorManager.createGalaxyTicket(disposer, id.galaxySector());
		return this.sectorManager.forceLoad(galaxyTicket)
				.flatMap(galaxy -> galaxy.loadSystem(disposer, id.systemSector()));
	}

	public Option<StarSystem> getSystem(SystemId id) {
		return this.sectorManager.getGalaxy(id.galaxySector()).flatMap(galaxy -> galaxy.getSystem(id.systemSector()));
	}

	public Option<CelestialNode> getSystemNode(SystemNodeId id) {
		return this.sectorManager.getGalaxy(id.system().galaxySector())
				.flatMap(galaxy -> galaxy.getSystemNode(id.system().systemSector(), id.nodeId()));
	}

	// public StarSystem getSystem(SystemId id) {
	// var galaxyVolume = getVolumeAt(id.galaxySector().sectorPos());
	// var galaxy = galaxyVolume.getById(id.galaxySector().sectorId());
	// if (galaxy == null)
	// return null;
	// var systemVolume = galaxy.getFull().sectorManager.get(id.systemSector());
	// var system = systemVolume.getById(id.systemSector().sectorId());
	// if (system == null)
	// return null;
	// return system.getFull();
	// }

	// public CelestialNode getSystemNode(SystemNodeId id) {
	// var system = getSystem(id.system());
	// if (system == null)
	// return null;
	// return system.rootNode.lookup(id.nodeId());
	// }

	// public Octree<Lazy<Galaxy.Info, Galaxy>> getVolumeAt(Vec3i volumePos) {
	// return getVolumeAt(volumePos, true);
	// }

	// public Octree<Lazy<Galaxy.Info, Galaxy>> getVolumeAt(Vec3i volumePos, boolean
	// create) {
	// var volume = this.volume.get(volumePos);
	// if (create) {
	// this.volume.addTicket(volumePos, 0, 15);
	// }
	// if (volume == null && create) {
	// volume = this.volume.get(volumePos);
	// }
	// return volume;
	// }

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

	private final long sectorSeed(Vec3i volumeCoords) {
		return FastHasher.withSeed(getCommonUniverseSeed()).append(volumeCoords).currentHash();
	}

	private final long galaxySeed(UniverseSectorId id) {
		return FastHasher.withSeed(sectorSeed(id.sectorPos())).appendInt(id.id()).currentHash();
	}

	public final ImmutableList<UniverseSector.InitialElement> generateSectorElements(Vec3i volumeCoords) {
		final var volumeMin = volumeCoords.lowerCorner().mul(VOLUME_LENGTH_ZM);
		final var volumeMax = volumeMin.add(VOLUME_LENGTH_ZM, VOLUME_LENGTH_ZM, VOLUME_LENGTH_ZM);

		final var elements = new Vector<UniverseSector.InitialElement>();
		final var random = new Random(sectorSeed(volumeCoords));

		final var maxDensity = ATTEMPT_COUNT / (VOLUME_LENGTH_ZM * VOLUME_LENGTH_ZM * VOLUME_LENGTH_ZM);
		for (var i = 0; i < ATTEMPT_COUNT; ++i) {
			final var galaxyPos = Vec3.random(random, volumeMin, volumeMax);
			final var density = sampleDensity(galaxyPos);
			if (density >= random.nextDouble(0, maxDensity)) {
				final var id = new UniverseSectorId(volumeCoords, elements.size());
				final var info = generateGalaxyInfo();
				final var seed = galaxySeed(id);
				elements.push(new UniverseSector.InitialElement(galaxyPos, info, seed));
			}
		}

		return elements;
	}

	public final Galaxy.Info generateGalaxyInfo() {
		final var random = new Random(getCommonUniverseSeed());

		final var info = new Galaxy.Info();
		final var typeIndex = random.nextInt(GalaxyType.values().length);
		info.type = GalaxyType.values()[typeIndex];
		info.ageMya = random.nextInt(100, 10000);

		return info;
	}

	public final Galaxy generateGalaxy(UniverseSectorId galaxyId, Galaxy.Info info) {
		final var random = new Random(getCommonUniverseSeed());
		return new Galaxy(this, galaxyId, info, info.type.createDensityFields(info.ageMya, random));
	}

}
