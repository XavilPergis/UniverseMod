package net.xavil.ultraviolet.common.universe.system;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.StableRandom;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.math.Formulas;
import net.xavil.hawklib.math.OrbitalPlane;
import net.xavil.hawklib.math.OrbitalShape;
import net.xavil.ultraviolet.Mod;
import net.xavil.universegen.system.BinaryCelestialNode;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.CelestialNodeChild;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;

public final class BasicStarSystemGenerator implements StarSystemGenerator {

	// the maximum amount of attempts we should take to generate stars.
	private static final int STAR_ATTEMPT_COUNT = 256;
	private static final int PLANET_ATTEMPT_COUNT = 256;

	private static final double MINIMUM_STAR_MASS = 0.08 * Units.Yg_PER_Msol;

	// how many times larger the radius of an orbit around a binary pair needs to be
	// than the maximum radius of the existing binary pair.
	public static final double SPACING_FACTOR = 2;

	@Override
	public CelestialNode generate(Context ctx) {
		final var rootRng = new StableRandom(1);

		CelestialNode root = StellarCelestialNode.fromMassAndAge(ctx.info.massYg, ctx.info.systemAgeMyr);
		generatePlanets(ctx, rootRng.split("root_planets"), root, 1.0);

		double remainingMass = ctx.info.massYg * ctx.rng.lerpWeightedDouble(8.0, 0.0, 2.0);

		final var planetsRng = rootRng.split("planets");
		for (int i = 0; i < STAR_ATTEMPT_COUNT; ++i) {
			if (ctx.rng.chance(0.1))
				break;

			final var idealStarMass = ctx.rng.lerpWeightedDouble(3.0, MINIMUM_STAR_MASS, ctx.info.massYg);
			if (idealStarMass > remainingMass)
				continue;

			final var starNode = StellarCelestialNode.fromMassAndAge(idealStarMass, ctx.info.systemAgeMyr);
			generatePlanets(ctx, planetsRng.split(i), starNode, 1.0);
			final var newRoot = mergeStarNodes(ctx, root, starNode, ctx.rng.chance(0.8));

			// bail if we ran out of places to put new stars
			if (newRoot == null) {
				Mod.LOGGER.warn("could not place star #{} due to overcrowding!", i);
				break;
			}

			remainingMass -= idealStarMass;
			root = newRoot;
		}

		return postprocess(root);
	}

	private static double radiusFromMass(double mass) {
		final var mass_Mearth = Units.Mearth_PER_Yg * mass;
		if (mass_Mearth < 1)
			return Units.km_PER_Rearth * Math.pow(mass_Mearth, 0.3);
		if (mass_Mearth < 200)
			return Units.km_PER_Rearth * Math.pow(mass_Mearth, 0.5);
		return Units.km_PER_Rearth * 22.6 * Math.pow(mass_Mearth, -0.0886);
	}

	private CelestialNode generatePlanet(StableRandom rng, double mass) {
		final var primary = new PlanetaryCelestialNode();
		primary.obliquityAngle = rng.weightedDouble("obliquity_angle", 2, 0, 2 / Math.PI);
		// primary.apsidalRate
		// primary.rotationalRate
		primary.massYg = 1;
		primary.radius = radiusFromMass(primary.massYg);
		// primary.temperature
		// primary.type
		// primary.rings
		// primary.childNodes

		if (rng.chance("is_binary", 0.08)) {
			// final var secondary = generatePlanet(rng.split("binary_partner"));

			// secondary.massYg = primary.massYg * rng.uniformDouble("partner_mass_ratio", 0.05, 0.5);
			// secondary.radius = radiusFromMass(secondary.massYg);
			// // secondary.apsidalRate
			// // secondary.rotationalRate
			// // secondary.temperature
			// // secondary.type
			// // secondary.rings
			// // secondary.childNodes

			// final var bnode = new BinaryCelestialNode();
			// bnode.massYg = primary.massYg + secondary.massYg;
			// bnode.setSiblings(primary, secondary);
			// bnode.setOrbitalShapes(null);

			// fairly distant gas giants with their own moon systems
			// mid-range earth-like binaries
			// very close up low mass binaries (like pluto and charon)

			final var ascendingNode = rng.uniformDouble("longitude_of_ascending_node", 0, 2 * Math.PI);
			final var argPeriapsis = rng.uniformDouble("argument_of_periapsis", 0, 2 * Math.PI);
			final var inclination = rng.weightedDouble("inclination", 5, 0.0, 0.5 * Math.PI);
			final var orbitalPlane = OrbitalPlane.fromOrbitalElements(inclination, ascendingNode, argPeriapsis);

			// bnode.orbitalPlane
			// bnode.phase
			// bnode.childNodes
			// bnode.apsidalRate
			// return bnode;
		}

		return primary;
	}

	private static double discDensity(double starMass, double r) {
		// k -> scale factor
		// N -> peak distance
		final double k = 1.0, N = 1.0;
		// this gives lower densities closer to the star
		return k * Math.E * r / N * Math.exp(-r / N);
		// final double E = 0.077;
		// final double A = 5;
		// final double N = 3;
		// return E * Math.sqrt(Units.Msol_PER_Yg * starMass) * Math.exp(-A * Math.pow(d, 1 / N));
	}

