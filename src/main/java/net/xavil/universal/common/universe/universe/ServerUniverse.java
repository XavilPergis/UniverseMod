package net.xavil.universal.common.universe.universe;

import java.util.Random;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.galaxy.StartingSystemGalaxyGenerationLayer;
import net.xavil.universal.common.universe.id.SectorId;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.mixin.accessor.MinecraftServerAccessor;
import net.xavil.universal.networking.s2c.ClientboundSyncCelestialTimePacket;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.CelestialRing;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.Rng;
import net.xavil.util.Units;
import net.xavil.util.math.Interval;
import net.xavil.util.math.OrbitalPlane;
import net.xavil.util.math.Vec3i;

public final class ServerUniverse extends Universe {

	protected final MinecraftServer server;
	protected StartingSystemGalaxyGenerationLayer startingGenerator;
	protected int overworldNodeId = -1;

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

	@Override
	public void tick() {
		super.tick();
		if (this.celestialTimeTicks % 200 == 0) {
			var syncPacket = new ClientboundSyncCelestialTimePacket(this.celestialTimeTicks);
			this.server.getPlayerList().broadcastAll(syncPacket);
		}
	}

	private record StartingSystem(double systemAgeMya, String name, CelestialNode rootNode,
			CelestialNode startingNode) {
	}

