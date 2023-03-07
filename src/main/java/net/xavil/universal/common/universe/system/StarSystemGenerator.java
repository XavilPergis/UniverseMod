package net.xavil.universal.common.universe.system;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.system.gen.AccreteContext;
import net.xavil.universal.common.universe.system.gen.ProtoplanetaryDisc;
import net.xavil.universal.common.universe.system.gen.SimulationParameters;
import net.xavil.universegen.system.BinaryCelestialNode;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.universegen.system.CelestialNodeChild;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.util.Units;
import net.xavil.util.math.Formulas;
import net.xavil.util.math.OrbitalPlane;

public class StarSystemGenerator {

	// how many times larger the radius of an orbit around a binary pair needs to be
	// than the maximum radius of the existing binary pair.
	public static final double BINARY_SYSTEM_SPACING_FACTOR = 4;
	public static final int PLANET_SEED_COUNT = 100;

	protected final Random random;
	protected final Galaxy galaxy;
	protected final StarSystem.Info info;

	protected final double maximumSystemRadius;

	public StarSystemGenerator(Random random, Galaxy galaxy, StarSystem.Info info) {
		this.random = random;
		this.galaxy = galaxy;
		this.info = info;

		this.maximumSystemRadius = random.nextDouble(100, 10000);
	}

	public static OrbitalPlane randomOrbitalPlane(Random random) {
		return OrbitalPlane.fromOrbitalElements(
				random.nextDouble(-Math.PI * 2, Math.PI * 2),
				random.nextDouble(-Math.PI * 2, Math.PI * 2),
				random.nextDouble(-Math.PI * 2, Math.PI * 2));
	}

	public CelestialNode generate() {
		final var stars = info.getStars().toList();

		// should never happen
		if (stars.isEmpty())
			throw new IllegalArgumentException("cannot generate a star system with no stars!");

		CelestialNode current = stars.get(0);
		for (var i = 1; i < stars.size(); ++i) {
			final var starToInsert = stars.get(i);
			Mod.LOGGER.info("Placing star #" + i + "");
			var newRoot = mergeStarNodes(current, starToInsert);
			if (newRoot != null) {
				current = newRoot;
			} else {
				Mod.LOGGER.error("Failed to place star #" + i + "!");
			}
		}

		// TODO: if stars have very large mass differences, we might consider putting
		// the smaller star in a unary orbit around the mroe massive star

		current = convertBinaryOrbits(current);
		placePlanets(current);
		determineOrbitalPlanes(current);

		return current;
	}

