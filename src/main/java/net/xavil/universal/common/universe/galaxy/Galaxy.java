package net.xavil.universal.common.universe.galaxy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.DensityField3;
import net.xavil.universal.common.universe.LodVolume;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.system.BinaryNode;
import net.xavil.universal.common.universe.system.OrbitalPlane;
import net.xavil.universal.common.universe.system.StarNode;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.system.StarSystemNode;
import net.xavil.universal.common.universe.universe.Universe;

public class Galaxy {

	public static class Info {
		public GalaxyType type;
		public double ageMya;
	}

	public final Universe parentUniverse;
	public final Info info;
	private final DensityField3 densityField;

	// private final Set<StarSystem> activeStarSystems = new HashSet<>();
	private final Map<Vec3i, LodVolume<StarSystem.Info, StarSystem>> loadedVolumes = new HashMap<>();

	public Galaxy(Universe parentUniverse, Info info, DensityField3 densityField) {
		this.parentUniverse = parentUniverse;
		this.info = info;
		this.densityField = densityField;
	}

	public static final double TM_PER_SECTOR = Units.TM_PER_LY * 10;
	public static final double TM_PER_SECTOR_3 = TM_PER_SECTOR * TM_PER_SECTOR * TM_PER_SECTOR;

	public static final int MAXIMUM_STARS_PER_SECTOR = 30000;
	public static final int MAXIMUM_STAR_PLACEMENT_ATTEMPTS = 16;
	public static final int DENSITY_SAMPLE_COUNT = 2500;

	public static final double SOL_LIFETIME_MYA = 10e4;

	private static Vec3 randomVec(Random random) {
		var x = random.nextDouble(0, TM_PER_SECTOR);
		var y = random.nextDouble(0, TM_PER_SECTOR);
		var z = random.nextDouble(0, TM_PER_SECTOR);
		return new Vec3(x, y, z);
	}

	private long volumeSeed(Vec3i volumeCoords) {
		var seed = Mth.murmurHash3Mixer(this.parentUniverse.getCommonUniverseSeed());
		seed ^= Mth.murmurHash3Mixer(seed ^ (long) volumeCoords.getX());
		seed ^= Mth.murmurHash3Mixer(seed ^ (long) volumeCoords.getY());
		seed ^= Mth.murmurHash3Mixer(seed ^ (long) volumeCoords.getZ());
		return seed;
	}

	private long systemSeed(Vec3i volumeCoords, int id) {
		var seed = volumeSeed(volumeCoords);
		seed ^= Mth.murmurHash3Mixer((long) id);
		return seed;
	}

	public LodVolume<StarSystem.Info, StarSystem> getOrGenerateVolume(Vec3i volumeCoords) {
		if (this.loadedVolumes.containsKey(volumeCoords)) {
			return this.loadedVolumes.get(volumeCoords);
		}

		var random = new Random(volumeSeed(volumeCoords));
		var volume = new LodVolume<StarSystem.Info, StarSystem>(volumeCoords, TM_PER_SECTOR,
				(info, offset, id) -> generateStarSystem(volumeCoords, offset, info, systemSeed(volumeCoords, id)));

		final var sectorBase = volume.getBasePos();
		var sectorDensitySum = 0.0;
		for (var i = 0; i < DENSITY_SAMPLE_COUNT; ++i) {
			var volumeOffsetTm = randomVec(random);
			sectorDensitySum += this.densityField.sampleDensity(sectorBase.add(volumeOffsetTm));
		}
		final var averageSectorDensity = Math.max(0, sectorDensitySum / DENSITY_SAMPLE_COUNT);

		var starAttemptCount = (int) (averageSectorDensity * TM_PER_SECTOR_3);

		if (starAttemptCount > MAXIMUM_STARS_PER_SECTOR) {
			Mod.LOGGER.warn("high star attempt count: {}", starAttemptCount);
			starAttemptCount = MAXIMUM_STARS_PER_SECTOR;
		}

		int successfulAttempts = 0;
		var maxDensity = starAttemptCount / TM_PER_SECTOR_3;
		for (var i = 0; i < starAttemptCount; ++i) {
			var infoSeed = random.nextLong();

			for (var j = 0; j < MAXIMUM_STAR_PLACEMENT_ATTEMPTS; ++j) {
				var volumeOffsetTm = randomVec(random);
				var density = this.densityField.sampleDensity(sectorBase.add(volumeOffsetTm));

				if (density >= random.nextDouble(0, maxDensity)) {
					volume.addInitial(volumeOffsetTm, generateStarSystemInfo(volumeCoords, volumeOffsetTm, infoSeed));
					successfulAttempts += 1;
					break;
				}
			}
		}

		Mod.LOGGER.info("[galaxygen] average stellar density: {}", averageSectorDensity);
		Mod.LOGGER.info("[galaxygen] star placement attempt count: {}", starAttemptCount);
		Mod.LOGGER.info("[galaxygen] successful star placements: {}", successfulAttempts);

		this.loadedVolumes.put(volumeCoords, volume);
		return volume;
	}

