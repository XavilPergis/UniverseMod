package net.xavil.ultraviolet.common.universe.system;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.Units;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.system.gen.AccreteContext;
import net.xavil.ultraviolet.common.universe.system.gen.ProtoplanetaryDisc;
import net.xavil.ultraviolet.common.universe.system.gen.SimulationParameters;
import net.xavil.universegen.system.BinaryCelestialNode;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.CelestialNodeChild;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.hawklib.math.Formulas;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.OrbitalPlane;

public class StarSystemGeneratorImpl {

	// how many times larger the radius of an orbit around a binary pair needs to be
	// than the maximum radius of the existing binary pair.
	public static final double SPACING_FACTOR = 2;
	public static final int PLANET_SEED_COUNT = 100;
	public static final int MAX_GENERATION_DEPTH = 5;

	protected final Rng rng;
	protected final Galaxy galaxy;
	protected final GalaxySector.SectorElementHolder info;
	protected double remainingMass;

	protected final double maximumSystemRadius;

	public StarSystemGeneratorImpl(Rng rng, Galaxy galaxy, GalaxySector.SectorElementHolder info) {
		this.rng = rng;
		this.galaxy = galaxy;
		this.info = info;

		this.maximumSystemRadius = rng.uniformDouble(100, 1000);
	}

	public static OrbitalPlane randomOrbitalPlane(Rng rng) {
		return OrbitalPlane.fromOrbitalElements(
				rng.uniformDouble(-Math.PI * 2, Math.PI * 2),
				rng.uniformDouble(-Math.PI * 2, Math.PI * 2),
				rng.uniformDouble(-Math.PI * 2, Math.PI * 2));
	}

	private Interval getStableOrbitInterval(CelestialNode node, OrbitalPlane referencePlane) {
		double currentMin = 0.0, currentMax = Double.POSITIVE_INFINITY;
		final var binaryParent = node.getBinaryParent();
		if (binaryParent != null) {
			var periapsisA = binaryParent.orbitalShapeA.periapsisDistance();
			var periapsisB = binaryParent.orbitalShapeB.periapsisDistance();
			var minPeriapsis = Math.min(periapsisA, periapsisB);
			currentMax = Math.min(currentMax, minPeriapsis / SPACING_FACTOR);
		}
		final var unaryParent = node.getUnaryParent();
		if (unaryParent != null) {
			final var nodeShape = node.getOrbitInfo().orbitalShape;
			// var nodeEffectLimits = Formulas.gravitationalEffectLimits(nodeShape,
			// node.massYg);
			double closestOuter = 0.0, closestInner = Double.POSITIVE_INFINITY;
			for (var sibling : unaryParent.childOrbits()) {
				if (sibling.node == node)
					continue;
				// FIXME: this does not take inclination/etc into account
				// the paths that highly elliptical orbits draw when projected onto the plane of
				// another orbit can cross a lot of other orbits that lay in the same plane,
				// provided that the inclination of the first is low with respect to the plane
				// of the others. since we dont take inclination into account, objects with
				// inclined, highly elliptical orbits (like those of comets) can cause the
				// stability interval to be very different than what it otherwise would be.

				// TODO: very small objects can squat stable orbits, whereas we probably want to
				// allow larger objects to displace the smaller objects instead.

				// assumes that no orbits are overlapping
				var siblingEffectLimits = Formulas.gravitationalEffectLimits(sibling.orbitalShape, sibling.node.massYg);
				if (sibling.orbitalShape.apoapsisDistance() < nodeShape.periapsisDistance()) {
					closestOuter = Math.max(closestOuter, siblingEffectLimits.higher());
				}
				if (sibling.orbitalShape.periapsisDistance() > nodeShape.apoapsisDistance()) {
					closestInner = Math.min(closestInner, siblingEffectLimits.lower());
				}
			}

			// these look backwards but are not
			var innerDistance = nodeShape.periapsisDistance() - closestOuter;
			var outerDistance = nodeShape.apoapsisDistance() - closestInner;
			var minDistance = Math.min(innerDistance, outerDistance);
			currentMax = Math.min(currentMax, minDistance / SPACING_FACTOR);
		}

		if (node instanceof BinaryCelestialNode binaryNode) {
			var apoapsisA = binaryNode.orbitalShapeA.apoapsisDistance();
			var apoapsisB = binaryNode.orbitalShapeB.apoapsisDistance();
			var maxApoapsis = Math.max(apoapsisA, apoapsisB);
			currentMin = Math.max(currentMin, maxApoapsis * SPACING_FACTOR);
		}

		double furthestChild = 0.0;
		for (var child : node.childOrbits()) {
			var childDistance = child.orbitalShape.apoapsisDistance();
			furthestChild = Math.max(furthestChild, childDistance);
		}
		currentMin = Math.max(currentMin, furthestChild * SPACING_FACTOR);

		return new Interval(currentMin, currentMax);
	}

