package net.xavil.ultraviolet.common.universe.system;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.math.Formulas;
import net.xavil.hawklib.math.OrbitalPlane;
import net.xavil.hawklib.math.OrbitalShape;
import net.xavil.universegen.system.BinaryCelestialNode;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.CelestialNodeChild;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.universegen.system.UnaryCelestialNode;

public final class BasicStarSystemGenerator implements StarSystemGenerator {

	// the maximum amount of attempts we should take to generate stars.
	private static final int STAR_ATTEMPT_COUNT = 256;
	private static final int PLANET_ATTEMPT_COUNT = 256;

	private static final double MINIMUM_STAR_MASS = 0.08 * Units.Yg_PER_Msol;

	// how many times larger the radius of an orbit around a binary pair needs to be
	// than the maximum radius of the existing binary pair.
	public static final double SPACING_FACTOR = 4;

	private final SplittableRng rng = new SplittableRng(0);
	private Logger logger;
	private CelestialNode root;

	public BasicStarSystemGenerator(CelestialNode root) {
		this.root = root;
	}

	@Override
	public CelestialNode generate(Context ctx) {
		this.logger = LoggerFactory.getLogger(String.format("SystemGenerator '%s'", ctx.systemId().toString()));

		this.rng.setSeed(ctx.seed);

		double remainingMass = ctx.info.massYg * this.rng.weightedDouble("remaining_mass", 8.0, 0.0, 2.0);

		this.rng.push("generate_secondary_stars");
		for (int i = 0; i < STAR_ATTEMPT_COUNT; ++i) {
			this.rng.advance();

			if (this.rng.chance("stop_iteration", 0.1))
				break;

			final var idealStarMass = this.rng.weightedDouble("star_mass", 3.0, MINIMUM_STAR_MASS, ctx.info.massYg);
			if (idealStarMass > remainingMass)
				continue;

			this.rng.push("star");
			final var starNode = StellarCelestialNode.fromMassAndAge(this.rng, idealStarMass, ctx.info.systemAgeMyr);
			this.rng.pop();

			// this.rng.push("generate_secondary_planets");
			// generatePlanetsAroundStar(ctx, starNode, 1.0);
			// this.rng.pop();

			final var newRoot = mergeStarNodes(ctx, this.root, starNode, this.rng.chance("close_orbit", 0.8));

			// bail if we ran out of places to put new stars
			if (newRoot == null) {
				this.logger.warn("could not place star #{} due to overcrowding!", i);
				break;
			}

			remainingMass -= idealStarMass;
			this.root = newRoot;
		}
		this.rng.pop();

		this.rng.push("generate_planets");
		generatePlanetsRecursively(ctx, this.root, Double.POSITIVE_INFINITY);
		this.rng.pop();

		return postprocess(this.root);
	}

	private void generatePlanetsRecursively(Context ctx, CelestialNode node, double maxRadius) {
		if (node instanceof BinaryCelestialNode binaryNode) {
			final var limit = binaryNode.orbitalShapeOuter.periapsisDistance() / SPACING_FACTOR;
			this.rng.push("generate_inner");
			generatePlanetsRecursively(ctx, binaryNode.inner, limit);
			this.rng.pop();
			this.rng.push("generate_outer");
			generatePlanetsRecursively(ctx, binaryNode.outer, limit);
			this.rng.pop();
		}

		this.rng.push("generate_children");
		for (int i = 0; i < node.childNodes.size(); ++i) {
			this.rng.advance();
			final var child = node.childNodes.get(i);
			final double innerLimit;
			if (i == 0) {
				innerLimit = child.orbitalShape.periapsisDistance() / SPACING_FACTOR;
			} else {
				final var prevChild = node.childNodes.get(i - 1);
				innerLimit = (child.orbitalShape.periapsisDistance() - prevChild.orbitalShape.apoapsisDistance())
						/ SPACING_FACTOR;
			}
			final double outerLimit;
			if (i == node.childNodes.size() - 1) {
				outerLimit = (maxRadius - child.orbitalShape.apoapsisDistance()) / SPACING_FACTOR;
			} else {
				final var nextChild = node.childNodes.get(i - 1);
				outerLimit = (nextChild.orbitalShape.apoapsisDistance() - child.orbitalShape.periapsisDistance())
						/ SPACING_FACTOR;
			}
			generatePlanetsRecursively(ctx, child.node, Math.min(innerLimit, outerLimit));
		}
		this.rng.pop();

		this.rng.push("generate_children");
		generatePlanetsAroundStar(ctx, node, 1.0, maxRadius);
		this.rng.pop();
	}

