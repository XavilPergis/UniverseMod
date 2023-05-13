package net.xavil.ultraviolet.common.universe.universe;

import java.util.Random;

import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxyType;
import net.xavil.ultraviolet.common.universe.galaxy.StartingSystemGalaxyGenerationLayer;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.common.universe.id.UniverseSectorId;
import net.xavil.ultraviolet.common.universe.station.SpaceStation;
import net.xavil.ultraviolet.common.universe.station.StationLocation;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.util.Disposable;
import net.xavil.util.Option;
import net.xavil.util.collections.Vector;
import net.xavil.util.collections.interfaces.ImmutableList;
import net.xavil.util.collections.interfaces.MutableList;
import net.xavil.util.collections.interfaces.MutableMap;
import net.xavil.util.hash.FastHasher;
import net.xavil.util.math.matrices.Vec3;
import net.xavil.util.math.matrices.Vec3i;

public abstract class Universe implements Disposable {

	// ~388 galaxies per 100 Zm^3
	public static final double VOLUME_LENGTH_ZM = 10;
	public static final int ATTEMPT_COUNT = 10000;

	public static final boolean IS_UNIVERSE_GEN_ASYNC = true;

	public double celestialTimeRate = 1;
	public double celestialTime = 0, lastCelestialTime = 0;
	public final UniverseSectorManager sectorManager = new UniverseSectorManager(this);
	public final Disposable.Multi disposer = new Disposable.Multi();
	protected final MutableMap<Integer, SpaceStation> spaceStations = MutableMap.hashMap();
	private int nextStationId = 0;

	@Override
	public void close() {
		this.disposer.close();
	}

	public abstract long getCommonUniverseSeed();

	public abstract long getUniqueUniverseSeed();

	public abstract StartingSystemGalaxyGenerationLayer getStartingSystemGenerator();

	public abstract Level createLevelForStation(String name, int id);

	public Option<SpaceStation> getStation(int id) {
		return this.spaceStations.get(id);
	}

	public Option<Integer> createStation(String name, StationLocation location) {
		if (this.spaceStations.values().any(station -> station.name == name))
			return Option.none();
		
		while (this.spaceStations.containsKey(this.nextStationId)) {
			this.nextStationId += 1;
		}
		final var id = this.nextStationId;
		this.nextStationId += 1;

		final var level = createLevelForStation(name, id);
		final var station = new SpaceStation(this, level, name, location);
		this.spaceStations.insert(id, station);
		return Option.some(id);
	}

	public Option<SpaceStation> getStationByName(String name) {
		return this.spaceStations.values().find(station -> station.name.equals(name));
	}

	public void tick(ProfilerFiller profiler, boolean isPaused) {
		if (!isPaused) {
			this.lastCelestialTime = this.celestialTime;
			this.celestialTime += this.celestialTimeRate / 20.0;
			this.spaceStations.values().forEach(station -> station.tick());
		}
		this.sectorManager.tick(profiler);
	}

	public final double getCelestialTime() {
		return getCelestialTime(0);
	}

	public double getCelestialTime(float partialTick) {
		return Mth.lerp(partialTick, this.lastCelestialTime, this.celestialTime); 
	}

	public Option<StarSystem> loadSystem(Disposable.Multi disposer, SystemId id) {
		final var galaxyTicket = this.sectorManager.createGalaxyTicket(disposer, id.universeSector());
		return this.sectorManager.forceLoad(galaxyTicket)
				.flatMap(galaxy -> galaxy.loadSystem(disposer, id.galaxySector()));
	}

	public Option<Galaxy> loadGalaxy(Disposable.Multi disposer, UniverseSectorId id) {
		final var galaxyTicket = this.sectorManager.createGalaxyTicket(disposer, id);
		return this.sectorManager.forceLoad(galaxyTicket);
	}

	public Option<StarSystem> getSystem(SystemId id) {
		return this.sectorManager.getGalaxy(id.universeSector()).flatMap(galaxy -> galaxy.getSystem(id.galaxySector()));
	}

	public Option<Vec3> getSystemPos(SystemId id) {
		return this.sectorManager.getGalaxy(id.universeSector())
				.flatMap(galaxy -> galaxy.getSystemPos(id.galaxySector()));
	}

	// public Option<Vec3> getSystemNodePos(SystemNodeId id, double time) {
	// final var systemPos = getSystemPos(id.system()).unwrapOrNull();
	// final var system = getSystem(id.system()).unwrapOrNull();
	// if (systemPos == null || system == null)
	// return Option.none();
	// final var node = system.rootNode.lookup(id.nodeId());
	// if (node == null)
	// return Option.none();
	// system.rootNode.updatePositions(time);
	// return Option.some(systemPos.add(node.position));
	// }

	public Option<CelestialNode> getSystemNode(SystemNodeId id) {
		return this.sectorManager.getGalaxy(id.system().universeSector())
				.flatMap(galaxy -> galaxy.getSystemNode(id.system().galaxySector(), id.nodeId()));
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
