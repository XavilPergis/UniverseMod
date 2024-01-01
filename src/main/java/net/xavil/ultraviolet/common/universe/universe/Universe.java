package net.xavil.ultraviolet.common.universe.universe;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.SplittableRng;
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
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.Vec3i;

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

	public Maybe<SpaceStation> getStation(int id) {
		return this.spaceStations.get(id);
	}

	public Maybe<Integer> createStation(String name, StationLocation location) {
		if (this.spaceStations.values().any(station -> station.name == name))
			return Maybe.none();
		
		while (this.spaceStations.containsKey(this.nextStationId)) {
			this.nextStationId += 1;
		}
		final var id = this.nextStationId;
		this.nextStationId += 1;

		final var level = createLevelForStation(name, id);
		final var station = new SpaceStation(this, level, name, location);
		this.spaceStations.insert(id, station);
		return Maybe.some(id);
	}

	public Maybe<SpaceStation> getStationByName(String name) {
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

	public Maybe<StarSystem> loadSystem(Disposable.Multi disposer, SystemId id) {
		final var galaxyTicket = this.sectorManager.createGalaxyTicket(disposer, id.universeSector());
		return this.sectorManager.forceLoad(galaxyTicket)
				.flatMap(galaxy -> galaxy.loadSystem(disposer, id.galaxySector()));
	}

	public Maybe<Galaxy> loadGalaxy(Disposable.Multi disposer, UniverseSectorId id) {
		final var galaxyTicket = this.sectorManager.createGalaxyTicket(disposer, id);
		return this.sectorManager.forceLoad(galaxyTicket);
	}

	public Maybe<StarSystem> getSystem(SystemId id) {
		return this.sectorManager.getGalaxy(id.universeSector()).flatMap(galaxy -> galaxy.getSystem(id.galaxySector()));
	}

	public Maybe<Vec3> getSystemPos(SystemId id) {
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

	public Maybe<CelestialNode> getSystemNode(SystemNodeId id) {
		return this.sectorManager.getGalaxy(id.system().universeSector())
				.flatMap(galaxy -> galaxy.getSystemNode(id.system().galaxySector(), id.nodeId()));
	}

	protected static Vec3 randomVec(Random random) {
		var x = random.nextDouble(0, VOLUME_LENGTH_ZM);
		var y = random.nextDouble(0, VOLUME_LENGTH_ZM);
		var z = random.nextDouble(0, VOLUME_LENGTH_ZM);
		return new Vec3(x, y, z);
	}

	// galaxies per Zm^3
	private static double sampleDensity(Vec3 volumeOffsetZm) {
		// TODO: use a noise field or something
		// ridged noise would probably work well, to sorta emulate galactic filaments
		return 3.88;
	}

	public final ImmutableList<UniverseSector.InitialElement> generateSectorElements(Vec3i volumeCoords) {
		final var rng = new SplittableRng(this.getCommonUniverseSeed());
		rng.advanceWith(volumeCoords.hash());

		final var volumeMin = volumeCoords.lowerCorner().mul(VOLUME_LENGTH_ZM);
		final var volumeMax = volumeCoords.upperCorner().mul(VOLUME_LENGTH_ZM);

		final var elements = new Vector<UniverseSector.InitialElement>();

		final var maxDensity = ATTEMPT_COUNT / Math.pow(VOLUME_LENGTH_ZM, 3);
		for (var i = 0; i < ATTEMPT_COUNT; ++i) {
			rng.advance();
			final var galaxyPos = Vec3.random(rng, volumeMin, volumeMax);
			final var density = sampleDensity(galaxyPos);
			if (density >= rng.uniformDouble("density", 0, maxDensity)) {
				final var info = generateGalaxyInfo(rng);
				elements.push(new UniverseSector.InitialElement(galaxyPos, info));
			}
		}

		return elements;
	}

	public final Galaxy.Info generateGalaxyInfo(SplittableRng rng) {
		// final var galaxyType = rng.uniformEnum("type", GalaxyType.class);
		// final var galaxyType = GalaxyType.ELLIPTICAL;
		final var galaxyType = GalaxyType.SPIRAL;
		final var seed = rng.uniformLong("seed");
		final var age = rng.uniformDouble("age", 100, 10000);
		return new Galaxy.Info(galaxyType, seed, age);
	}

	public final Galaxy generateGalaxy(UniverseSectorId galaxyId, Galaxy.Info info) {
		final var rng = new SplittableRng(getCommonUniverseSeed());
		rng.advanceWith(galaxyId.sectorPos().x);
		rng.advanceWith(galaxyId.sectorPos().y);
		rng.advanceWith(galaxyId.sectorPos().z);
		rng.advanceWith(galaxyId.id());
		return new Galaxy(this, galaxyId, info, info.type.createDensityFields(info.ageMya, rng));
	}

}