	private static double radiusFromMass(double mass) {
		final var mass_Mearth = Units.Mearth_PER_Yg * mass;
		if (mass_Mearth < 1)
			return Units.km_PER_Rearth * Math.pow(mass_Mearth, 0.3);
		if (mass_Mearth < 200)
			return Units.km_PER_Rearth * Math.pow(mass_Mearth, 0.5);
		return Units.km_PER_Rearth * 22.6 * Math.pow(mass_Mearth, -0.0886);
	}

	private CelestialNode generatePlanet(double mass, double maxDistance) {
		Assert.isTrue(!Double.isNaN(maxDistance));
		final CelestialNode res;
		if (mass * Units.Mearth_PER_Yg > 1e-6 && rng.chance("is_binary", 0.1)) {
			final var massRatio = this.rng.weightedDouble("partner_mass_ratio", 0.6, 0.5, 0.9);

			final var primaryMass = massRatio * mass;
			final var secondaryMass = (1 - massRatio) * mass;

			this.rng.push("primary");
			final var primary = generatePlanet(primaryMass, maxDistance);
			this.rng.pop();

			final var semiMajorL = SPACING_FACTOR * getExclusionRadius(primary);
			// higher bound increases by 0.02 au per earth mass
			final var semiMajorH = 0.02 * primaryMass * Units.Mearth_PER_Yg * Units.Tm_PER_au;

			final var eccentricity = rng.uniformDouble("eccentricity", 0.0, 0.1);
			final var distance = rng.uniformDouble("semi_major", semiMajorL, semiMajorH);

			if (distance > maxDistance)
				return primary;

			final var outerShape = new OrbitalShape(eccentricity, distance / (1 + eccentricity));
			final var innerShape = new OrbitalShape(eccentricity,
					outerShape.semiMajor() * (secondaryMass / primaryMass));

			Assert.isTrue(!Double.isNaN(outerShape.semiMajor()));
			Assert.isTrue(!Double.isNaN(innerShape.semiMajor()));

			final var closestDistance = outerShape.periapsisDistance() - innerShape.periapsisDistance();

			this.rng.push("secondary");
			final var secondary = generatePlanet(secondaryMass, closestDistance / SPACING_FACTOR);
			this.rng.pop();

			final var node = new BinaryCelestialNode();
			node.massYg = primary.massYg + secondary.massYg;
			node.setSiblings(primary, secondary);

			node.orbitalShapeInner = innerShape;
			node.orbitalShapeOuter = outerShape;

			// fairly distant gas giants with their own moon systems
			// mid-range earth-like binaries
			// very close up low mass binaries (like pluto and charon)

			final var ascendingNode = rng.uniformDouble("longitude_of_ascending_node", 0, 2 * Math.PI);
			final var argPeriapsis = rng.uniformDouble("argument_of_periapsis", 0, 2 * Math.PI);
			final var inclination = rng.weightedDouble("inclination", 5, 0.0, 0.05 * Math.PI);
			final var orbitalPlane = OrbitalPlane.fromOrbitalElements(inclination, ascendingNode, argPeriapsis);

			node.orbitalPlane = orbitalPlane;
			node.phase = rng.uniformDouble("phase", 0, 2 * Math.PI);
			// bnode.apsidalRate
			res = node;
		} else {
			final var node = new PlanetaryCelestialNode();
			node.obliquityAngle = rng.weightedDouble("obliquity_angle", 2, 0, 2 / Math.PI);
			// node.apsidalRate
			node.rotationalRate = rng.uniformDouble("rotational_rate", 0, 0.000872664626);
			node.massYg = mass;
			node.radius = radiusFromMass(node.massYg);
			// node.temperature

			node.type = PlanetaryCelestialNode.Type.ROCKY_WORLD;

			// node.rings
			// node.childNodes

			node.radius = radiusFromMass(node.massYg);
			node.type = PlanetaryCelestialNode.Type.ROCKY_WORLD;

			// idk lol, should probably take the escape velocity of gasses into account
			double gasGiantChance = Mth.inverseLerp(node.massYg, 5.0 * Units.Yg_PER_Mearth, 20.0 * Units.Yg_PER_Mearth);
			gasGiantChance = Mth.clamp(gasGiantChance, 0, 1);
			if (this.rng.chance("gas_giant_chance", gasGiantChance)) {
				node.type = PlanetaryCelestialNode.Type.GAS_GIANT;
			}
			if (node.massYg > 13 * Units.Yg_PER_Mjupiter) {
				node.type = PlanetaryCelestialNode.Type.BROWN_DWARF;
			}

			res = node;
		}

		if (res.massYg > 0.01 * Units.Yg_PER_Mearth) {
			this.rng.push("moons");
			generateMoonsAroundPlanet(res, maxDistance);
			this.rng.pop();
		}

		return res;
	}