	public final double MINIMUM_STAR_MASS_YG = Units.YG_PER_MSOL * 0.1;
	public final double MAXIMUM_STAR_MASS_YG = Units.YG_PER_MSOL * 30.0;

	private double generateStarMass(Random random, double upperBoundYg) {
		upperBoundYg = Math.min(MAXIMUM_STAR_MASS_YG, upperBoundYg);
		var massFactor = Math.pow(random.nextDouble(), 15);
		var massYg = Mth.lerp(massFactor, MINIMUM_STAR_MASS_YG, upperBoundYg);
		return massYg;
	}

	// Basic system info
	private StarSystem.Info generateStarSystemInfo(Vec3i volumeCoords, Vec3 volumeOffsetTm, long seed) {
		var random = new Random(seed);
		var info = new StarSystem.Info();

		// TODO: systems tend to form in clusters, so a better idea might be to have a
		// noise field and use that as the system age directly, so we get a nice mix of
		// very young systems and old systems.
		var systemAgeFactor = Math.pow(random.nextDouble(), 3);
		info.systemAgeMya = Mth.lerp(systemAgeFactor, 1, this.info.ageMya);
		// noise field for driving this too; once again because stars form in clusters
		var remainingHydrogenYg = random.nextDouble(Units.YG_PER_MSOL * 0.1, Units.YG_PER_MSOL * 100);

		// there's always at least one star per system
		var initialStarMass = generateStarMass(random, remainingHydrogenYg);
		info.stars.add(generateStarNode(random, info.systemAgeMya, initialStarMass));

		// NOTE: generating the stars upfront in this simple way does not seem to be too
		// costly to do, even directly on the render thread. It still might make sense
		// to do everything a background thread, still.
		for (var attempts = 0; attempts < 256; ++attempts) {
			if (random.nextDouble() < 0.3)
				break;

			var mass = generateStarMass(random, remainingHydrogenYg);
			if (remainingHydrogenYg >= mass && remainingHydrogenYg >= MINIMUM_STAR_MASS_YG) {
				remainingHydrogenYg -= mass;
				info.stars.add(generateStarNode(random, info.systemAgeMya, mass));
			}
		}

		info.remainingHydrogenYg = remainingHydrogenYg;

		return info;
	}

	// Full system info
	public StarSystem generateStarSystem(Vec3i volumeCoords, Vec3 volumeOffsetTm, StarSystem.Info info, long seed) {
		var random = new Random(seed);

		var systemPlane = new OrbitalPlane(
				random.nextDouble(-Math.PI * 2, Math.PI * 2),
				random.nextDouble(-Math.PI * 2, Math.PI * 2),
				random.nextDouble(-Math.PI * 2, Math.PI * 2));

		var rootNode = pairStars(random, systemPlane, info.stars);

		rootNode.assignIds();

		return new StarSystem(this, "Test", rootNode);
	}

	public static final double MAXIMUM_SYSTEM_RADIUS_TM = Units.au(5000);

	private static void replaceNode(StarSystemNode existing, BinaryNode newNode) {
		// set up backlinks
		var parent = existing.getBinaryParent();
		if (parent != null) {
			// if the existing node has a parent, we need to notify the parent that one of
			// its children has changed, and then tell the new node that it has a new
			// parent!
			newNode.setBinaryParent(parent);
			parent.replace(existing, newNode);
		}
		newNode.getA().setBinaryParent(newNode);
		newNode.getB().setBinaryParent(newNode);
	}

