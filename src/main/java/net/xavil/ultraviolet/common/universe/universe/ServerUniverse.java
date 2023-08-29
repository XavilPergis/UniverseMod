package net.xavil.ultraviolet.common.universe.universe;

import java.util.OptionalLong;
import java.util.Random;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.Rng;
import static net.xavil.hawklib.Units.*;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.dimension.DynamicDimensionManager;
import net.xavil.ultraviolet.common.level.EmptyChunkGenerator;
import net.xavil.ultraviolet.common.universe.WorldType;
import net.xavil.ultraviolet.common.universe.galaxy.StartingSystemGalaxyGenerationLayer;
import net.xavil.ultraviolet.common.universe.galaxy.SystemTicket;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.common.universe.id.UniverseSectorId;
import net.xavil.ultraviolet.common.universe.station.StationLocation;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.mixin.accessor.LevelAccessor;
import net.xavil.ultraviolet.mixin.accessor.MinecraftServerAccessor;
import net.xavil.ultraviolet.networking.s2c.ClientboundSpaceStationInfoPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundSyncCelestialTimePacket;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.CelestialRing;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.OrbitalPlane;
import net.xavil.hawklib.math.matrices.Vec3i;

public final class ServerUniverse extends Universe {

	protected final MinecraftServer server;
	protected StartingSystemGalaxyGenerationLayer startingGenerator;
	protected int overworldNodeId = -1;
	private int timeSyncIntervalTicks = 200;
	private int ticksUntilTimeSync = timeSyncIntervalTicks;

	protected SystemTicket startingSystemTicket = null;

	public ServerUniverse(MinecraftServer server) {
		this.server = server;
	}

	// TODO: allow changing the universe seed via configs
	// the default seed is shared between all instances of the mod, because i think
	// it would be cool to be able to go "hey check out this world i found" and give
	// people the ability to just. go there.
	@Override
	public long getCommonUniverseSeed() {
		// floof :3
		return 0xf100f;
	}

	@Override
	public long getUniqueUniverseSeed() {
		var worldData = MinecraftServerAccessor.getWorldData(this.server);
		var worldSeed = worldData.worldGenSettings().seed();
		return Mth.murmurHash3Mixer(getCommonUniverseSeed()) ^ Mth.murmurHash3Mixer(worldSeed);
	}

	@Override
	public StartingSystemGalaxyGenerationLayer getStartingSystemGenerator() {
		return this.startingGenerator;
	}

	// ew
	@Override
	public Maybe<Integer> createStation(String name, StationLocation location) {
		final var res = super.createStation(name, location);
		res.ifSome(id -> {
			final var station = this.spaceStations.get(id).unwrap();
			final var packet = new ClientboundSpaceStationInfoPacket();
			packet.id = id;
			packet.name = station.name;
			packet.orientation = station.orientation;
			packet.locationNbt = StationLocation.toNbt(station.getLocation());
			this.server.getPlayerList().broadcastAll(packet);
		});
		return res;
	}

	public static final DimensionType STATION_DIM_TYPE = DimensionType.create(OptionalLong.of(1000), true, false, false,
			false, 1, false, false, true, true, false, 0, 384, 384, BlockTags.INFINIBURN_OVERWORLD,
			DimensionType.OVERWORLD_EFFECTS, 0.0f);

	@Override
	public Level createLevelForStation(String name, int id) {
		final var manager = DynamicDimensionManager.get(this.server);
		final var universe = MinecraftServerAccessor.getUniverse(this.server);

		final var key = DynamicDimensionManager.getKey("station_" + name);
		final var newLevel = manager.getOrCreateLevel(key, () -> {
			final var generator = EmptyChunkGenerator.create(this.server.registryAccess());
			return new LevelStem(Holder.direct(STATION_DIM_TYPE), generator);
		});

		LevelAccessor.setUniverse(newLevel, universe);
		LevelAccessor.setWorldType(newLevel, new WorldType.Station(id));

		return newLevel;
	}