	private static double discDensity(double cutoutDistance, double massFactor, double r) {
		// inflection point
		final var k = cutoutDistance;
		// steepness
		final var n = 100;

		// mask values apst this will be treated as 1, used to avoid generating
		// intermediate infinities
		final var L = 0.999;

		final var kn = k * n;

		// the values of d at which `mask(d) = L` and `mask(d) = 1-L` respectively
		// x=k\sqrt[kn]{\frac{R}{\left(1-R\right)}}
		final var maskLimitH = k * Math.pow((1 - L) / L, 1 / kn);
		// substitute L for 1-L in previous equation
		final var maskLimitL = k * Math.pow(L / (1 - L), 1 / kn);
		final double mask;
		if (r <= maskLimitL) {
			mask = 0;
		} else if (r >= maskLimitH) {
			mask = 1;
		} else {
			// m\left(x\right)=\frac{x^{kn}}{k^{kn}+x^{kn}}\left\{x\ge0\right\}
			mask = Math.pow(r, kn) / (Math.pow(k, kn) + Math.pow(r, kn));
		}

		final double E = 0.2;
		final double a = 5;
		final double N = 3;

		// f\left(x\right)=Ee^{-ax^{\frac{1}{N}}}\left\{x\ge0\right\}
		final var density = E * massFactor
				* Math.exp(-a * Math.pow(r, 1 / N));

		return mask * density;
	}

	private static final int MASS_INTEGRATION_SUBDIVISIONS = 16;

	// very simplistic, considers
	private static double sweptMass(double cutoutDistance, double massFactor, double inner, double outer) {
		final var dr = (outer - inner) / MASS_INTEGRATION_SUBDIVISIONS;
		double sum = 0.0;
		double prev = inner * discDensity(cutoutDistance, massFactor, inner);
		for (int i = 1; i <= MASS_INTEGRATION_SUBDIVISIONS; ++i) {
			final var r = Mth.lerp(i / (double) MASS_INTEGRATION_SUBDIVISIONS, inner, outer);
			final var cur = r * discDensity(cutoutDistance, massFactor, r);
			sum += dr * (prev + cur / 2); // trapezoid sum
			prev = cur;
		}
		return Units.Yg_PER_Msol * 2 * Math.PI * sum;
	}

