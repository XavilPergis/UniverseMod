package net.xavil.ultraviolet.common.universe.system;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.StableRandom;
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

public class RealisticStarSystemGenerator implements StarSystemGenerator {

	// how many times larger the radius of an orbit around a binary pair needs to be
	// than the maximum radius of the existing binary pair.
	public static final double SPACING_FACTOR = 2;
	public static final int PLANET_SEED_COUNT = 100;
	public static final int MAX_GENERATION_DEPTH = 5;

	protected StableRandom rootRng;
	protected StableRandom currentRng;
	protected Galaxy galaxy;
	protected GalaxySector.SectorElementHolder info;
	protected double remainingMass;

	protected double maximumSystemRadius;

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
			var periapsisA = binaryParent.orbitalShapeInner.periapsisDistance();
			var periapsisB = binaryParent.orbitalShapeOuter.periapsisDistance();
			var minPeriapsis = Math.min(periapsisA, periapsisB);
			currentMax = Math.min(currentMax, minPeriapsis / SPACING_FACTOR);
		}
		final var unaryParent = node.getUnaryParent();
		if (unaryParent != null) {
			final var nodeShape = node.getOrbitInfo().orbitalShape;
			// var nodeEffectLimits = Formulas.gravitationalEffectLimits(nodeShape,
			// node.massYg);
			double closestOuter = 0.0, closestInner = Double.POSITIVE_INFINITY;
			for (var sibling : unaryParent.childNodes.iterable()) {
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
					closestOuter = Math.max(closestOuter, siblingEffectLimits.higher);
				}
				if (sibling.orbitalShape.periapsisDistance() > nodeShape.apoapsisDistance()) {
					closestInner = Math.min(closestInner, siblingEffectLimits.lower);
				}
			}

			// these look backwards but are not
			var innerDistance = nodeShape.periapsisDistance() - closestOuter;
			var outerDistance = nodeShape.apoapsisDistance() - closestInner;
			var minDistance = Math.min(innerDistance, outerDistance);
			currentMax = Math.min(currentMax, minDistance / SPACING_FACTOR);
		}

		if (node instanceof BinaryCelestialNode binaryNode) {
			var apoapsisA = binaryNode.orbitalShapeInner.apoapsisDistance();
			var apoapsisB = binaryNode.orbitalShapeOuter.apoapsisDistance();
			var maxApoapsis = Math.max(apoapsisA, apoapsisB);
			currentMin = Math.max(currentMin, maxApoapsis * SPACING_FACTOR);
		}

		double furthestChild = 0.0;
		for (var child : node.childNodes.iterable()) {
			var childDistance = child.orbitalShape.apoapsisDistance();
			furthestChild = Math.max(furthestChild, childDistance);
		}
		currentMin = Math.max(currentMin, furthestChild * SPACING_FACTOR);

		return new Interval(currentMin, currentMax);
	}

	public static final double MAX_METALLICITY = 0.2;

	// lmao this is ass
	@Override
	public CelestialNode generate(Context ctx) {
		this.rootRng = new StableRandom(ctx.rng.uniformLong());
		this.currentRng = this.rootRng;
		this.galaxy = ctx.galaxy;
		this.info = ctx.info;
		this.maximumSystemRadius = this.rootRng.uniformDouble("maximum_system_radius", 100, 1000);
		return generate();
	}

	public CelestialNode generate() {
		final var stellarProperties = new StellarCelestialNode.Properties();
		stellarProperties.load(this.info.massYg, 0.0);
		final var node = StellarCelestialNode.fromMassAndAge(this.info.massYg, this.info.systemAgeMyr);

		final var remainingMass = this.rootRng.uniformDouble("remaining_mass", 0.05, 0.9 * this.info.massYg);

		var metallicity = this.galaxy.densityFields.metallicity.sample(this.info.systemPosTm);
		if (metallicity > MAX_METALLICITY) {
			// Mod.LOGGER.warn(
			// 		"Tried to generate star system with a metallicity of '{}', which is greater than the limit of '{}'",
			// 		metallicity, MAX_METALLICITY);
			metallicity = MAX_METALLICITY;
		}

		metallicity = this.rootRng.weightedDouble("metallicity", 4, 0.001, 0.1);

		final var params = new SimulationParameters();
		final var stableInterval = getStableOrbitInterval(node, OrbitalPlane.ZERO).mul(1.0 / Units.Tm_PER_au);
		final var nodeMass = Units.Msol_PER_Yg * node.massYg;
		final var ctx = new AccreteContext(params, this.rootRng.split("system_gen"), stellarProperties.luminosityLsol, nodeMass,
				this.info.systemAgeMyr,
				metallicity, stableInterval);

		final var protoDisc = new ProtoplanetaryDisc(ctx, remainingMass);

		try {
			// final var res = protoDisc.build();
			// if (res != null)
			// return res;
			protoDisc.collapseDisc(node);
			return node;
		} catch (Throwable t) {
			t.printStackTrace();
			Mod.LOGGER.error("Failed to generate system!");
			throw t;
		}
	}

	private double getTotalLuminosity(CelestialNode node) {
		double total = 0.0;
		if (node instanceof BinaryCelestialNode binaryNode) {
			total += getTotalLuminosity(binaryNode.getInner());
			total += getTotalLuminosity(binaryNode.getOuter());
		}
		for (var child : node.childNodes.iterable()) {
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
		newNode.getInner().setBinaryParent(newNode);
		newNode.getOuter().setBinaryParent(newNode);
	}

	private @Nullable CelestialNode mergeSingleStar(StableRandom rng, StellarCelestialNode existing,
			StellarCelestialNode toInsert, boolean closeOrbit) {

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
			var limit = Units.Tm_PER_au * 10;
			distance = rng.weightedDouble("distance", 3, minDistance, Math.min(maxDistance, limit));
		} else {
			distance = rng.uniformDouble("distance", minDistance, maxDistance);
		}
		Mod.LOGGER.info("Success [distance={}]", distance);

		var squishFactor = 1;
		var newNode = BinaryCelestialNode.fromSquishFactor(existing, toInsert, OrbitalPlane.ZERO, squishFactor,
				distance, rng.uniformDouble("orbital_phase", 0, 2 * Math.PI));
		replaceNode(existing, newNode);
		return newNode;
	}

	private static double getExclusionRadius(CelestialNode node) {
		if (node instanceof StellarCelestialNode starNode) {
			return 10 * Units.Tu_PER_ku * starNode.radius;
		} else if (node instanceof BinaryCelestialNode binaryNode) {
			return SPACING_FACTOR * binaryNode.orbitalShapeOuter.semiMajor();
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

		var closestDistance = binaryParent.orbitalShapeInner.periapsisDistance()
				+ binaryParent.orbitalShapeOuter.periapsisDistance();

		return closestDistance / SPACING_FACTOR;
	}

	private @Nullable CelestialNode mergeStarWithBinary(StableRandom rng, BinaryCelestialNode existing,
			StellarCelestialNode toInsert, boolean closeOrbit) {

		// i kinda hate this, but i cant think of a nicer way to do this rn.
		boolean triedOuter = false, triedInnerA = false, triedInnerB = false;
		while (!triedOuter || !triedInnerA || !triedInnerB) {

			if (!triedOuter && (triedInnerA && triedInnerB || rng.chance("try_outer", 0.3))) {
				triedOuter = true;

				// We want to avoid putting nodes into P-type orbits that are too close to their
				// partner, as these types of configurations are usually very unstable in real
				// life.
				final var minRadius = Math.max(getExclusionRadius(existing), getExclusionRadius(toInsert));
				final var maxRadius = getMaximumBinaryDistanceForReplacement(existing);

				Mod.LOGGER.info("Attempting P-Type [min={}, max={}]", minRadius, maxRadius);

				if (minRadius <= maxRadius) {
					final var radius = rng.weightedDouble("orbital_radius", 2, minRadius, maxRadius);
					Mod.LOGGER.info("Success [radius={}]", radius);

					OrbitalPlane orbitalPlane;
					if (closeOrbit) {
						orbitalPlane = existing.orbitalPlane;
						// existing.orbitalPlane = OrbitalPlane.ZERO;
					} else {
						orbitalPlane = OrbitalPlane.fromInclination(
								rng.uniformDouble("inclination", 0, 2.0 * Math.PI),
								rng.split("orbital_plane"));
					}

					final var squishFactor = 1;
					final var phase = rng.uniformDouble("orbital_phase", 0, 2 * Math.PI);
					final var newNode = BinaryCelestialNode.fromSquishFactor(existing, toInsert, orbitalPlane,
							squishFactor, radius, phase);
					replaceNode(existing, newNode);
					return newNode;
				}
			} else {
				var a = existing.getInner();
				var b = existing.getOuter();

				if (!triedInnerA && rng.chance("inner", 0.5)) {
					triedInnerA = true;
					Mod.LOGGER.info("Attempting S-Type A");
					a = mergeStarNodes(rng.split("a"), a, toInsert, closeOrbit);
				} else if (!triedInnerB) {
					triedInnerB = true;
					Mod.LOGGER.info("Attempting S-Type B");
					b = mergeStarNodes(rng.split("b"), b, toInsert, closeOrbit);
				}

				if (a == null || b == null)
					return null;

				// final var newNode = new BinaryCelestialNode(a, b);
				final var newNode = BinaryCelestialNode.fromSquishFactor(a, b,
						existing.orbitalPlane,
						1, existing.orbitalShapeOuter.semiMajor(),
						existing.phase);
				return newNode;
			}
		}

		return null;
	}

	private @Nullable CelestialNode mergeStarNodes(StableRandom rng, CelestialNode existing,
			StellarCelestialNode toInsert, boolean closeOrbit) {

		if (existing instanceof StellarCelestialNode starNode) {
			return mergeSingleStar(rng, starNode, toInsert, closeOrbit);
		} else if (existing instanceof BinaryCelestialNode binaryNode) {
			return mergeStarWithBinary(rng, binaryNode, toInsert, closeOrbit);
		}

		Mod.LOGGER.error("tried to merge non-stellar nodes! " + existing + ", " + toInsert);

		return existing;
	}

	public static final double UNARY_ORBIT_THRESHOLD = 0.05;

	private CelestialNode convertBinaryOrbits(CelestialNode node) {
		for (var childOrbit : node.childNodes.iterable()) {
			convertBinaryOrbits(childOrbit.node);
		}

		if (node instanceof BinaryCelestialNode binaryNode) {
			binaryNode.setInner(convertBinaryOrbits(binaryNode.getInner()));
			binaryNode.setOuter(convertBinaryOrbits(binaryNode.getOuter()));
			if (UNARY_ORBIT_THRESHOLD * binaryNode.getInner().massYg > binaryNode.getOuter().massYg) {
				final var unary = new CelestialNodeChild<>(binaryNode.getInner(), binaryNode.getOuter(),
						binaryNode.orbitalShapeOuter, binaryNode.orbitalPlane, binaryNode.phase);
				binaryNode.getInner().insertChild(unary);

				for (final var child : binaryNode.childNodes.iterable()) {
					final var newChildInfo = new CelestialNodeChild<>(binaryNode.getInner(), child.node,
							child.orbitalShape,
							child.orbitalPlane, child.phase);
					binaryNode.getInner().insertChild(newChildInfo);
				}

				return binaryNode.getInner();
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
