package net.xavil.universal.common.universe.system;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.galaxy.Galaxy;

public class StarSystemGenerator {

	// how many times larger the radius of an orbit around a binary pair needs to be
	// than the maximum radius of the existing binary pair.
	public static final double BINARY_SYSTEM_SPACING_FACTOR = 3;
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

	public StarSystemNode generate() {
		final var stars = info.getStars().toList();

		// should never happen
		if (stars.isEmpty())
			throw new IllegalArgumentException("cannot generate a star system with no stars!");

		StarSystemNode current = stars.get(0);
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

		placePlanets(current);
		determineOrbitalPlanes(current);

		return current;
	}

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

	private @Nullable StarSystemNode mergeSingleStar(StarNode existing, StarNode toInsert) {

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
		var newNode = new BinaryNode(existing, toInsert, OrbitalPlane.ZERO, squishFactor, radius,
				random.nextDouble(2 * Math.PI));
		replaceNode(existing, newNode);
		return newNode;
	}

	private static double getExclusionRadius(StarSystemNode node) {
		if (node instanceof StarNode starNode) {
			// TODO: exclusion zone for more massive stars should be larger, but im not sure
			// by how much...
			return Units.au(0.5) * starNode.massYg / Units.YG_PER_MSOL;
		} else if (node instanceof BinaryNode binaryNode) {
			return BINARY_SYSTEM_SPACING_FACTOR * binaryNode.maxOrbitalRadiusTm;
		}
		return 0;
	}

	private double getMaximumRadius(StarSystemNode node) {
		var parent = node.getBinaryParent();
		if (parent == null)
			return maximumSystemRadius;
		// FIXME: i dont think this is quite right, and oly works for circular orbits.
		// In a circular orbit, apastron is not well-defined, since the bodies are
		// equidistant at all times. In this way, the apastron defined in StarSystemNode
		// for a circular orbit is just the orbit's diameter.
		return parent.maxOrbitalRadiusTm / BINARY_SYSTEM_SPACING_FACTOR;
	}

	private @Nullable StarSystemNode mergeStarWithBinary(BinaryNode existing, StarNode toInsert) {

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
					var newNode = new BinaryNode(existing, toInsert, OrbitalPlane.ZERO, squishFactor, radius,
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

	private @Nullable StarSystemNode mergeStarNodes(StarSystemNode existing, StarNode toInsert) {

		if (existing instanceof StarNode starNode) {
			return mergeSingleStar(starNode, toInsert);
		} else if (existing instanceof BinaryNode binaryNode) {
			return mergeStarWithBinary(binaryNode, toInsert);
		}

		throw new IllegalArgumentException("tried to merge non-stellar nodes!");
	}

	private void placePlanets(StarSystemNode node) {
		if (node instanceof BinaryNode binaryNode) {
			placePlanets(binaryNode.getA());
			placePlanets(binaryNode.getB());
		}
		for (var childOrbit : node.childOrbits()) {
			placePlanets(childOrbit.node);
		}

		var massMsol = node.massYg / Units.YG_PER_MSOL;
		var stabilityLimit = Units.au(1000) * Math.pow(massMsol, 1.5) * 0.66;

		var minRadius = getExclusionRadius(node);
		var maxRadius = getMaximumRadius(node);
		maxRadius = 1.4 * Math.min(maxRadius, stabilityLimit);
		if (minRadius > maxRadius)
			return;

		if (node instanceof StarNode starNode) {

			var capturedPlanetCountDouble = Mth.lerp(Math.pow(random.nextDouble(), 2), 0, 20);
			var capturedPlanetCount = (int) Math.floor(capturedPlanetCountDouble);

			for (var i = 0; i < capturedPlanetCount; ++i) {
				// FIXME: do something real
				// we want to take into account how much dust and gas the stars snatched up
				// we dont want to have overlapping orbits
				var initialMass = random.nextDouble(10, 1e7);
				var initialOrbitalRadius = random.nextDouble(minRadius, maxRadius);

				var typesLength = PlanetNode.Type.values().length;
				var randomType = PlanetNode.Type.values()[random.nextInt(typesLength)];
				var r = random.nextDouble(0.02, 30);

				var planetNode = new PlanetNode(randomType, initialMass, r, 300);
				var ecc = Math.pow(random.nextDouble(0, 0.5), 3);
				var orbitalShape = new OrbitalShape(ecc, initialOrbitalRadius);

				var orbit = new StarSystemNode.UnaryOrbit(planetNode, orbitalShape, OrbitalPlane.ZERO,
						random.nextDouble(2 * Math.PI));
				node.insertChild(orbit);
			}
		}

		var capturedPlanetCountDouble = Mth.lerp(Math.pow(random.nextDouble(), 20), 0, 20);
		var capturedPlanetCount = (int) Math.floor(capturedPlanetCountDouble);

		// TODO: captured objects should probably have higher eccentricities than usual
		for (var i = 0; i < capturedPlanetCount; ++i) {
			// FIXME: do something real
			// we want to take into account how much dust and gas the stars snatched up
			// we dont want to have overlapping orbits
			var initialMass = random.nextDouble(10, 1e7);
			var initialOrbitalRadius = random.nextDouble(minRadius, maxRadius);

			var typesLength = PlanetNode.Type.values().length;
			var randomType = PlanetNode.Type.values()[random.nextInt(typesLength)];
			var r = random.nextDouble(0.02, 30);

			var planetNode = new PlanetNode(randomType, initialMass, r, 300);
			var ecc = Math.pow(random.nextDouble(0, 0.5), 3);
			var orbitalShape = new OrbitalShape(ecc, initialOrbitalRadius);

			var orbit = new StarSystemNode.UnaryOrbit(planetNode, orbitalShape, OrbitalPlane.ZERO,
					random.nextDouble(2 * Math.PI));
			node.insertChild(orbit);
		}
	}

	private void determineOrbitalPlanes(StarSystemNode node) {

		var massMsol = node.massYg / Units.YG_PER_MSOL;
		var stabilityLimit = Units.au(1000) * Math.pow(massMsol, 1.5) * 0.66;

		var ta = 2 * Math.PI * Math.pow(random.nextDouble(), 4);
		node.obliquityAngle = random.nextDouble(-ta, ta);

		// earth speed: approx. 7e-5 rad/s
		// conservation of angular momentum? small bodies might spin MUCH faster
		node.rotationalSpeed = Mth.lerp(Math.pow(random.nextDouble(), 4), 0.2 * 7e-5, 200 * 7e-5);
		if (random.nextFloat() < 0.05)
			node.rotationalSpeed *= -1;

		if (node instanceof BinaryNode binaryNode) {
			determineOrbitalPlanes(binaryNode.getA());
			determineOrbitalPlanes(binaryNode.getB());

			// basically, we want to determine if stars are in orbit because they formed in
			// different cores with different orbital planes and found their ways into
			// orbits, or if they formed as part of a disc fragmentation event.
			if (binaryNode.maxOrbitalRadiusTm > stabilityLimit) {
				binaryNode.orbitalPlane = randomOrbitalPlane(random);
			} else {
				var t = 0.1 * Math.pow(binaryNode.maxOrbitalRadiusTm / stabilityLimit, 4);
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

			if (childOrbit.orbitalShape.semimajorAxisTm() > stabilityLimit) {
				childOrbit.orbitalPlane = randomOrbitalPlane(random);
			} else {
				var t = 0.1 * Math.pow(childOrbit.orbitalShape.semimajorAxisTm() / stabilityLimit, 4);
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