	private double totalStellarMass(CelestialNode node) {
		if (node instanceof BinaryCelestialNode binaryNode) {
			return totalStellarMass(binaryNode.inner) + totalStellarMass(binaryNode.outer);
		}
		// if (node instanceof StellarCelestialNode starNode) {
		// }
		return node.massYg;
		// throw new IllegalArgumentException("node was not stellar or binary!");
	}

	private void generateMoonsAroundPlanet(CelestialNode planet, double maxDistance) {

		// final var stellarMass = totalStellarMass(parent);

		// TODO: pick better minimum and maximum
		final var discMin = getExclusionRadius(planet);
		// final var discMax = Units.Tm_PER_au
		// * this.rng.weightedDouble("disc_max", 2, 0.4, 1)
		// * Math.sqrt(Units.Msol_PER_Yg * planet.massYg);
		final var discMax = this.rng.weightedDouble("disc_max", 2, 0.4, 1) * maxDistance;

		double discMass = planet.massYg * this.rng.weightedDouble("disc_mass", 2.0, 0.0, 0.7);
		final var discPlane = OrbitalPlane.random(this.rng.rng("disc_plane"));

		double currentSemiMajor = rng.weightedDouble("initial_semi_major", 5, discMin, discMax);
		this.rng.push("generation_attempts");
		for (int i = 0; i < PLANET_ATTEMPT_COUNT; ++i) {
			this.rng.advance();

			Assert.isTrue(!Double.isNaN(currentSemiMajor));

			final var spacingFactor = this.rng.weightedDouble("spacing_factor", 3, 1, 1.5);
			final var ecc = this.rng.weightedDouble("eccentricity", 10, 0.0, 0.3);

			final var periapsis = currentSemiMajor * spacingFactor;
			final var semiMajor = periapsis / (1 - ecc);
			final var apoapsis = 2 * semiMajor - periapsis;
			currentSemiMajor = 1.25 * apoapsis;

			Assert.isTrue(!Double.isNaN(semiMajor));

			if (apoapsis > discMax)
				break;

			final var orbitalShape = new OrbitalShape(ecc, semiMajor);

			final var ascendingNode = this.rng.uniformDouble("longitude_of_ascending_node", 0, 2 * Math.PI);
			final var argPeriapsis = this.rng.uniformDouble("argument_of_periapsis", 0, 2 * Math.PI);
			final var inclination = this.rng.weightedDouble("inclination", 5, 0.0, 0.05 * Math.PI);
			final var orbitalPlane = OrbitalPlane.fromOrbitalElements(inclination, ascendingNode, argPeriapsis)
					.withReferencePlane(discPlane);

			final var planetMass = this.rng.weightedDouble("mass", 3.0, 0.0, 0.001 * discMass);
			Assert.isTrue(!Double.isNaN(planetMass));

			final var phase = rng.uniformDouble("phase", 0, 2 * Math.PI);
			this.rng.push("planet");
			if (planetMass > Units.Yg_PER_Mearth * 0.001) {
				final var childMaxDistance = Math.min(
						Formulas.hillSphereRadius(planet.massYg, planetMass, ecc, semiMajor),
						(apoapsis - periapsis) / (4.0 * SPACING_FACTOR));
				final var node = generatePlanet(planetMass, childMaxDistance);
				final var child = new CelestialNodeChild<>(planet, node, orbitalShape, orbitalPlane, phase);
				planet.childNodes.push(child);
			}
			this.rng.pop();
		}
		this.rng.pop();
	}

	private double resonantSemiMajor(double semiMajor, double ratio) {
		// a1, R in [0, 1] -> a2
		// T1 = sqrt(k*a1^3)
		// T2 = T1 / R
		// a2 = cbrt(T2^2 / k)

		// a2 = cbrt((sqrt(k * a1^3) / R)^2 / k)
		// a2 = cbrt(a1^3 / R^2)
		// a2 = (a1^3 / R^2)^(1/3)
		// a2 = a1 / R^(2/3)
		return semiMajor * Math.pow(ratio, (2.0 / 3.0));
	}