	private static final int MASS_INTEGRATION_SUBDIVISIONS = 16;

	// very simplistic, considers
	private static double sweptMass(double starMass, double inner, double outer) {
		final var dr = (outer - inner) / MASS_INTEGRATION_SUBDIVISIONS;
		double sum = 0.0;
		// disc density is multiplied 
		double prev = inner * discDensity(starMass, inner);
		for (int i = 1; i <= MASS_INTEGRATION_SUBDIVISIONS; ++i) {
			final var r = Mth.lerp(i / (double) MASS_INTEGRATION_SUBDIVISIONS, inner, outer);
			final var cur = r * discDensity(starMass, r);
			sum += dr * (prev + cur / 2); // trapezoid sum
			prev = cur;
		}
		return Units.Yg_PER_Msol * 2 * Math.PI * sum;
	}

	private void generatePlanets(Context ctx, StableRandom rng, CelestialNode parent, double initialDiscMass) {
		// TODO: what is metallicity used for? I think some amount of metals are needed
		// to form actual planets, but im not sure how to work that in.
		final var systemMetallicity = ctx.galaxy.densityFields.metallicity.sample(ctx.info.systemPosTm);
		final var metallicityPlanetCountFactor = systemMetallicity / ctx.galaxy.info.maxMetallicity();

		// final var planetCount = metallicityPlanetCountFactor *
		// Mth.floor(ctx.rng.lerpWeightedDouble(4.0, 0.0, 30.0));

		// TODO: pick better minimum and maximum
		final var discMin = 0.0;
		final var discMax = Units.Tm_PER_au
				* rng.weightedDouble("disc_max", 2, 0, 100)
				* Math.sqrt(Units.Msol_PER_Yg * parent.massYg);

		double discMass = initialDiscMass;

		double currentSemiMajor = rng.weightedDouble("initial_semi_major", 5, discMin, discMax);
		for (int i = 0; i < PLANET_ATTEMPT_COUNT; ++i) {
			final var planetRng = rng.split(i);

			final var spacingFactor = planetRng.weightedDouble("spacing_factor", 2, 1, 1.5);
			final var ecc = planetRng.weightedDouble("eccentricity", 3);

			final var periapsis = currentSemiMajor * spacingFactor;
			final var semiMajor = periapsis / (1 - ecc);
			final var apoapsis = 2 * semiMajor - periapsis;

			if (apoapsis > discMax)
				break;

			final var distanceFromPrevious = apoapsis - currentSemiMajor;
			currentSemiMajor = 1.35 * apoapsis;

			final var orbitalShape = new OrbitalShape(ecc, semiMajor);

			final var ascendingNode = rng.uniformDouble("longitude_of_ascending_node", 0, 2 * Math.PI);
			final var argPeriapsis = rng.uniformDouble("argument_of_periapsis", 0, 2 * Math.PI);
			final var inclination = rng.weightedDouble("inclination", 5, 0.0, 0.5 * Math.PI);
			final var orbitalPlane = OrbitalPlane.fromOrbitalElements(inclination, ascendingNode, argPeriapsis);

			// fixed mass;
			// {
			// const fixed a = pos;
			// const fixed b = fixed(135, 100) * apoapsis;
			// mass = mass_from_disk_area(a, b, discMax);
			// mass *= rand.Fixed() * discDensity;
			// }
			// if (mass < 0) { // hack around overflow
			// Output("WARNING: planetary mass has overflowed! (child of %s)\n",
			// primary->GetName().c_str());
			// mass = fixed(Sint64(0x7fFFffFFffFFffFFull));
			// }
			// assert(mass >= 0);

			// final var node = generatePlanet(rng.split("planet"));

			// final var phase = rng.uniformDouble("phase", 0, 2 * Math.PI);
			// final var child = new CelestialNodeChild<>(parent, node, orbitalShape, orbitalPlane, phase);
			// parent.childNodes.push(child);

		}
	}

	private static void replaceNode(CelestialNode existing, BinaryCelestialNode newNode) {
		// set up backlinks
		final var parent = existing.getBinaryParent();
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

	@Nullable
	private CelestialNode mergeSingleStar(Context ctx, StellarCelestialNode existing, StellarCelestialNode toInsert,
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
		OrbitalPlane orbitalPlane;
		orbitalPlane = OrbitalPlane.fromInclination(ctx.rng.uniformDouble(0, 2.0 * Math.PI), ctx.rng);
		if (closeOrbit) {
			final var limit = Units.Tm_PER_au * 10;
			distance = ctx.rng.lerpWeightedDouble(3.0, minDistance, Math.min(maxDistance, limit));
			// orbitalPlane = OrbitalPlane.ZERO;
		} else {
			distance = ctx.rng.uniformDouble(minDistance, maxDistance);
		}
		Mod.LOGGER.info("Success [distance={}]", distance);

		final var phase = ctx.rng.uniformDouble(0, 2 * Math.PI);
		final var newNode = BinaryCelestialNode.fromSquishFactor(existing, toInsert,
				orbitalPlane, 1, distance, phase);
		replaceNode(existing, newNode);
		return newNode;
	}

	/**
	 * @param node The node to query.
	 * @return The minimum distance that other objects may be placed from this node.
	 */
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
		double maxDistance = Units.Tm_PER_au * 1000;

		final var binaryParent = node.getBinaryParent();
		if (binaryParent != null) {
			final var closestDistance = binaryParent.orbitalShapeOuter.periapsisDistance();
			maxDistance = Math.min(maxDistance, closestDistance);
		}

		return maxDistance / SPACING_FACTOR;
	}

