package net.xavil.universal.common.universe.universe;

import java.util.List;
import java.util.Random;

import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.UniverseId;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.system.BinaryNode;
import net.xavil.universal.common.universe.system.OrbitalPlane;
import net.xavil.universal.common.universe.system.OrbitalShape;
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

	// public StarSystemNode getSystemNode(UniverseId id) {
	// 	var galaxyVolume = getOrGenerateGalaxyVolume(id.galaxySector().sectorPos());
	// 	var galaxy = galaxyVolume.getById(id.galaxySector().sectorId()).getFull();
	// }

	public StarSystem generateStartingSystem(Random random, Galaxy galaxy) {
		var starA = StarNode.fromMass(random, StarNode.Type.MAIN_SEQUENCE, Units.msol(1.4));
		var starB = StarNode.fromMass(random, StarNode.Type.MAIN_SEQUENCE, Units.msol(0.4));
		var nodeAB = new BinaryNode(starA, starB, new OrbitalPlane(2 * Math.PI * 0.09, 0, 0), 1, Units.au(0.9));

		var overworld = new PlanetNode(PlanetNode.Type.EARTH_LIKE_WORLD, Units.mearth(1));
		var moon = new PlanetNode(PlanetNode.Type.ROCKY_WORLD, Units.mearth(0.01));
		nodeAB.insertChild(
				new StarSystemNode.UnaryOrbit(overworld, true, new OrbitalShape(0, Units.au(1.7)), OrbitalPlane.ZERO));
		overworld.insertChild(
				new StarSystemNode.UnaryOrbit(moon, true, new OrbitalShape(0, Units.au(0.00257)), OrbitalPlane.ZERO));

		return new StarSystem(galaxy, nodeAB);
	}

	@Override
	public StartingSystemGalaxyGenerationLayer getStartingSystemGenerator() {
		return this.startingGenerator;
	}

	public void prepare() {
		var random = new Random(getUniqueUniverseSeed() + 4);

		var starA = StarNode.fromMass(random, StarNode.Type.MAIN_SEQUENCE, Units.msol(1.4));
		var starB = StarNode.fromMass(random, StarNode.Type.MAIN_SEQUENCE, Units.msol(0.4));
		var nodeAB = new BinaryNode(starA, starB, new OrbitalPlane(2 * Math.PI * 0.09, 0, 0), 1, Units.au(0.9));

		var overworld = new PlanetNode(PlanetNode.Type.EARTH_LIKE_WORLD, Units.mearth(1));
		var moon1 = new PlanetNode(PlanetNode.Type.ROCKY_WORLD, Units.mearth(0.01));
		var moon2 = new PlanetNode(PlanetNode.Type.ROCKY_WORLD, Units.mearth(0.04));
		nodeAB.insertChild(
				new StarSystemNode.UnaryOrbit(overworld, true, new OrbitalShape(0, Units.au(1.7)), OrbitalPlane.ZERO));
		overworld.insertChild(
				new StarSystemNode.UnaryOrbit(moon1, true, new OrbitalShape(0, Units.au(0.00257)), OrbitalPlane.ZERO));
		overworld.insertChild(
				new StarSystemNode.UnaryOrbit(moon2, true, new OrbitalShape(0, Units.au(0.01)), new OrbitalPlane(0.1, 0, 0)));

		var otherWorld = new PlanetNode(PlanetNode.Type.WATER_WORLD, Units.mearth(1.5));
		nodeAB.insertChild(
			new StarSystemNode.UnaryOrbit(otherWorld, true, new OrbitalShape(0, Units.au(4.2)), OrbitalPlane.ZERO));

		nodeAB.assignIds();

		var info = new StarSystem.Info();
		info.systemAgeMya = 4600;
		info.remainingHydrogenYg = 0;
		info.name = "Sol"; // i dont actually wanna make Sol, i wanna start in a fantasy system
		info.stars = List.of(starA, starB);

		var rootNode = nodeAB;
		var startingNodeId = nodeAB.find(overworld);

		var gx = (int) (1000 * random.nextGaussian());
		var gy = (int) (1000 * random.nextGaussian());
		var gz = (int) (1000 * random.nextGaussian());
		var galaxySectorPos = new Vec3i(gx, gy, gz);
		var galaxyVolume = getVolumeAt(galaxySectorPos, true);
		var galaxyIds = galaxyVolume.streamIds().toArray();
		var initialGalaxyId = galaxyIds[random.nextInt(galaxyIds.length)];
		var startingGalaxyId = new UniverseId.SectorId(galaxySectorPos, initialGalaxyId);

		var sx = (int) (250 * random.nextGaussian());
		var sy = (int) (250 * random.nextGaussian());
		var sz = (int) (250 * random.nextGaussian());
		var systemVolumePos = new Vec3i(sx, sy, sz);

		this.startingGenerator = new StartingSystemGalaxyGenerationLayer(startingGalaxyId, systemVolumePos, rootNode,
				startingNodeId);

		// HACK: force discovery of the starting system id, otherwise it will remain -1.
		// i really dislike this architecture but im not smart enough to know what else to do :(
		galaxyVolume.getById(initialGalaxyId).getFull().getVolumeAt(systemVolumePos);
	}

	// public UniverseId prepareStartingVolume() {
	// var random = new Random(getUniqueUniverseSeed() + 4);

	// var startingGalaxySector = getStartingGalaxySectorPos();
	// var volume = getOrGenerateGalaxyVolume(startingGalaxySector);
	// var ids = volume.streamIds().toArray();
	// var initialGalaxyId = ids[random.nextInt(ids.length)];

	// var galaxyVolume = getOrGenerateGalaxyVolume(startingGalaxySector);
	// var galaxy = galaxyVolume.getById(initialGalaxyId).getFull();

	// // --- system setup ---

	// // --- end system setup ---

	// var startingSystemSector = getStartingSystemSectorPos();
	// var systemVolume = galaxy.getOrGenerateVolume(startingSystemSector);
	// var startingId = systemVolume.insert(randomVec(random), new
	// Lazy<StarSystem.Info, StarSystem>(info, n -> system));

	// return new UniverseId(
	// new SectorId(startingGalaxySector, initialGalaxyId),
	// new SectorId(startingSystemSector, startingId),
	// overworldId);
	// }

}