	private void generatePlanetsAroundStar(Context ctx, CelestialNode parent, double initialDiscMass,
			double maxDistance) {
		// TODO: what is metallicity used for? I think some amount of metals are needed
		// to form actual planets, but im not sure how to work that in.
		final var systemMetallicity = ctx.galaxy.densityFields.metallicity.sample(ctx.info.systemPosTm);
		final var metallicityFactor = systemMetallicity / ctx.galaxy.info.maxMetallicity();

		final var maxPlanetCount = Mth.floor(this.rng.weightedDouble("max_planets", 2.0, 0.0, 100.0));

		final var cutoutDistance = Math.sqrt(Units.Msol_PER_Yg * totalStellarMass(parent));

		// TODO: pick better minimum and maximum
		final var discMin = Math.max(0.02, getExclusionRadius(parent));
		double discMax = Units.Tm_PER_au
				* this.rng.weightedDouble("disc_max", 1, 0, 100)
				* Math.sqrt(Units.Msol_PER_Yg * parent.massYg);
		discMax = Math.min(discMax, maxDistance);

		// double discMass = initialDiscMass;
		double startingDiscMass = parent.massYg * this.rng.weightedDouble("disc_mass", 2.0, 0.0, 0.01);
		double discMass = startingDiscMass;

		final var discPlane = OrbitalPlane.random(this.rng.rng("disc_plane"));

		double currentSemiMajor = rng.weightedDouble("initial_semi_major", 5, discMin, discMax);
		this.rng.push("generation_attempts");
		for (int i = 0; i < PLANET_ATTEMPT_COUNT; ++i) {
			this.rng.advance();

			if (i >= maxPlanetCount)
				break;

			final var spacingFactor = this.rng.weightedDouble("spacing_factor", 2, 1, 1.5);
			// final var ecc = this.rng.weightedDouble("eccentricity", 5);
			final var ecc = 0.0;
			final var inclination1 = this.rng.weightedDouble("inclination1", 5, 0.0, 0.05 * Math.PI);
			final var inclination2 = this.rng.uniformDouble("inclination2", 0.2 * Math.PI, 0.5 * Math.PI);
			final var inclination = Mth.lerp(ecc, inclination1, inclination2);

			final var prevBound = currentSemiMajor;
			final var periapsis = currentSemiMajor * spacingFactor;
			final var semiMajor = periapsis / (1 - ecc);
			final var apoapsis = 2 * semiMajor - periapsis; // * Mth.lerp(Math.cos(inclination), 0.2, 1.0);
			currentSemiMajor = 1.35 * apoapsis;
			final var curBound = currentSemiMajor;

			if (apoapsis > discMax)
				break;

			final var orbitalShape = new OrbitalShape(ecc, semiMajor);

			final var ascendingNode = this.rng.uniformDouble("longitude_of_ascending_node", 0, 2 * Math.PI);
			final var argPeriapsis = this.rng.uniformDouble("argument_of_periapsis", 0, 2 * Math.PI);
			final var orbitalPlane = OrbitalPlane.fromOrbitalElements(inclination, ascendingNode, argPeriapsis)
					.withReferencePlane(discPlane);

			final var mf = Mth.lerp(metallicityFactor, 5.0, 1.0);
			double planetMass = 0;
			for (int j = 0; j < 16; ++j) {
				this.rng.push("mass");
				planetMass = this.rng.weightedDouble(j, mf, 0.0, 0.05 * Units.Yg_PER_Msol);
				this.rng.pop();
				if (planetMass <= discMass)
					break;
			}

			if (planetMass > discMass)
				break;
			discMass -= planetMass;

			// final var planetMass = this.rng.uniformDouble("mass", 0.5, 1.0)
			// * sweptMass(cutoutDistance, discMass, periapsis, apoapsis);
			final var phase = rng.uniformDouble("phase", 0, 2 * Math.PI);

			this.rng.push("planet");
			if (planetMass > Units.Yg_PER_Mearth * 0.01) {
				final var childMaxDistance = Math.min(
						Formulas.hillSphereRadius(parent.massYg, planetMass, ecc, semiMajor),
						(curBound - prevBound) / (4.0 * SPACING_FACTOR));
				final var node = generatePlanet(planetMass, childMaxDistance);
				final var child = new CelestialNodeChild<>(parent, node, orbitalShape, orbitalPlane, phase);
				parent.childNodes.push(child);
			}
			this.rng.pop();

		}
		this.rng.pop();

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
		this.logger.info("Attempting Single [min={}, max={}]", minDistance, maxDistance);

		// If there is no place that an orbit can be inserted, signal that to the
		// caller.
		if (minDistance > maxDistance)
			return null;

		this.rng.push("merge_single_star");

		double distance = 0;
		OrbitalPlane orbitalPlane = OrbitalPlane.random(this.rng.rng("orbital_plane"));
		if (closeOrbit) {
			double limit = Units.Tm_PER_au * 1;
			if (!this.rng.chance("very_close", 0.6)) {
				limit = Units.Tm_PER_au * 10;
			}
			limit = Math.min(maxDistance, limit);
			if (limit > minDistance)
				distance = this.rng.weightedDouble("distance", 3.0, minDistance, limit);
			// orbitalPlane = OrbitalPlane.ZERO;
		} else {
			distance = this.rng.weightedDouble("distance", 3.0, maxDistance, minDistance);
		}
		this.logger.info("Success [distance={}]", distance);
		final var phase = this.rng.uniformDouble("phase", 0, 2 * Math.PI);

		final double squishFactor = 1.0;

		CelestialNode res = null;
		if (UNARY_ORBIT_THRESHOLD * existing.massYg > toInsert.massYg) {
			final var orbitalShape = OrbitalShape.fromAxes(distance, squishFactor * distance);
			final var child = new CelestialNodeChild<>(existing, toInsert, orbitalShape, orbitalPlane, phase);
			existing.insertChild(child);
		} else if (UNARY_ORBIT_THRESHOLD * toInsert.massYg > existing.massYg) {
			final var orbitalShape = OrbitalShape.fromAxes(distance, squishFactor * distance);
			final var child = new CelestialNodeChild<>(toInsert, existing, orbitalShape, orbitalPlane, phase);
			toInsert.insertChild(child);
		} else {
			final var bnode = BinaryCelestialNode.fromSquishFactor(existing, toInsert,
					orbitalPlane, squishFactor, distance, phase);
			replaceNode(existing, bnode);
			res = bnode;
		}

		this.rng.pop();
		return res;
	}