	private StartingSystem startingSystem(Rng rng) {
		// NOTE: reference plane is the ecliptic plane
		final var sol = StellarCelestialNode.fromMass(rng, StellarCelestialNode.Type.MAIN_SEQUENCE, Units.fromMsol(1));
		sol.obliquityAngle = Math.toRadians(7.25);
		sol.rotationalPeriod = 2.90307e-6;

		// final var s1 = StarNode.fromMass(random, StarNode.Type.MAIN_SEQUENCE, Units.msol(0.5));
		// final var s2 = StarNode.fromMass(random, StarNode.Type.MAIN_SEQUENCE, Units.msol(1));
		// final var s3 = StarNode.fromMass(random, StarNode.Type.MAIN_SEQUENCE, Units.msol(5));

		// final var b1 = new BinaryNode(s1, s2, OrbitalPlane.ZERO, 0.5, 1, 0);
		// final var b2 = new BinaryNode(b1, s3, OrbitalPlane.ZERO, 0.2, 10, 0);

		// final var testStar = StarNode.fromMass(random, StarNode.Type.MAIN_SEQUENCE, Units.msol(0.4));
		// sol.obliquityAngle = Math.toRadians(7.25);
		// sol.rotationalSpeed = 2.90307e-6;

		// @formatter:off
		// final var a = new PlanetNode(PlanetNode.Type.ROCKY_WORLD, Units.mearth(1), 0.3829, 427);
		// a.obliquityAngle = Math.toRadians(0);
		// a.rotationalSpeed = 1.24099e-8;
		// final var b = new PlanetNode(PlanetNode.Type.ROCKY_ICE_WORLD, Units.mearth(0.01), 0.3829, 427);
		// b.obliquityAngle = Math.toRadians(0);
		// b.rotationalSpeed = 1.24099e-8;
		final var mercury = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_WORLD, Units.fromMearth(0.055), 0.3829, 427);
		mercury.obliquityAngle = Math.toRadians(0.034);
		mercury.rotationalPeriod = 2 * Math.PI / 1.24099e-6;
		final var venus = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_WORLD, Units.fromMearth(0.815), 0.9499, 737);
		venus.obliquityAngle = Math.toRadians(0.034);
		venus.rotationalPeriod = 2 * Math.PI / -4.13193e-7;
		final var earth = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.EARTH_LIKE_WORLD, Units.fromMearth(1), 1, 287.91);
		earth.obliquityAngle = Math.toRadians(23.4392811);
		earth.rotationalPeriod = 2 * Math.PI / 7.29211e-5;
		final var mars = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_WORLD, Units.fromMearth(0.107), 0.5314, 213);
		mars.obliquityAngle = Math.toRadians(25.19);
		mars.rotationalPeriod = 2 * Math.PI / 7.088216e-5;
		final var jupiter = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.GAS_GIANT, Units.fromMearth(317.8), 10.973, 165);
		jupiter.obliquityAngle = Math.toRadians(3.13);
		jupiter.rotationalPeriod = 2 * Math.PI / 1.7583764e-4;
		final var saturn = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.GAS_GIANT, Units.fromMearth(95.159), 9.1402, 97);
		saturn.obliquityAngle = Math.toRadians(26.73);
		saturn.rotationalPeriod = 2 * Math.PI / 1.65539181e-4;
		final var uranus = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.GAS_GIANT, Units.fromMearth(14.536), 3.9763, 76);
		uranus.obliquityAngle = Math.toRadians(97.77);
		uranus.rotationalPeriod = 2 * Math.PI / -1.01239075e-4;
		final var neptune = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.GAS_GIANT, Units.fromMearth(17.147), 3.8603, 72);
		neptune.obliquityAngle = Math.toRadians(28.32);
		neptune.rotationalPeriod = 2 * Math.PI / 1.08338253e-4;

		sol.insertChild(mercury, 0.205630,  Units.fromAu(0.387098), Math.toRadians(7.005),   Math.toRadians(48.331),    Math.toRadians(29.124),    0);
		// sol.insertChild(mercury, 0.9,       Units.au(3.1),      Math.toRadians(86),      Math.toRadians(48.331),    Math.toRadians(29.124),    0);
		sol.insertChild(venus,   0.006772,  Units.fromAu(0.723332), Math.toRadians(3.39458), Math.toRadians(76.680),    Math.toRadians(54.884),    0);
		sol.insertChild(earth,   0.0167086, Units.fromAu(1),        Math.toRadians(0.00005), Math.toRadians(-11.26064), Math.toRadians(114.20783), 0);
		sol.insertChild(mars,    0.0934,    Units.fromAu(1.523680), Math.toRadians(1.850),   Math.toRadians(49.57854),  Math.toRadians(286.5),     0);
		sol.insertChild(jupiter, 0.0489,    Units.fromAu(5.2038),   Math.toRadians(1.303),   Math.toRadians(100.464),   Math.toRadians(273.867),   0);
		sol.insertChild(saturn,  0.0565,    Units.fromAu(9.5826),   Math.toRadians(2.485),   Math.toRadians(113.665),   Math.toRadians(339.392),   0);
		sol.insertChild(uranus,  0.04717,   Units.fromAu(19.19126), Math.toRadians(0.773),   Math.toRadians(74.006),    Math.toRadians(96.998857), 0);
		sol.insertChild(neptune, 0.008678,  Units.fromAu(30.07),    Math.toRadians(1.770),   Math.toRadians(131.783),   Math.toRadians(273.187),   0);
		// s1.insertChild(a, 0.5,   Units.au(1), Math.toRadians(0),   Math.toRadians(0), Math.toRadians(0), 0);
		// a.insertChild(b, 0,   Units.au(0.01), Math.toRadians(45),   Math.toRadians(0), Math.toRadians(0), 0);

		final var moon = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_WORLD, Units.fromMearth(0.0123), 0.2727, 250);
		moon.obliquityAngle = Math.toRadians(6.687);
		moon.rotationalPeriod = 2 * Math.PI / 2.4626008e-6;
		earth.insertChild(moon, 0.0549, Units.fromAu(0.00257), Math.toRadians(5.145), 0, 0, 8);

		final var io = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_WORLD, Units.fromMearth(0.015), 0.286, 110);
		io.obliquityAngle = Math.toRadians(0);
		io.rotationalPeriod = 2 * Math.PI / 4.1105928e-5;
		final var europa = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_ICE_WORLD, Units.fromMearth(0.008), 0.245, 102);
		europa.obliquityAngle = Math.toRadians(0);
		europa.rotationalPeriod = 2 * Math.PI / 2.04782725e-5;
		final var gyanmede = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_ICE_WORLD, Units.fromMearth(0.413), 0.2727, 110);
		gyanmede.obliquityAngle = Math.toRadians(0);
		gyanmede.rotationalPeriod = 2 * Math.PI / 1.01644439e-5;
		final var callisto = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_ICE_WORLD, Units.fromMearth(0.018), 0.378, 134);
		callisto.obliquityAngle = Math.toRadians(0);
		callisto.rotationalPeriod = 2 * Math.PI / 4.3574793e-6;

		jupiter.insertChild(io,       0.0041, Units.fromAu(0.00281), Math.toRadians(2.213), 0, 0, 9);
		jupiter.insertChild(europa,   0.0090, Units.fromAu(0.00448), Math.toRadians(1.791), 0, 0, 10);
		jupiter.insertChild(gyanmede, 0.0013, Units.fromAu(0.00715), Math.toRadians(2.214), 0, 0, 11);
		jupiter.insertChild(callisto, 0.0074, Units.fromAu(0.01258), Math.toRadians(2.017), 0, 0, 12);

		final var mimas = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_WORLD, Units.fromMearth(6.3e-6), 0.0311, 64);
		mimas.obliquityAngle = Math.toRadians(0);
		mimas.rotationalPeriod = 81425.2573;
		final var enceladus = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_ICE_WORLD, Units.fromMearth(1.8e-5), 0.0395, 75);
		enceladus.obliquityAngle = Math.toRadians(0);
		enceladus.rotationalPeriod = 118386.835;
		final var tethys = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ICE_WORLD, Units.fromMearth(1.03e-4), 0.0416, 86);
		tethys.obliquityAngle = Math.toRadians(0);
		tethys.rotationalPeriod = 163106.093;
		final var dione = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_ICE_WORLD, Units.fromMearth(1.834e-4), 0.088, 87);
		dione.obliquityAngle = Math.toRadians(0);
		dione.rotationalPeriod = 236469.456;
		final var rhea = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ICE_WORLD, Units.fromMearth(3.9e-4), 0.1197, 76);
		rhea.obliquityAngle = Math.toRadians(0);
		rhea.rotationalPeriod = 390373.517;
		final var titan = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_WORLD, Units.fromMearth(0.0225), 0.404, 97);
		titan.obliquityAngle = Math.toRadians(0);
		titan.rotationalPeriod = 1377648;
		final var iapetus = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ICE_WORLD, Units.fromMearth(0.0225), 0.1151, 110);
		iapetus.obliquityAngle = Math.toRadians(0);
		iapetus.rotationalPeriod = 6853377.6;

		// saturn axial tilt 0.46652651

		saturn.insertChild(mimas,     0.0196, Units.fromAu(0.00124), Math.toRadians(1.574),   0.0  % (2.0 * Math.PI), 0, 13);
		saturn.insertChild(enceladus, 0.0047, Units.fromAu(0.00159), Math.toRadians(0.009),   1.0  % (2.0 * Math.PI), 0, 14);
		saturn.insertChild(tethys,    0.0001, Units.fromAu(0.00196), Math.toRadians(1.12),    5.0  % (2.0 * Math.PI), 0, 15);
		saturn.insertChild(dione,     0.0022, Units.fromAu(0.00252), Math.toRadians(0.019),   7.0  % (2.0 * Math.PI), 0, 16);
		saturn.insertChild(rhea,      0.0012, Units.fromAu(0.00352), Math.toRadians(0.345),   11.0 % (2.0 * Math.PI), 0, 17);
		saturn.insertChild(titan,     0.0288, Units.fromAu(0.00816), Math.toRadians(0.34854), 13.0 % (2.0 * Math.PI), 0, 18);
		saturn.insertChild(iapetus,   0.0276, Units.fromAu(0.02380), Math.toRadians(15.47),   17.0 % (2.0 * Math.PI), 0, 19);

		saturn.addRing(new CelestialRing(OrbitalPlane.ZERO, 0, new Interval(Units.fromAu(0.00044719888), Units.fromAu(0.000937045423)), Units.fromMearth(0.40 * 6.3e-6)));
		// @formatter:on

		// var root = new BinaryNode(sol, testStar, OrbitalPlane.ZERO, 1, Units.au(100), 0);
		// var root = b2;

		return new StartingSystem(4600, "Sol", sol, earth);
	}

	// private Vec3i selectStartingVolume(Random random) {
	// 	var sx = (int) (250 * random.nextGaussian());
	// 	var sy = (int) (250 * random.nextGaussian());
	// 	var sz = (int) (250 * random.nextGaussian());
	// 	var systemVolumePos = Vec3i.ZERO;
	// }

	public void prepare() {
		var rng = Rng.wrap(new Random(getUniqueUniverseSeed() + 4));

		var startingSystem = startingSystem(rng);
		startingSystem.rootNode.assignIds();

		// var info = StarSystem.Info.custom(startingSystem.systemAgeMya, startingSystem.name, startingSystem);
		// info.systemAgeMya = startingSystem.systemAgeMya;
		// info.remainingHydrogenYg = 0;
		// info.name = startingSystem.name;	
		// startingSystem.rootNode.visit(node -> {
		// 	if (node instanceof StellarCelestialNode starNode)
		// 		info.addStar(starNode);
		// });

		var rootNode = startingSystem.rootNode;
		var startingNodeId = startingSystem.rootNode.find(startingSystem.startingNode);

		var gx = (int) (1000 * rng.uniformDouble(-1, 1));
		var gy = (int) (1000 * rng.uniformDouble(-1, 1));
		var gz = (int) (1000 * rng.uniformDouble(-1, 1));
		var galaxySectorPos = Vec3i.from(gx, gy, gz);
		var galaxyVolume = getVolumeAt(galaxySectorPos, true);

		var layerIds = galaxyVolume.elements.keySet().toIntArray();
		var initialLayerId = layerIds[rng.uniformInt(0, layerIds.length)];

		var initialLayer = galaxyVolume.elements.get(initialLayerId);
		var initialGalaxySectorId = rng.uniformInt(0, initialLayer.size());
		var initialGalaxyId = new SectorId(galaxySectorPos, new Octree.Id(initialLayerId, initialGalaxySectorId));

		// var initialG

		// var sx = (int) (250 * random.nextGaussian());
		// var sy = (int) (250 * random.nextGaussian());
		// var sz = (int) (250 * random.nextGaussian());
		var systemVolumePos = Vec3i.ZERO;

		this.startingGenerator = new StartingSystemGalaxyGenerationLayer(initialGalaxyId, systemVolumePos, rootNode,
				startingNodeId);
	}

}