	private static @Nullable StarSystemNode mergeSingleStar(Random random, OrbitalPlane systemPlane,
			StarNode existing, StarNode toInsert) {

		// if the node we are placing ourselves into orbit with is already part of a
		// binary orbit, then there is a maximum radius of the new binary node: one
		// which places the minimum distance of the new node directly at the partner of
		// the star we're joining.

		var minRadius = Math.max(getStarExclusionRadius(existing), getStarExclusionRadius(toInsert));

		// FIXME: i dont think this is quite right, and oly works for circular orbits.
		// In a circular orbit, apastron is not well-defined, since the bodies are
		// equidistant at all times. In this way, the apastron defined in StarSystemNode
		// for a circular orbit is just the orbit's diameter.
		var parent = existing.getBinaryParent();
		var maxRadius = parent == null ? MAXIMUM_SYSTEM_RADIUS_TM
				: parent.apastronDistanceTm / BINARY_SYSTEM_SPACING_FACTOR;

		Mod.LOGGER.info("Attempting Insert [min={}, max={}]", minRadius, maxRadius);

		// If there is no place that an orbit can be inserted, signal that to the
		// caller.
		if (minRadius > maxRadius)
			return null;

		var radius = random.nextDouble(minRadius, maxRadius);
		Mod.LOGGER.info("Success [radius={}]", radius);

		var newNode = new BinaryNode(existing, toInsert, systemPlane, 0, radius);
		replaceNode(existing, newNode);
		return newNode;
	}

	public static final double BINARY_SYSTEM_SPACING_FACTOR = 2;

	private static double getStarExclusionRadius(StarNode star) {
		// TODO: exclusion zone for more massive stars should be larger, but im not sure
		// by how much...
		return Units.au(0.5);
	}

	private static @Nullable StarSystemNode mergeStarWithBinary(Random random, OrbitalPlane systemPlane,
			BinaryNode existing, StarNode toInsert) {

		// i kinda hate this, but i cant think of a nicer way to do this rn.
		var parent = existing.getBinaryParent();
		boolean triedPType = false, triedSTypeA = false, triedSTypeB = false;
		while (!triedPType || !triedSTypeA || !triedSTypeB) {

			if ((!triedPType && triedSTypeA && triedSTypeB) || (!triedPType && random.nextBoolean())) {
				triedPType = true;
				// try to merge into a P-type orbit (put star into node with the binary node we
				// were given)

				// We want to avoid putting nodes into P-type orbits that are too close to their
				// partner, as these types of configurations are usually very unstable in real
				// life.
				var minRadius = BINARY_SYSTEM_SPACING_FACTOR * existing.apastronDistanceTm;
				var maxRadius = parent == null ? MAXIMUM_SYSTEM_RADIUS_TM
						: parent.apastronDistanceTm / BINARY_SYSTEM_SPACING_FACTOR;

				Mod.LOGGER.info("Attempting P-Type [min={}, max={}]", minRadius, maxRadius);

				if (minRadius <= maxRadius) {
					var radius = random.nextDouble(minRadius, maxRadius);
					Mod.LOGGER.info("Success [radius={}]", radius);
					var newNode = new BinaryNode(existing, toInsert, systemPlane, 0, radius);
					replaceNode(existing, newNode);
					return newNode;
				}
			} else {
				// try to merge into an S-type orbit (put star into node with one of the child
				// nodes of the binary node we were given)
				var node = existing.getB();
				if (!triedSTypeA && random.nextBoolean()) {
					triedSTypeA = true;
					node = existing.getA();
					Mod.LOGGER.info("Attempting S-Type [A]");
				} else {
					triedSTypeB = true;
					Mod.LOGGER.info("Attempting S-Type [B]");
				}

				var newNode = mergeStarNodes(random, systemPlane, node, toInsert);
				if (newNode != null) {
					return existing;
				}
			}

		}

		return null;
	}

	private static @Nullable StarSystemNode mergeStarNodes(Random random, OrbitalPlane systemPlane,
			StarSystemNode existing, StarNode toInsert) {

		if (existing instanceof StarNode starNode) {
			return mergeSingleStar(random, systemPlane, starNode, toInsert);
		} else if (existing instanceof BinaryNode binaryNode) {
			return mergeStarWithBinary(random, systemPlane, binaryNode, toInsert);
		}

		throw new IllegalArgumentException("tried to merge non-stellar nodes!");
	}