	/**
	 * @param node The node to query.
	 * @return The minimum distance that other objects may be placed from this node.
	 */
	private static double getExclusionRadius(CelestialNode node) {
		if (node instanceof UnaryCelestialNode unaryNode) {
			return SPACING_FACTOR * Units.Tu_PER_ku * unaryNode.radius;
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

		CelestialNode res = null;

		this.rng.push("merge_binary");

		// i kinda hate this, but i cant think of a nicer way to do this rn.
		boolean triedOuter = false, triedInnerA = false, triedInnerB = false;
		while (!triedOuter || !triedInnerA || !triedInnerB) {
			this.rng.advance();

			if (!triedOuter && (triedInnerA && triedInnerB || this.rng.chance("outer_chance", 0.3))) {
				triedOuter = true;

				// We want to avoid putting nodes into P-type orbits that are too close to their
				// partner, as these types of configurations are usually very unstable in real
				// life.
				final var minRadius = Math.max(getExclusionRadius(existing), getExclusionRadius(toInsert));
				final var maxRadius = getMaximumBinaryDistanceForReplacement(existing);

				this.logger.info("Attempting P-Type [min={}, max={}]", minRadius, maxRadius);

				if (minRadius <= maxRadius) {
					final var radius = this.rng.weightedDouble("radius", 2.0, minRadius, maxRadius);
					this.logger.info("Success [radius={}]", radius);

					OrbitalPlane orbitalPlane;
					if (closeOrbit) {
						orbitalPlane = existing.orbitalPlane;
						// existing.orbitalPlane = OrbitalPlane.ZERO;
					} else {
						orbitalPlane = OrbitalPlane.random(this.rng.rng("orbital_plane"));
					}

					final var squishFactor = 1;
					final var newNode = BinaryCelestialNode.fromSquishFactor(existing, toInsert, orbitalPlane,
							squishFactor, radius,
							this.rng.uniformDouble("phase", 0, 2 * Math.PI));
					res = newNode;
					break;
				}
			} else {
				var a = existing.getInner();
				var b = existing.getOuter();

				if (!triedInnerA && this.rng.chance("a_or_b", 0.5)) {
					triedInnerA = true;
					this.logger.info("Attempting S-Type A");
					a = mergeStarNodes(ctx, a, toInsert, closeOrbit);
				} else if (!triedInnerB) {
					triedInnerB = true;
					this.logger.info("Attempting S-Type B");
					b = mergeStarNodes(ctx, b, toInsert, closeOrbit);
				}

				if (a == null || b == null)
					return null;

				// final var newNode = new BinaryCelestialNode(a, b);
				final var newNode = BinaryCelestialNode.fromSquishFactor(a, b,
						existing.orbitalPlane,
						1, existing.orbitalShapeOuter.semiMajor(),
						existing.phase);
				res = newNode;
				break;
			}
		}

		this.rng.pop();

		if (res == null)
			this.logger.info("Attempt Failed");
		return res;
	}

	private @Nullable CelestialNode mergeStarNodes(Context ctx, CelestialNode existing, StellarCelestialNode toInsert,
			boolean closeOrbit) {

		if (existing instanceof StellarCelestialNode starNode) {
			return mergeSingleStar(ctx, starNode, toInsert, closeOrbit);
		} else if (existing instanceof BinaryCelestialNode binaryNode) {
			return mergeStarWithBinary(ctx, binaryNode, toInsert, closeOrbit);
		}

		this.logger.error("tried to merge non-stellar nodes! " + existing + ", " + toInsert);

		return existing;
	}

	// mass ratio at which a binary orbit is converted to a unary orbit.
	public static final double UNARY_ORBIT_THRESHOLD = 0.05;

	private CelestialNode postprocess(CelestialNode node) {

		final var newChildren = node.childNodes.iter().map(childOrbit -> {
			final var newChild = postprocess(childOrbit.node);
			if (newChild == null)
				return null;
			return new CelestialNodeChild<CelestialNode>(node, newChild,
					childOrbit.orbitalShape, childOrbit.orbitalPlane, childOrbit.phase);
		}).filterNull().collectTo(Vector::new);
		node.childNodes.clear();
		node.childNodes.extend(newChildren);

		// convert binaries that have a large enough mass discrepancy into a unary
		// configuration
		if (node instanceof BinaryCelestialNode binaryNode) {
			final var a = postprocess(binaryNode.getInner());
			binaryNode.setInner(a);
			final var b = postprocess(binaryNode.getOuter());
			binaryNode.setOuter(b);
			if (a.massYg < 0 && UNARY_ORBIT_THRESHOLD * a.massYg > b.massYg) {
				a.insertChild(new CelestialNodeChild<>(a, b,
						binaryNode.orbitalShapeOuter, binaryNode.orbitalPlane, binaryNode.phase));
				a.childNodes.extend(binaryNode.childNodes.iter()
						.map(child -> new CelestialNodeChild<>(a, child.node,
								child.orbitalShape, child.orbitalPlane, child.phase)));
				return a;
			}
		}

		// TODO: remove sattelites that are too close to/inside parent body

		return node;
	}

}