	@Nullable
	private CelestialNode mergeStarWithBinary(Context ctx, BinaryCelestialNode existing, StellarCelestialNode toInsert,
			boolean closeOrbit) {

		// i kinda hate this, but i cant think of a nicer way to do this rn.
		boolean triedOuter = false, triedInnerA = false, triedInnerB = false;
		while (!triedOuter || !triedInnerA || !triedInnerB) {

			if (!triedOuter && (triedInnerA && triedInnerB || ctx.rng.chance(0.3))) {
				triedOuter = true;

				// We want to avoid putting nodes into P-type orbits that are too close to their
				// partner, as these types of configurations are usually very unstable in real
				// life.
				final var minRadius = Math.max(getExclusionRadius(existing), getExclusionRadius(toInsert));
				final var maxRadius = getMaximumBinaryDistanceForReplacement(existing);

				Mod.LOGGER.info("Attempting P-Type [min={}, max={}]", minRadius, maxRadius);

				if (minRadius <= maxRadius) {
					final var radius = ctx.rng.lerpWeightedDouble(2.0, minRadius, maxRadius);
					Mod.LOGGER.info("Success [radius={}]", radius);

					OrbitalPlane orbitalPlane;
					if (closeOrbit) {
						orbitalPlane = existing.orbitalPlane;
						// existing.orbitalPlane = OrbitalPlane.ZERO;
					} else {
						orbitalPlane = OrbitalPlane.fromInclination(ctx.rng.uniformDouble(0, 2.0 * Math.PI), ctx.rng);
					}

					final var squishFactor = 1;
					final var newNode = BinaryCelestialNode.fromSquishFactor(existing, toInsert, orbitalPlane,
							squishFactor, radius,
							ctx.rng.uniformDouble(0, 2 * Math.PI));
					return newNode;
				}
			} else {
				var a = existing.getInner();
				var b = existing.getOuter();

				if (!triedInnerA && ctx.rng.chance(0.5)) {
					triedInnerA = true;
					Mod.LOGGER.info("Attempting S-Type A");
					a = mergeStarNodes(ctx, a, toInsert, closeOrbit);
				} else if (!triedInnerB) {
					triedInnerB = true;
					Mod.LOGGER.info("Attempting S-Type B");
					b = mergeStarNodes(ctx, b, toInsert, closeOrbit);
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

		Mod.LOGGER.info("Attempt Failed");
		return null;
	}

	private @Nullable CelestialNode mergeStarNodes(Context ctx, CelestialNode existing, StellarCelestialNode toInsert,
			boolean closeOrbit) {

		if (existing instanceof StellarCelestialNode starNode) {
			return mergeSingleStar(ctx, starNode, toInsert, closeOrbit);
		} else if (existing instanceof BinaryCelestialNode binaryNode) {
			return mergeStarWithBinary(ctx, binaryNode, toInsert, closeOrbit);
		}

		Mod.LOGGER.error("tried to merge non-stellar nodes! " + existing + ", " + toInsert);

		return existing;
	}

	// mass ratio at which a binary orbit is converted to a unary orbit.
	public static final double UNARY_ORBIT_THRESHOLD = 0.05;

	private CelestialNode postprocess(CelestialNode node) {
		final var newChildren = node.childNodes.iter().map(childOrbit -> {
			final var newChild = postprocess(childOrbit.node);
			return new CelestialNodeChild<CelestialNode>(node, newChild,
					childOrbit.orbitalShape, childOrbit.orbitalPlane, childOrbit.phase);
		}).collectTo(Vector::new);
		node.childNodes.clear();
		node.childNodes.extend(newChildren);

		// convert binaries that have a large enough mass discrepancy into a unary
		// configuration
		if (node instanceof BinaryCelestialNode binaryNode) {
			final var a = postprocess(binaryNode.getInner());
			binaryNode.setInner(a);
			final var b = postprocess(binaryNode.getOuter());
			binaryNode.setOuter(b);
			if (UNARY_ORBIT_THRESHOLD * a.massYg > b.massYg) {
				a.insertChild(new CelestialNodeChild<>(a, b,
						binaryNode.orbitalShapeOuter, binaryNode.orbitalPlane, binaryNode.phase));
				a.childNodes.extend(binaryNode.childNodes.iter()
						.map(child -> new CelestialNodeChild<>(a, child.node,
								child.orbitalShape, child.orbitalPlane, child.phase)));
				return a;
			}
		}

		return node;
	}

}