	private static StarSystemNode pairStars(Random random, OrbitalPlane systemPlane,
			List<StarNode> stars) {
		if (stars.isEmpty())
			return null;

		StarSystemNode current = stars.get(0);
		for (var i = 1; i < stars.size(); ++i) {
			final var starToInsert = stars.get(i);
			Mod.LOGGER.info("Placing star #" + i + "");
			var newRoot = mergeStarNodes(random, systemPlane, current, starToInsert);
			if (newRoot == null) {
				Mod.LOGGER.error("Failed to place star #" + i + "!");
			} else {
				current = newRoot;
			}
		}

		// var pairingList = new ArrayList<StarSystemNode>(stars);
		// while (pairingList.size() > 1) {
		// 	var a = pairingList.remove(random.nextInt(0, pairingList.size()));
		// 	var b = pairingList.remove(random.nextInt(0, pairingList.size()));
		// 	if (a == b)
		// 		continue;

		// 	// var minDistance = Double.POSITIVE_INFINITY;
		// 	if (a instanceof StarNode starNodeA && b instanceof StarNode starNodeB) {
		// 		// sun = 1.989e+9 Yg
		// 		var eccentricity = random.nextDouble(0, 0.05);
		// 		var smaTm = random.nextDouble(Units.au(0.5), Units.au(10000));
		// 		var node = new BinaryNode(a, b, systemPlane, eccentricity, smaTm);
		// 		pairingList.add(node);
		// 	}

		// }

		return current;
	}

	public static final double NEUTRON_STAR_MIN_INITIAL_MASS_YG = Units.msol(10);
	public static final double BLACK_HOLE_MIN_INITIAL_MASS_YG = Units.msol(25);

	private static @Nullable StarNode generateStarNode(Random random, double systemAgeMya, double massYg) {
		final var massMsol = massYg / Units.YG_PER_MSOL;

		final var initialLuminosityLsol = Math.pow(massMsol + random.nextDouble(-0.1, 0.1), 3.5);
		final var initialRadiusRsol = Math.pow(massMsol + random.nextDouble(-0.1, 0.1), 0.8);

		final var starLifetime = SOL_LIFETIME_MYA * (massMsol / initialLuminosityLsol);

		var targetType = StarNode.Type.MAIN_SEQUENCE;
		if (systemAgeMya > starLifetime) {
			// TODO: figure out luminosity and mass and stuff
			if (massYg < NEUTRON_STAR_MIN_INITIAL_MASS_YG) {
				targetType = StarNode.Type.WHITE_DWARF;
			} else if (massYg < BLACK_HOLE_MIN_INITIAL_MASS_YG) {
				targetType = StarNode.Type.NEUTRON_STAR;
			} else {
				targetType = StarNode.Type.BLACK_HOLE;
			}
		} else if (systemAgeMya > starLifetime * 0.8) {
			targetType = StarNode.Type.GIANT;
		}

		final var finalMassYg = targetType.curveMass(random, massYg);
		final var luminosityLsol = targetType.curveLuminosity(random, initialLuminosityLsol);
		final var radiusRsol = targetType.curveRadius(random, initialRadiusRsol);
		final var temperatureK = Units.K_PER_TSOL * Math.pow(luminosityLsol, 0.25) * Math.sqrt(1 / radiusRsol);

		return new StarNode(targetType, finalMassYg, luminosityLsol, radiusRsol, temperatureK);
	}

	// 1. Molecular cloud fragment undergoes graivational collapse
	// 2. Protoplanetary disc forms around the protostar
	// 3. Dust grains clump and clear lanes around them producing hundreds of
	// protoplanets
	// 4. protoplanets collide, producing a smaller number of higher-mass planets.

	// after a planetesimal has accreted enough mass, it can start to accrete
	// hydrogen and helium, and turn into a gas giant. There is FAR more gas than
	// dust in the universe, so being able to capture it can make planets very big.

	// frost line

	// planetesimal collisions

	// tidal heating? tidal locking? what causes that?

	// planet migration?
	// seems to play big role in the formation of Sol

}