	@Override
	public void tick(ProfilerFiller profiler, boolean isPaused) {
		super.tick(profiler, isPaused);
		if (!isPaused) {
			this.ticksUntilTimeSync -= 1;
			if (this.ticksUntilTimeSync <= 0) {
				syncTime(false);
				this.ticksUntilTimeSync = this.timeSyncIntervalTicks;
			}
		}
	}

	public void syncTime(boolean isDiscontinuous) {
		final var syncPacket = new ClientboundSyncCelestialTimePacket(this.celestialTime);
		syncPacket.celestialTimeRate = this.celestialTimeRate;
		syncPacket.isDiscontinuous = isDiscontinuous;
		this.server.getPlayerList().broadcastAll(syncPacket);
	}

	public void syncTime(ServerPlayer player, boolean isDiscontinuous) {
		final var syncPacket = new ClientboundSyncCelestialTimePacket(this.celestialTime);
		syncPacket.celestialTimeRate = this.celestialTimeRate;
		syncPacket.isDiscontinuous = isDiscontinuous;
		player.connection.send(syncPacket);
	}

	private record StartingSystem(double systemAgeMya, String name, CelestialNode rootNode,
			CelestialNode startingNode) {
	}

	private StartingSystem startingSystem() {
		// NOTE: reference plane is the ecliptic plane
		final var sol = StellarCelestialNode.fromMass(StellarCelestialNode.Type.MAIN_SEQUENCE, Yg_PER_Msol * 1);
		sol.obliquityAngle = Math.toRadians(7.25);
		sol.rotationalRate = 2.90307e-6;

		// @formatter:off
		final var mercury = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_WORLD, Yg_PER_Mearth * (0.055), 0.3829, 427);
		mercury.obliquityAngle = Math.toRadians(0.034);
		mercury.rotationalRate = 1.24099e-6;
		final var venus = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_WORLD, Yg_PER_Mearth * (0.815), 0.9499, 737);
		venus.obliquityAngle = Math.toRadians(0.034);
		venus.rotationalRate = -4.13193e-7;
		final var earth = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.EARTH_LIKE_WORLD, Yg_PER_Mearth * (1), 1, 287.91);
		earth.obliquityAngle = Math.toRadians(23.4392811);
		earth.rotationalRate = 7.29211e-5;
		final var mars = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_WORLD, Yg_PER_Mearth * (0.107), 0.5314, 213);
		mars.obliquityAngle = Math.toRadians(25.19);
		mars.rotationalRate = 7.088216e-5;
		final var jupiter = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.GAS_GIANT, Yg_PER_Mearth * (317.8), 10.973, 165);
		jupiter.obliquityAngle = Math.toRadians(3.13);
		jupiter.rotationalRate = 1.7583764e-4;
		final var saturn = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.GAS_GIANT, Yg_PER_Mearth * (95.159), 9.1402, 97);
		saturn.obliquityAngle = Math.toRadians(26.73);
		saturn.rotationalRate = 1.65539181e-4;
		final var uranus = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.GAS_GIANT, Yg_PER_Mearth * (14.536), 3.9763, 76);
		uranus.obliquityAngle = Math.toRadians(97.77);
		uranus.rotationalRate = -1.01239075e-4;
		final var neptune = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.GAS_GIANT, Yg_PER_Mearth * (17.147), 3.8603, 72);
		neptune.obliquityAngle = Math.toRadians(28.32);
		neptune.rotationalRate = 1.08338253e-4;
		final var pluto = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ICE_WORLD, Yg_PER_Mearth * (0.00218), 0.1868, 44);
		pluto.obliquityAngle = Math.toRadians(28.32);
		pluto.rotationalRate = 1.08338253e-4;

		sol.insertChild(mercury, 0.205630,  Tm_PER_au * (0.387098), Math.toRadians(7.005),   Math.toRadians(48.331),    Math.toRadians(29.124),    1);
		sol.insertChild(venus,   0.006772,  Tm_PER_au * (0.723332), Math.toRadians(3.39458), Math.toRadians(76.680),    Math.toRadians(54.884),    2);
		sol.insertChild(earth,   0.0167086, Tm_PER_au * (1),        Math.toRadians(0.00005), Math.toRadians(-11.26064), Math.toRadians(114.20783), 3);
		sol.insertChild(mars,    0.0934,    Tm_PER_au * (1.523680), Math.toRadians(1.850),   Math.toRadians(49.57854),  Math.toRadians(286.5),     4);
		sol.insertChild(jupiter, 0.0489,    Tm_PER_au * (5.2038),   Math.toRadians(1.303),   Math.toRadians(100.464),   Math.toRadians(273.867),   5);
		sol.insertChild(saturn,  0.0565,    Tm_PER_au * (9.5826),   Math.toRadians(2.485),   Math.toRadians(113.665),   Math.toRadians(339.392),   6);
		sol.insertChild(uranus,  0.04717,   Tm_PER_au * (19.19126), Math.toRadians(0.773),   Math.toRadians(74.006),    Math.toRadians(96.998857), 7);
		sol.insertChild(neptune, 0.008678,  Tm_PER_au * (30.07),    Math.toRadians(1.770),   Math.toRadians(131.783),   Math.toRadians(273.187),   8);
		sol.insertChild(pluto,   0.2488,    Tm_PER_au * (29.658),    Math.toRadians(17.16),   Math.toRadians(110.299),   Math.toRadians(113.834),   9);

		final var moon = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_WORLD, Yg_PER_Mearth * (0.0123), 0.2727, 250);
		moon.obliquityAngle = Math.toRadians(6.687);
		moon.rotationalRate = 2.4626008e-6;

		earth.insertChild(moon, 0.0549, Tm_PER_au * (0.00257), Math.toRadians(5.145), 0, 0, 8);

		final var io = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_WORLD, Yg_PER_Mearth * (0.015), 0.286, 110);
		io.obliquityAngle = Math.toRadians(0);
		io.rotationalRate = 4.1105928e-5;
		final var europa = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_ICE_WORLD, Yg_PER_Mearth * (0.008), 0.245, 102);
		europa.obliquityAngle = Math.toRadians(0);
		europa.rotationalRate = 2.04782725e-5;
		final var gyanmede = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_ICE_WORLD, Yg_PER_Mearth * (0.413), 0.2727, 110);
		gyanmede.obliquityAngle = Math.toRadians(0);
		gyanmede.rotationalRate = 1.01644439e-5;
		final var callisto = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_ICE_WORLD, Yg_PER_Mearth * (0.018), 0.378, 134);
		callisto.obliquityAngle = Math.toRadians(0);
		callisto.rotationalRate = 4.3574793e-6;

		jupiter.insertChild(io,       0.0041, Tm_PER_au * (0.00281), Math.toRadians(2.213), 0, 0, 9);
		jupiter.insertChild(europa,   0.0090, Tm_PER_au * (0.00448), Math.toRadians(1.791), 0, 0, 10);
		jupiter.insertChild(gyanmede, 0.0013, Tm_PER_au * (0.00715), Math.toRadians(2.214), 0, 0, 11);
		jupiter.insertChild(callisto, 0.0074, Tm_PER_au * (0.01258), Math.toRadians(2.017), 0, 0, 12);

		final var mimas = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_WORLD, Yg_PER_Mearth * (6.3e-6), 0.0311, 64);
		mimas.obliquityAngle = Math.toRadians(0);
		mimas.rotationalRate = 2.0 * Math.PI / 81425.2573;
		final var enceladus = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_ICE_WORLD, Yg_PER_Mearth * (1.8e-5), 0.0395, 75);
		enceladus.obliquityAngle = Math.toRadians(0);
		enceladus.rotationalRate = 2.0 * Math.PI / 118386.835;
		final var tethys = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ICE_WORLD, Yg_PER_Mearth * (1.03e-4), 0.0416, 86);
		tethys.obliquityAngle = Math.toRadians(0);
		tethys.rotationalRate = 2.0 * Math.PI / 163106.093;
		final var dione = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_ICE_WORLD, Yg_PER_Mearth * (1.834e-4), 0.088, 87);
		dione.obliquityAngle = Math.toRadians(0);
		dione.rotationalRate = 2.0 * Math.PI / 236469.456;
		final var rhea = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ICE_WORLD, Yg_PER_Mearth * (3.9e-4), 0.1197, 76);
		rhea.obliquityAngle = Math.toRadians(0);
		rhea.rotationalRate = 2.0 * Math.PI / 390373.517;
		final var titan = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_WORLD, Yg_PER_Mearth * (0.0225), 0.404, 97);
		titan.obliquityAngle = Math.toRadians(0);
		titan.rotationalRate = 2.0 * Math.PI / 1377648;
		final var iapetus = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ICE_WORLD, Yg_PER_Mearth * (0.0225), 0.1151, 110);
		iapetus.obliquityAngle = Math.toRadians(0);
		iapetus.rotationalRate = 2.0 * Math.PI / 6853377.6;

		saturn.insertChild(mimas,     0.0196, Tm_PER_au * (0.00124), Math.toRadians(1.574),   0.0  % (2.0 * Math.PI), 0, 13);
		saturn.insertChild(enceladus, 0.0047, Tm_PER_au * (0.00159), Math.toRadians(0.009),   1.0  % (2.0 * Math.PI), 0, 14);
		saturn.insertChild(tethys,    0.0001, Tm_PER_au * (0.00196), Math.toRadians(1.12),    5.0  % (2.0 * Math.PI), 0, 15);
		saturn.insertChild(dione,     0.0022, Tm_PER_au * (0.00252), Math.toRadians(0.019),   7.0  % (2.0 * Math.PI), 0, 16);
		saturn.insertChild(rhea,      0.0012, Tm_PER_au * (0.00352), Math.toRadians(0.345),   11.0 % (2.0 * Math.PI), 0, 17);
		saturn.insertChild(titan,     0.0288, Tm_PER_au * (0.00816), Math.toRadians(0.34854), 13.0 % (2.0 * Math.PI), 0, 18);
		saturn.insertChild(iapetus,   0.0276, Tm_PER_au * (0.02380), Math.toRadians(15.47),   17.0 % (2.0 * Math.PI), 0, 19);

		saturn.rings.push(new CelestialRing(OrbitalPlane.ZERO, 0, new Interval(Tm_PER_au * (0.00044719888), Tm_PER_au * (0.000937045423)), Yg_PER_Mearth * (0.40 * 6.3e-6)));
		// @formatter:on

		return new StartingSystem(4600, "Sol", sol, earth);
	}

	public void prepare() {
		final var rng = Rng.wrap(new Random(getUniqueUniverseSeed() + 4));

		var startingSystem = startingSystem();
		startingSystem.rootNode.assignIds();

		var rootNode = startingSystem.rootNode;
		var startingNodeId = startingSystem.rootNode.find(startingSystem.startingNode);

		try (final var disposer = Disposable.scope()) {
			final var sectorPos = Vec3i.ZERO;
			final var tempTicket = this.sectorManager.createSectorTicket(disposer,
					UniverseSectorTicketInfo.single(sectorPos));
			this.sectorManager.forceLoad(tempTicket);

			final var galaxySector = this.sectorManager.getSector(sectorPos).unwrap();
			final var elementIndex = rng.uniformInt(0, galaxySector.initialElements.size());

			final var tempTicket2 = this.sectorManager.createGalaxyTicket(disposer,
					new UniverseSectorId(sectorPos, elementIndex));
			final var galaxy = this.sectorManager.forceLoad(tempTicket2).unwrap();

			this.startingGenerator = new StartingSystemGalaxyGenerationLayer(galaxy, rootNode, startingNodeId);
			galaxy.addGenerationLayer(this.startingGenerator);
			final var startingId = this.startingGenerator.getStartingSystemId();

			Mod.LOGGER.info("placing starting system at {}", startingId);

			this.startingSystemTicket = galaxy.sectorManager.createSystemTicket(this.disposer,
					startingId.system().galaxySector());
			galaxy.sectorManager.forceLoad(this.startingSystemTicket);
		}
	}

	public Maybe<StarSystem> loadSystem(Disposable.Multi disposer, SystemId id) {
		return this.loadGalaxy(disposer, id.universeSector())
				.flatMap(galaxy -> galaxy.loadSystem(disposer, id.galaxySector()));
	}

}