	public static final double MAX_METALLICITY = 0.5;

	public CelestialNode generate() {
		final var node = StellarCelestialNode.fromMassAndAge(this.info.massYg, this.info.systemAgeMyr);

		final var remainingMass = this.info.massYg * this.rng.uniformDouble(0.0, 0.9);

		var metallicity = this.galaxy.densityFields.metallicity.sample(this.info.systemPosTm);
		if (metallicity > MAX_METALLICITY) {
			Mod.LOGGER.warn(
					"Tried to generate star system with a metallicity of '{}', which is greater than the limit of '{}'",
					metallicity, MAX_METALLICITY);
			metallicity = MAX_METALLICITY;
		}

		final var params = new SimulationParameters();
		final var stableInterval = getStableOrbitInterval(node, OrbitalPlane.ZERO);
		final var nodeMass = node.massYg / Units.Yg_PER_Msol;
		final var ctx = new AccreteContext(params, rng, getTotalLuminosity(node), nodeMass, this.info.systemAgeMyr,
				stableInterval);

		final var protoDisc = new ProtoplanetaryDisc(ctx, remainingMass, metallicity);

		try {
			// final var res = protoDisc.build();
			// if (res != null)
			// 	return res;
			protoDisc.collapseDisc(node);
		} catch (Throwable t) {
			t.printStackTrace();
		}

		Mod.LOGGER.error("Failed to generate system!");
		return node;
	}

	private double getTotalLuminosity(CelestialNode node) {
		double total = 0.0;
		if (node instanceof BinaryCelestialNode binaryNode) {
			total += getTotalLuminosity(binaryNode.getA());
			total += getTotalLuminosity(binaryNode.getB());
		}
		for (var child : node.childOrbits()) {
			total += getTotalLuminosity(child.node);
		}
		if (node instanceof StellarCelestialNode starNode) {
			total += starNode.luminosityLsol;
		}
		return total;
	}

	// private void generatePlanets(CelestialNode node, SimulationParameters params,
	// double remainingMass, double metallicity) {
	// generatePlanets(node, params, MAX_GENERATION_DEPTH, remainingMass,
	// metallicity);
	// }

	// private void generatePlanets(CelestialNode node, SimulationParameters params,
	// int remainingDepth, double remainingMass, double metallicity) {
	// if (remainingDepth < 0)
	// return;

	// var nodeLuminosity = getTotalLuminosity(node);
	// if (nodeLuminosity > 0) {
	// var stableInterval = getStableOrbitInterval(node, OrbitalPlane.ZERO);
	// var nodeMass = node.massYg / Units.Yg_PER_Msol;
	// var ctx = new AccreteContext(params, rng, nodeLuminosity, nodeMass,
	// this.info.systemAgeMyr, stableInterval);

	// var protoDisc = new ProtoplanetaryDisc(ctx);
	// try {
	// protoDisc.collapseDisc(node);
	// } catch (Throwable t) {
	// t.printStackTrace();
	// Mod.LOGGER.error("Failed to generate system!");
	// }

	// // TODO: promote brown dwarves and stars
	// }

	// node.visitDirectDescendants(child -> generatePlanets(child, params,
	// remainingDepth - 1));
	// }

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

	private @Nullable CelestialNode mergeSingleStar(StellarCelestialNode existing, StellarCelestialNode toInsert,
			boolean closeOrbit) {

		// if the node we are placing ourselves into orbit with is already part of a
		// binary orbit, then there is a maximum radius of the new binary node: one
		// which places the minimum distance of the new node directly at the partner of
		// the star we're joining.

		final var exclusionRadiusA = 2 * Units.Tu_PER_ku * existing.radius;
		final var exclusionRadiusB = 2 * Units.Tu_PER_ku * toInsert.radius;
		final var minDistance = exclusionRadiusA + exclusionRadiusB;
		final var maxDistance = getMaximumBinaryDistanceForReplacement(existing);
		Mod.LOGGER.info("Attempting Single [min={}, max={}]", minDistance, maxDistance);

		// If there is no place that an orbit can be inserted, signal that to the
		// caller.
		if (minDistance > maxDistance)
			return null;

		double distance = 0;
		if (closeOrbit) {
			var t = Math.pow(rng.uniformDouble(), 3);
			var limit = Units.Tm_PER_au * 10;
			distance = Mth.lerp(t, minDistance, Math.min(maxDistance, limit));
		} else {
			distance = rng.uniformDouble(minDistance, maxDistance);
		}
		Mod.LOGGER.info("Success [distance={}]", distance);

		var squishFactor = 1;
		var newNode = BinaryCelestialNode.fromSquishFactor(existing, toInsert, OrbitalPlane.ZERO, squishFactor,
				distance,
				rng.uniformDouble(0, 2 * Math.PI));
		replaceNode(existing, newNode);
		return newNode;
	}

