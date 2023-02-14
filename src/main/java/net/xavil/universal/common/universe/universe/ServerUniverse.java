package net.xavil.universal.common.universe.universe;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.galaxy.StartingSystemGalaxyGenerationLayer;
import net.xavil.universal.common.universe.id.SectorId;
import net.xavil.universal.common.universe.system.PlanetNode;
import net.xavil.universal.common.universe.system.StarNode;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.system.StarSystemNode;
import net.xavil.universal.mixin.accessor.MinecraftServerAccessor;

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

	private record StartingSystem(double systemAgeMya, String name, StarSystemNode rootNode,
			StarSystemNode startingNode) {
	}

	private StartingSystem startingSystem(Random random) {
		// NOTE: reference plane is the ecliptic plane
		final var sol = StarNode.fromMass(random, StarNode.Type.MAIN_SEQUENCE, Units.msol(1));
		sol.obliquityAngle = Math.toRadians(7.25);
		sol.rotationalSpeed = 2.90307e-6;

		// @formatter:off
		final var mercury = new PlanetNode(PlanetNode.Type.ROCKY_WORLD, Units.mearth(0.055));
		mercury.obliquityAngle = Math.toRadians(0.034);
		mercury.rotationalSpeed = 1.24099e-6;
		final var venus = new PlanetNode(PlanetNode.Type.ROCKY_WORLD, Units.mearth(0.815));
		venus.obliquityAngle = Math.toRadians(0.034);
		venus.rotationalSpeed = -4.13193e-7;
		final var earth = new PlanetNode(PlanetNode.Type.EARTH_LIKE_WORLD, Units.mearth(1));
		earth.obliquityAngle = Math.toRadians(23.4392811);
		earth.rotationalSpeed = 7.29211e-5;
		final var mars = new PlanetNode(PlanetNode.Type.ROCKY_WORLD, Units.mearth(0.107));
		mars.obliquityAngle = Math.toRadians(25.19);
		mars.rotationalSpeed = 7.088216e-5;
		final var jupiter = new PlanetNode(PlanetNode.Type.GAS_GIANT, Units.mearth(317.8));
		jupiter.obliquityAngle = Math.toRadians(3.13);
		jupiter.rotationalSpeed = 1.7583764e-4;
		final var saturn = new PlanetNode(PlanetNode.Type.GAS_GIANT, Units.mearth(95.159));
		saturn.obliquityAngle = Math.toRadians(26.73);
		saturn.rotationalSpeed = 1.65539181e-4;
		final var uranus = new PlanetNode(PlanetNode.Type.GAS_GIANT, Units.mearth(14.536));
		uranus.obliquityAngle = Math.toRadians(97.77);
		uranus.rotationalSpeed = -1.01239075e-4;
		final var neptune = new PlanetNode(PlanetNode.Type.GAS_GIANT, Units.mearth(17.147));
		neptune.obliquityAngle = Math.toRadians(28.32);
		neptune.rotationalSpeed = 1.08338253e-4;

		// sol.insertChild(mercury, 0.205630,  Units.au(0.387098), Math.toRadians(7.005),   Math.toRadians(48.331),    Math.toRadians(29.124));
		// sol.insertChild(venus,   0.006772,  Units.au(0.723332), Math.toRadians(3.39458), Math.toRadians(76.680),    Math.toRadians(54.884));
		// sol.insertChild(earth,   0.0167086, Units.au(1),        Math.toRadians(0.00005), Math.toRadians(-11.26064), Math.toRadians(114.20783));
		// sol.insertChild(mars,    0.0934,    Units.au(1.523680), Math.toRadians(1.850),   Math.toRadians(49.57854),  Math.toRadians(286.5));
		// sol.insertChild(jupiter, 0.0489,    Units.au(5.2038),   Math.toRadians(1.303),   Math.toRadians(100.464),   Math.toRadians(273.867));
		// sol.insertChild(saturn,  0.0565,    Units.au(9.5826),   Math.toRadians(2.485),   Math.toRadians(113.665),   Math.toRadians(339.392));
		// sol.insertChild(uranus,  0.04717,   Units.au(19.19126), Math.toRadians(0.773),   Math.toRadians(74.006),    Math.toRadians(96.998857));
		// sol.insertChild(neptune, 0.008678,  Units.au(30.07),    Math.toRadians(1.770),   Math.toRadians(131.783),   Math.toRadians(273.187));

		sol.insertChild(mercury, 0, Units.au(0.387098), Math.toRadians(0), Math.toRadians(0), Math.toRadians(0));
		sol.insertChild(venus,   0, Units.au(0.723332), Math.toRadians(0), Math.toRadians(0), Math.toRadians(0));
		sol.insertChild(earth,   0, Units.au(1),        Math.toRadians(0), Math.toRadians(0), Math.toRadians(0));
		sol.insertChild(mars,    0, Units.au(1.523680), Math.toRadians(0), Math.toRadians(0), Math.toRadians(0));
		sol.insertChild(jupiter, 0, Units.au(5.2038),   Math.toRadians(0), Math.toRadians(0), Math.toRadians(0));
		sol.insertChild(saturn,  0, Units.au(9.5826),   Math.toRadians(0), Math.toRadians(0), Math.toRadians(0));
		sol.insertChild(uranus,  0, Units.au(19.19126), Math.toRadians(0), Math.toRadians(0), Math.toRadians(0));
		sol.insertChild(neptune, 0, Units.au(30.07),    Math.toRadians(0), Math.toRadians(0), Math.toRadians(0));

		final var moon = new PlanetNode(PlanetNode.Type.ROCKY_WORLD, Units.mearth(0.0123));
		moon.obliquityAngle = Math.toRadians(6.687);
		moon.rotationalSpeed = 2.4626008e-6;
		earth.insertChild(moon, 0.0549, Units.au(0.00257), Math.toRadians(5.145), 0, 0);
		// @formatter:on

		return new StartingSystem(4600, "Sol", sol, earth);
	}

	public void prepare() {
		var random = new Random(getUniqueUniverseSeed() + 4);

		var startingSystem = startingSystem(random);
		startingSystem.rootNode.assignIds();

		var info = new StarSystem.Info();
		info.systemAgeMya = startingSystem.systemAgeMya;
		info.remainingHydrogenYg = 0;
		info.name = startingSystem.name;
		
		info.stars = new ArrayList<>();
		startingSystem.rootNode.visit(node -> {
			if (node instanceof StarNode starNode)
				info.stars.add(starNode);
		});

		var rootNode = startingSystem.rootNode;
		var startingNodeId = startingSystem.rootNode.find(startingSystem.startingNode);

		var gx = (int) (1000 * random.nextGaussian());
		var gy = (int) (1000 * random.nextGaussian());
		var gz = (int) (1000 * random.nextGaussian());
		var galaxySectorPos = new Vec3i(gx, gy, gz);
		var galaxyVolume = getVolumeAt(galaxySectorPos, true);

		var layerIds = galaxyVolume.elements.keySet().toIntArray();
		var initialLayerId = layerIds[random.nextInt(0, layerIds.length)];

		var initialLayer = galaxyVolume.elements.get(initialLayerId);
		var initialGalaxySectorId = random.nextInt(0, initialLayer.size());
		var initialGalaxyId = new SectorId(galaxySectorPos, new Octree.Id(initialLayerId, initialGalaxySectorId));

		var sx = (int) (250 * random.nextGaussian());
		var sy = (int) (250 * random.nextGaussian());
		var sz = (int) (250 * random.nextGaussian());
		var systemVolumePos = new Vec3i(sx, sy, sz);

		this.startingGenerator = new StartingSystemGalaxyGenerationLayer(initialGalaxyId, systemVolumePos, rootNode,
				startingNodeId);
	}

}