	private static void replaceNode(CelestialNode existing, BinaryCelestialNode newNode) {
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

	private @Nullable CelestialNode mergeSingleStar(StellarCelestialNode existing, StellarCelestialNode toInsert) {

		// if the node we are placing ourselves into orbit with is already part of a
		// binary orbit, then there is a maximum radius of the new binary node: one
		// which places the minimum distance of the new node directly at the partner of
		// the star we're joining.

		var minRadius = Math.max(getExclusionRadius(existing), getExclusionRadius(toInsert));
		var maxRadius = getMaximumRadius(existing);
		Mod.LOGGER.info("Attempting Single [min={}, max={}]", minRadius, maxRadius);

		// If there is no place that an orbit can be inserted, signal that to the
		// caller.
		if (minRadius > maxRadius)
			return null;

		var radius = random.nextDouble(minRadius, maxRadius);
		Mod.LOGGER.info("Success [radius={}]", radius);

		// var squishFactor = Mth.lerp(1 - Math.pow(random.nextDouble(), 7), 0.2, 1);
		var squishFactor = 1;
		var newNode = new BinaryCelestialNode(existing, toInsert, OrbitalPlane.ZERO, squishFactor, radius,
				random.nextDouble(2 * Math.PI));
		replaceNode(existing, newNode);
		return newNode;
	}

	private static double getExclusionRadius(CelestialNode node) {
		if (node instanceof StellarCelestialNode starNode) {
			return 10 * Units.m_PER_Rsol / 1e12 * starNode.radiusRsol;
		} else if (node instanceof BinaryCelestialNode binaryNode) {
			return BINARY_SYSTEM_SPACING_FACTOR * binaryNode.orbitalShapeB.semiMajor();
		}
		return 0;
	}

	private double getMaximumRadius(CelestialNode node) {
		var parent = node.getBinaryParent();
		if (parent == null)
			return maximumSystemRadius;
		// FIXME: i dont think this is quite right, and oly works for circular orbits.
		// In a circular orbit, apastron is not well-defined, since the bodies are
		// equidistant at all times. In this way, the apastron defined in StarSystemNode
		// for a circular orbit is just the orbit's diameter.
		return parent.orbitalShapeB.semiMajor() / BINARY_SYSTEM_SPACING_FACTOR;
	}

	private @Nullable CelestialNode mergeStarWithBinary(BinaryCelestialNode existing, StellarCelestialNode toInsert) {

		// i kinda hate this, but i cant think of a nicer way to do this rn.
		boolean triedPType = false, triedSTypeA = false, triedSTypeB = false;
		while (!triedPType || !triedSTypeA || !triedSTypeB) {

			if ((!triedPType && triedSTypeA && triedSTypeB) || (!triedPType && random.nextBoolean())) {
				triedPType = true;
				// try to merge into a P-type orbit (put star into node with the binary node we
				// were given)

				// We want to avoid putting nodes into P-type orbits that are too close to their
				// partner, as these types of configurations are usually very unstable in real
				// life.
				var minRadius = Math.max(getExclusionRadius(existing), getExclusionRadius(toInsert));
				var maxRadius = getMaximumRadius(existing);

				Mod.LOGGER.info("Attempting P-Type [min={}, max={}]", minRadius, maxRadius);

				if (minRadius <= maxRadius) {
					var radius = random.nextDouble(minRadius, maxRadius);
					Mod.LOGGER.info("Success [radius={}]", radius);
					// var squishFactor = Mth.lerp(1 - Math.pow(random.nextDouble(), 7), 0.2, 1);
					var squishFactor = 1;
					var newNode = new BinaryCelestialNode(existing, toInsert, OrbitalPlane.ZERO, squishFactor, radius,
							random.nextDouble(2 * Math.PI));
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
					Mod.LOGGER.info("Attempting S-Type A");
				} else {
					triedSTypeB = true;
					Mod.LOGGER.info("Attempting S-Type B");
				}

				var newNode = mergeStarNodes(node, toInsert);
				if (newNode != null) {
					return existing;
				}
			}

		}

		return null;
	}

	private @Nullable CelestialNode mergeStarNodes(CelestialNode existing, StellarCelestialNode toInsert) {

		if (existing instanceof StellarCelestialNode starNode) {
			return mergeSingleStar(starNode, toInsert);
		} else if (existing instanceof BinaryCelestialNode binaryNode) {
			return mergeStarWithBinary(binaryNode, toInsert);
		}

		throw new IllegalArgumentException("tried to merge non-stellar nodes!");
	}

	public static final double UNARY_ORBIT_THRESHOLD = 0.05;

	private CelestialNode convertBinaryOrbits(CelestialNode node) {
		for (var childOrbit : node.childOrbits()) {
			convertBinaryOrbits(childOrbit.node);
		}

		if (node instanceof BinaryCelestialNode binaryNode) {
			binaryNode.setA(convertBinaryOrbits(binaryNode.getA()));
			binaryNode.setB(convertBinaryOrbits(binaryNode.getB()));
			if (UNARY_ORBIT_THRESHOLD * binaryNode.getA().massYg > binaryNode.getB().massYg) {
				final var unary = new CelestialNodeChild<>(binaryNode.getA(), binaryNode.getB(),
						binaryNode.orbitalShapeB, binaryNode.orbitalPlane, 0);
				binaryNode.getA().insertChild(unary);
				return binaryNode.getA();
			}
		}

		return node;
	}

	private void placePlanetsAroundStar(StellarCelestialNode node) {

		var params = new SimulationParameters();
		var ctx = new AccreteContext(params, random, node.luminosityLsol, node.massYg / Units.Yg_PER_Msol);
		var protoDisc = new ProtoplanetaryDisc(ctx);

		try {
			protoDisc.collapseDisc(node);
		} catch (Exception ex) {
			ex.printStackTrace();
			Mod.LOGGER.error("accrete error " + ex);
		}

		// var minRadius = getExclusionRadius(node);
		// var maxRadius = getMaximumRadius(node);

		// var protoDiscMass = random.nextDouble(0.001, 0.3) * node.massYg;
		// var protoDiscDustPercent = random.nextDouble(0.001, 0.05);
		// var protoDiscSize = random.nextDouble(0.001, 0.3);

		// var curRadius = 2 * random.nextDouble() + minRadius;
		// var maxAttempts = random.nextInt(0, 30);
		// for (var attempt = 0; attempt < maxAttempts; ++attempt) {
		// if (random.nextDouble() < 0.05 || curRadius > maxRadius)
		// break;

		// // idk lol
		// var frostLine = Units.fromAu(5) * Math.sqrt(node.luminosityLsol);

		// var albedo = random.nextDouble();

		// var starLuminosityW = Units.W_PER_Lsol * node.luminosityLsol;
		// var temperatureK = Math.pow(starLuminosityW * (1 - albedo) / (Math.PI *
		// Units.BOLTZMANN_CONSTANT_W_PER_m2K4), 0.25)
		// / (2 * Math.sqrt(curRadius * 1e12));

		// PlanetNode.Type type = null;

		// double planetRadius = 1;
		// var initialMass = Units.fromMearth(random.nextDouble(0.01, 500));
		// if (curRadius < frostLine) {
		// initialMass = Units.fromMearth(random.nextDouble(0.05, 2));
		// }

		// if (curRadius > frostLine) {
		// boolean isGasGiant = random.nextDouble() < 0.6;
		// if (isGasGiant) {
		// initialMass = Units.fromMearth(random.nextDouble(50, 500));
		// type = PlanetNode.Type.GAS_GIANT;
		// planetRadius = (Units.m_PER_Rjupiter / Units.m_PER_Rearth) *
		// random.nextDouble(0.7, 1.5);
		// } else {
		// initialMass = Units.fromMearth(random.nextDouble(0.1, 10));
		// var isRocky = random.nextDouble() < 0.5;
		// type = isRocky ? PlanetNode.Type.ROCKY_ICE_WORLD : PlanetNode.Type.ICE_WORLD;
		// planetRadius = initialMass / Units.Yg_PER_Mearth * random.nextDouble(0.6,
		// 1.4);
		// }
		// } else if (curRadius > frostLine * 0.8) {
		// initialMass = Units.fromMearth(random.nextDouble(0.05, 2));
		// if (temperatureK > 280 && temperatureK < 290 && initialMass > 0.8) {
		// type = PlanetNode.Type.EARTH_LIKE_WORLD;
		// planetRadius = initialMass / Units.Yg_PER_Mearth * random.nextDouble(0.85,
		// 1.15);
		// } else {
		// type = PlanetNode.Type.ROCKY_WORLD;
		// planetRadius = initialMass / Units.Yg_PER_Mearth * random.nextDouble(0.85,
		// 1.15);
		// }
		// } else {
		// initialMass = Units.fromMearth(random.nextDouble(0.05, 2));
		// type = PlanetNode.Type.ROCKY_WORLD;
		// planetRadius = initialMass / Units.Yg_PER_Mearth * random.nextDouble(0.5,
		// 1.15);
		// }

		// // var typesLength = PlanetNode.Type.values().length;
		// // var randomType = PlanetNode.Type.values()[random.nextInt(typesLength)];
		// // var planetRadius = random.nextDouble(0.02, 30);

		// var planetNode = new PlanetNode(type, initialMass, planetRadius,
		// temperatureK);
		// var ecc = Math.pow(random.nextDouble(0, 1), 3) / 2;
		// var orbitalShape = new OrbitalShape(ecc, curRadius);

		// var orbit = new StarSystemNode.UnaryOrbit(node, planetNode, orbitalShape,
		// OrbitalPlane.ZERO,
		// random.nextDouble(2 * Math.PI));
		// node.insertChild(orbit);

		// curRadius *= Mth.lerp(Math.pow(random.nextDouble(), 4), 1.4, 6);

		// placePlanets(planetNode);
		// }
	}

	private void placePlanetsAroundGasGiant(PlanetaryCelestialNode node) {
		var minRadius = 5 * (Units.m_PER_Rearth / 1e12) * node.radiusRearth;
		var largeMoonCount = random.nextInt(0, 5);
	}

	private void placePlanets(CelestialNode node) {
		if (node instanceof BinaryCelestialNode binaryNode) {
			placePlanets(binaryNode.getA());
			placePlanets(binaryNode.getB());
		} else if (node instanceof StellarCelestialNode starNode) {
			for (var childOrbit : node.childOrbits()) {
				placePlanets(childOrbit.node);
			}

			placePlanetsAroundStar(starNode);
		} else if (node instanceof PlanetaryCelestialNode planetNode) {
			placePlanetsAroundGasGiant(planetNode);
		}

		// var massMsol = node.massYg / Units.YG_PER_MSOL;
		// var stabilityLimit = Units.au(1000) * Math.pow(massMsol, 1.5) * 0.66;

		// var minRadius = getExclusionRadius(node);
		// var maxRadius = getMaximumRadius(node);
		// maxRadius = 1.4 * Math.min(maxRadius, stabilityLimit);
		// if (minRadius >= maxRadius)
		// return;

		// if (node instanceof StarNode starNode) {

		// var capturedPlanetCountDouble = Mth.lerp(Math.pow(random.nextDouble(), 2), 0,
		// 20);
		// var capturedPlanetCount = (int) Math.floor(capturedPlanetCountDouble);

		// for (var i = 0; i < capturedPlanetCount; ++i) {
		// // FIXME: do something real
		// // we want to take into account how much dust and gas the stars snatched up
		// // we dont want to have overlapping orbits
		// var initialMass = random.nextDouble(10, 1e7);
		// var initialOrbitalRadius = random.nextDouble(minRadius, maxRadius);

		// var typesLength = PlanetNode.Type.values().length;
		// var randomType = PlanetNode.Type.values()[random.nextInt(typesLength)];
		// var r = random.nextDouble(0.02, 30);

		// var planetNode = new PlanetNode(randomType, initialMass, r, 300);
		// var ecc = Math.pow(random.nextDouble(0, 0.5), 3);
		// var orbitalShape = new OrbitalShape(ecc, initialOrbitalRadius);

		// var orbit = new StarSystemNode.UnaryOrbit(node, planetNode, orbitalShape,
		// OrbitalPlane.ZERO,
		// random.nextDouble(2 * Math.PI));
		// node.insertChild(orbit);
		// }
		// }

		// var capturedPlanetCountDouble = Mth.lerp(Math.pow(random.nextDouble(), 20),
		// 0, 20);
		// var capturedPlanetCount = (int) Math.floor(capturedPlanetCountDouble);

		// // TODO: captured objects should probably have higher eccentricities than
		// usual
		// for (var i = 0; i < capturedPlanetCount; ++i) {
		// // FIXME: do something real
		// // we want to take into account how much dust and gas the stars snatched up
		// // we dont want to have overlapping orbits
		// var initialMass = random.nextDouble(10, 1e7);
		// var initialOrbitalRadius = random.nextDouble(minRadius, maxRadius);

		// var typesLength = PlanetNode.Type.values().length;
		// var randomType = PlanetNode.Type.values()[random.nextInt(typesLength)];
		// var r = random.nextDouble(0.02, 30);

		// var planetNode = new PlanetNode(randomType, initialMass, r, 300);
		// var ecc = Math.pow(random.nextDouble(0, 0.5), 3);
		// var orbitalShape = new OrbitalShape(ecc, initialOrbitalRadius);

		// var orbit = new StarSystemNode.UnaryOrbit(node, planetNode, orbitalShape,
		// OrbitalPlane.ZERO,
		// random.nextDouble(2 * Math.PI));
		// node.insertChild(orbit);
		// }
	}

	private double timeUntilTidallyLockedMya(CelestialNodeChild<?> orbit) {
		if (orbit.node instanceof PlanetaryCelestialNode planetNode) {

			var rigidity = planetNode.type.rigidity;
			if (Double.isNaN(rigidity))
				return Double.NaN;

			var meanRadiusM = Units.m_PER_Rearth * planetNode.radiusRearth;

			var denom = 1e9 * orbit.node.massYg * Math.pow(1e9 * orbit.parentNode.massYg, 2);
			var lockingTime = 6 * Math.pow(orbit.orbitalShape.semiMajor() * 1e12, 6) * meanRadiusM * rigidity / denom;
			return lockingTime / 1e4;
		}
		return Double.NaN;
	}

	private void determineOrbitalPlanes(CelestialNode node) {

		var massMsol = node.massYg / Units.Yg_PER_Msol;
		var stabilityLimit = Units.fromAu(10000) * Math.pow(massMsol, 1.5) * 0.66;

		var ta = 2 * Math.PI * Math.pow(random.nextDouble(), 4);
		node.obliquityAngle = random.nextDouble(-ta, ta);

		// earth speed: approx. 7e-5 rad/s
		// conservation of angular momentum? small bodies might spin MUCH faster
		node.rotationalPeriod = Mth.lerp(random.nextDouble(), 0.2 * 86400, 4 * 86400);
		if (random.nextFloat() < 0.05)
			node.rotationalPeriod *= -1;

		if (node instanceof BinaryCelestialNode binaryNode) {
			determineOrbitalPlanes(binaryNode.getA());
			determineOrbitalPlanes(binaryNode.getB());

			// basically, we want to determine if stars are in orbit because they formed in
			// different cores with different orbital planes and found their ways into
			// orbits, or if they formed as part of a disc fragmentation event.
			if (binaryNode.orbitalShapeB.semiMajor() > stabilityLimit) {
				binaryNode.orbitalPlane = randomOrbitalPlane(random);
			} else {
				var t = 0.1 * Math.pow(binaryNode.orbitalShapeB.semiMajor() / stabilityLimit, 4);
				binaryNode.orbitalPlane = OrbitalPlane.fromOrbitalElements(
						2 * Math.PI * random.nextDouble(-t, t),
						2 * Math.PI * random.nextDouble(-1, 1),
						2 * Math.PI * random.nextDouble(-1, 1));
			}
			// binaryNode.orbitalPlane = OrbitalPlane.fromOrbitalElements(
			// Math.PI / 8 * random.nextDouble(-1, 1),
			// 2 * Math.PI * random.nextDouble(-1, 1),
			// 2 * Math.PI * random.nextDouble(-1, 1));
		}
		for (var childOrbit : node.childOrbits()) {
			determineOrbitalPlanes(childOrbit.node);

			var lockingTimeMya = timeUntilTidallyLockedMya(childOrbit);
			var orbitalPeriod = Formulas.orbitalPeriod(childOrbit.orbitalShape.semiMajor(),
					childOrbit.parentNode.massYg);
			var lockingPercentage = Math.min(1, lockingTimeMya / this.info.systemAgeMya);
			node.rotationalPeriod = Mth.lerp(lockingPercentage, node.rotationalPeriod, orbitalPeriod);

			if (childOrbit.orbitalShape.semiMajor() > stabilityLimit || childOrbit.orbitalShape.eccentricity() > 0.3) {
				childOrbit.orbitalPlane = randomOrbitalPlane(random);
			} else {
				var t = 0.1 * Math.pow(childOrbit.orbitalShape.semiMajor() / stabilityLimit, 4);
				t += 0.2 * Math.pow(random.nextDouble(), 20);
				childOrbit.orbitalPlane = OrbitalPlane.fromOrbitalElements(
						2 * Math.PI * random.nextDouble(-t, t),
						2 * Math.PI * random.nextDouble(-1, 1),
						2 * Math.PI * random.nextDouble(-1, 1));
			}
			// childOrbit.orbitalPlane = OrbitalPlane.fromOrbitalElements(
			// Math.PI / 32 * random.nextDouble(-1, 1),
			// 2 * Math.PI * random.nextDouble(-1, 1),
			// 2 * Math.PI * random.nextDouble(-1, 1));
		}
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