	private static double getExclusionRadius(CelestialNode node) {
		if (node instanceof StellarCelestialNode starNode) {
			return 10 * Units.Tu_PER_ku * starNode.radius;
		} else if (node instanceof BinaryCelestialNode binaryNode) {
			return SPACING_FACTOR * binaryNode.orbitalShapeB.semiMajor();
		}
		return 0;
	}

	// the maximum distance that a binary node that replaces the passed node can
	// have
	private double getMaximumBinaryDistanceForReplacement(CelestialNode node) {
		var binaryParent = node.getBinaryParent();
		// var unaryParent = node.getUnaryParent();
		if (binaryParent == null)
			return maximumSystemRadius;

		var closestDistance = binaryParent.orbitalShapeA.periapsisDistance()
				+ binaryParent.orbitalShapeB.periapsisDistance();

		return closestDistance / SPACING_FACTOR;
	}

	private @Nullable CelestialNode mergeStarWithBinary(BinaryCelestialNode existing, StellarCelestialNode toInsert,
			boolean closeOrbit) {

		// i kinda hate this, but i cant think of a nicer way to do this rn.
		boolean triedPType = false, triedSTypeA = false, triedSTypeB = false;
		while (!triedPType || !triedSTypeA || !triedSTypeB) {

			if ((!triedPType && triedSTypeA && triedSTypeB) || (!triedPType && rng.chance(0.3))) {
				triedPType = true;
				// try to merge into a P-type orbit (put star into node with the binary node we
				// were given)

				// We want to avoid putting nodes into P-type orbits that are too close to their
				// partner, as these types of configurations are usually very unstable in real
				// life.
				var minRadius = Math.max(getExclusionRadius(existing), getExclusionRadius(toInsert));
				var maxRadius = getMaximumBinaryDistanceForReplacement(existing);

				Mod.LOGGER.info("Attempting P-Type [min={}, max={}]", minRadius, maxRadius);

				if (minRadius <= maxRadius) {
					var radius = rng.uniformDouble(minRadius, maxRadius);
					Mod.LOGGER.info("Success [radius={}]", radius);
					// var squishFactor = Mth.lerp(1 - Math.pow(random.uniformDouble(), 7), 0.2, 1);
					var squishFactor = 1;
					var newNode = BinaryCelestialNode.fromSquishFactor(existing, toInsert, OrbitalPlane.ZERO,
							squishFactor, radius,
							rng.uniformDouble(0, 2 * Math.PI));
					replaceNode(existing, newNode);
					return newNode;
				}
			} else {
				// try to merge into an S-type orbit (put star into node with one of the child
				// nodes of the binary node we were given)
				var node = existing.getB();
				if (!triedSTypeA && rng.chance(0.5)) {
					triedSTypeA = true;
					node = existing.getA();
					Mod.LOGGER.info("Attempting S-Type A");
				} else {
					triedSTypeB = true;
					Mod.LOGGER.info("Attempting S-Type B");
				}

				var newNode = mergeStarNodes(node, toInsert, closeOrbit);
				if (newNode != null) {
					return existing;
				}
			}
		}

		return null;
	}

	private @Nullable CelestialNode mergeStarNodes(CelestialNode existing, StellarCelestialNode toInsert,
			boolean closeOrbit) {

		if (existing instanceof StellarCelestialNode starNode) {
			return mergeSingleStar(starNode, toInsert, closeOrbit);
		} else if (existing instanceof BinaryCelestialNode binaryNode) {
			return mergeStarWithBinary(binaryNode, toInsert, closeOrbit);
		}

		Mod.LOGGER.error("tried to merge non-stellar nodes! " + existing + ", " + toInsert);

		return existing;
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
						binaryNode.orbitalShapeB, binaryNode.orbitalPlane, binaryNode.phase);
				binaryNode.getA().insertChild(unary);

				for (final var child : binaryNode.childOrbits()) {
					final var newChildInfo = new CelestialNodeChild<>(binaryNode.getA(), child.node, child.orbitalShape,
							child.orbitalPlane, child.phase);
					binaryNode.getA().insertChild(newChildInfo);
				}

				return binaryNode.getA();
			}
		}

		return node;
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
