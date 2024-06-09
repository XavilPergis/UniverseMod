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
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.OrbitalPlane;
import net.xavil.hawklib.math.OrbitalShape;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;

// not physically-based at all
public final class BasicStarSystemGenerator implements StarSystemGenerator {

	public static final Logger LOGGER = LoggerFactory.getLogger("BasicStarSystemGenerator");

	// the maximum amount of attempts we should take to generate stars.
	private static final int STAR_ATTEMPT_COUNT = 256;
	private static final int PLANET_ATTEMPT_COUNT = 256;

	private static final double MINIMUM_STAR_MASS = 0.08 * Units.Yg_PER_Msol;

	// how many times larger the radius of an orbit around a binary pair needs to be
	// than the maximum radius of the existing binary pair.
	public static final double SPACING_FACTOR = 6;

	private final SplittableRng rng = new SplittableRng(0);
	private CelestialNode root;
	private double systemMetallicity;

	public BasicStarSystemGenerator(CelestialNode root) {
		this.root = root;
	}

	@Override
	public CelestialNode generate(Context ctx) {
		this.rng.setCurrent(ctx.seed);

		// FIXME: real metallicity
		// this.systemMetallicity =
		// ctx.galaxy.parameters.metallicity.sample(ctx.info.systemPosTm);
		this.systemMetallicity = this.rng.uniformDouble("metallicity", Galaxy.METALLICITY_RANGE);

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
			final var starProps = new StellarProperties();
			starProps.load(idealStarMass, ctx.info.systemAgeMyr, 1.42857e-02);
			final var starNode = new StellarCelestialNode();
			starNode.type = StellarCelestialNode.Type.STAR;
			starNode.massYg = starProps.massYg;
			starNode.luminosityLsol = starProps.luminosityLsol;
			starNode.radius = Units.km_PER_Rsol * starProps.radiusRsol;
			starNode.temperature = starProps.temperatureK;
			// final var starNode = StellarCelestialNode.fromMassAndAge(this.rng, idealStarMass, ctx.info.systemAgeMyr);
			this.rng.pop();

			// this.rng.push("generate_secondary_planets");
			// generatePlanetsAroundStar(ctx, starNode, 1.0);
			// this.rng.pop();

			final var newRoot = mergeStarNodes(ctx, this.root, starNode, this.rng.chance("close_orbit", 0.8));

			// bail if we ran out of places to put new stars
			if (newRoot == null) {
				LOGGER.warn("could not place star #{} due to overcrowding!", i);
				break;
			}

			remainingMass -= idealStarMass;
			this.root = newRoot;
		}
		this.rng.pop();

		this.rng.push("generate_planets");
		generatePlanetsRecursively(ctx, this.root, Double.POSITIVE_INFINITY);
		this.rng.pop();

		// return postprocess(this.root);
		return this.root;
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
		// generatePlanetsAroundStar(ctx, node, 1.0, maxRadius);
		generatePlanets(ctx, node, maxRadius);
		this.rng.pop();
	}

	// TODO: generate density for unary nodes and use that to derive radius
	private static double radiusFromMass(double mass) {
		final var mass_Mearth = Units.Mearth_PER_Yg * mass;
		if (mass_Mearth < 1)
			return Units.km_PER_Rearth * Math.pow(mass_Mearth, 0.3);
		if (mass_Mearth < 200)
			return Units.km_PER_Rearth * Math.pow(mass_Mearth, 0.5);
		return Units.km_PER_Rearth * 22.6 * Math.pow(mass_Mearth, -0.0886);
	}

	private Interval pickOrbitBounds(CelestialNode node) {
		final var massDistanceFactor = Units.Tm_PER_au * Math.pow(Units.Msol_PER_Yg * node.massYg, 1.0 / 1.7);
		final var semiMajorL = Math.max(0.1 * massDistanceFactor, getExclusionRadius(node));
		final var semiMajorH = this.rng.weightedDouble("disc_max", 1, 2, 100) * massDistanceFactor;
		return new Interval(semiMajorL, semiMajorH);
	}

	private double pickPlanetMass(Context ctx, CelestialNode parent, double discMass) {
		double weight = 1;
		double minMass = this.rng.weightedDouble("mass_min", 1.5, 0.0, 0.0) * parent.massYg;
		double maxMass = this.rng.weightedDouble("mass_max", 1.5, 0.0, 0.000500) * parent.massYg;

		if (parent instanceof StellarCelestialNode) {
			final var metallicityFactor = this.systemMetallicity / Galaxy.METALLICITY_RANGE.max;
			weight *= Mth.lerp(metallicityFactor, 5, 0.8);
		}

		return this.rng.weightedDouble("mass", weight, minMass, maxMass);
	}

	private CelestialNode generateSinglePlanet(double mass, double maxDistance) {
		Assert.isTrue(!Double.isNaN(maxDistance));

		if (mass * Units.Mearth_PER_Yg > 1e-6 && rng.chance("is_binary", 0.05)) {
			final var massRatio = this.rng.uniformDouble("partner_mass_ratio", 0.5, 0.9);
			final var primaryMass = massRatio * mass;
			final var secondaryMass = (1 - massRatio) * mass;

			this.rng.push("primary");
			final var primary = generateSinglePlanet(primaryMass, maxDistance);
			this.rng.pop();

			final var distance = maxDistance;

			final var eccentricity = rng.uniformDouble("eccentricity", 0.0, 0.1);
			final var outerShape = new OrbitalShape(eccentricity, distance / (1 + eccentricity));
			final var innerShape = new OrbitalShape(eccentricity,
					outerShape.semiMajor() * (secondaryMass / primaryMass));

			// final var primaryExclusion = getExclusionRadius(primary);
			final var closestDistance = outerShape.periapsisDistance() + innerShape.periapsisDistance();
			final var childMaxDistance = 0.5 * closestDistance / SPACING_FACTOR;

			Assert.isTrue(!Double.isNaN(outerShape.semiMajor()));
			Assert.isTrue(!Double.isNaN(innerShape.semiMajor()));

			if (distance < getExclusionRadius(primary)) {
				// LOGGER.info("failed to make binary: distance {} less than exclusion {}",
				// distance,
				// getExclusionRadius(primary));
				return primary;
			}

			if (distance > maxDistance) {
				// LOGGER.info("failed to make binary: distance {} greater than max {}",
				// distance, maxDistance);
				return primary;
			}

			this.rng.push("secondary");
			final var secondary = generateSinglePlanet(secondaryMass, childMaxDistance);
			this.rng.pop();

			final var node = new BinaryCelestialNode();
			node.massYg = primary.massYg + secondary.massYg;
			node.setSiblings(primary, secondary);
			node.orbitalShapeInner = innerShape;
			node.orbitalShapeOuter = outerShape;

			final var inclination = rng.weightedDouble("inclination", 5, 0.0, 0.05 * Math.PI);
			node.orbitalPlane = OrbitalPlane.fromInclination(inclination, rng.rng("orbital_plane"));
			node.phase = rng.uniformDouble("phase", 0, 2 * Math.PI);

			// apply tidal locking
			if (primary instanceof PlanetaryCelestialNode unary1
					&& secondary instanceof PlanetaryCelestialNode unary2) {
				final var orbitalPeriod = Formulas.orbitalPeriod(innerShape.semiMajor(), node.massYg);
				final var lockedRate = 2 * Math.PI / orbitalPeriod;
				// 1 locking to 2
				final var t1 = Formulas.timeUntilTidallyLocked(unary1.massYg, unary1.radius, unary1.type.rigidity,
						unary2.massYg, innerShape.semiMajor());
				unary1.rotationalRate = Mth.lerp(Math.pow(t1, 2.0), unary1.rotationalRate, lockedRate);
				// 2 locking to 1
				final var t2 = Formulas.timeUntilTidallyLocked(unary2.massYg, unary2.radius, unary2.type.rigidity,
						unary1.massYg, outerShape.semiMajor());
				unary2.rotationalRate = Mth.lerp(Math.pow(t2, 2.0), unary2.rotationalRate, lockedRate);
			}

			// LOGGER.info("binary success: {}, {}", distance, maxDistance);

			// bnode.apsidalRate
			return node;
		} else {
			final var node = new PlanetaryCelestialNode();
			node.obliquityAngle = rng.weightedDouble("obliquity_angle", 5, 0, 2 * Math.PI);
			// node.apsidalRate
			final var rotationPerDay = 2 * Math.PI / (60 * 60 * 24);
			node.rotationalRate = rotationPerDay * rng.weightedDouble("rotational_rate", 10, 1, 9);
			if (rng.chance("retrograde_rotation", 0.02))
				node.rotationalRate *= -1;
			node.massYg = mass;

			// 5.5 g/cm3
			// node.temperature

			// TODO
			node.type = PlanetaryCelestialNode.Type.ROCKY_WORLD;

			// idk lol, should probably take the escape velocity of gasses into account
			double gasGiantChance = Mth.inverseLerp(node.massYg, 5.0 * Units.Yg_PER_Mearth, 6.0 * Units.Yg_PER_Mearth);
			gasGiantChance = Mth.clamp(gasGiantChance, 0, 1);
			if (this.rng.chance("gas_giant_chance", gasGiantChance)) {
				node.type = PlanetaryCelestialNode.Type.GAS_GIANT;
			}
			if (node.massYg > 13 * Units.Yg_PER_Mjupiter) {
				node.type = PlanetaryCelestialNode.Type.BROWN_DWARF;
			}

			if (node.massYg * Units.Mearth_PER_Yg > 0.8 && node.massYg * Units.Mearth_PER_Yg < 1.5)
				node.type = PlanetaryCelestialNode.Type.EARTH_LIKE_WORLD;

			node.radius = radiusFromMass(node.massYg);

			// LOGGER.info("single success");

			return node;
		}
	}

	private void generatePlanets(Context ctx, CelestialNode parent, double maxDistance) {

		if (parent instanceof BinaryCelestialNode binaryNode) {
			// final var primaryExclusion = getExclusionRadius(primary);
			final var closestDistance = binaryNode.orbitalShapeOuter.periapsisDistance()
					+ binaryNode.orbitalShapeInner.periapsisDistance();
			final var childMaxDistance = 0.5 * closestDistance / SPACING_FACTOR;

			generatePlanets(ctx, binaryNode.inner, childMaxDistance);
			generatePlanets(ctx, binaryNode.outer, childMaxDistance);
		}

		int maxPlanetCount = Mth.floor(this.rng.weightedDouble("max_planets", 4.0, 0.0, 100.0));

		// final var metallicityFactor = this.systemMetallicity / ctx.galaxy.info.maxMetallicity();
		// maxPlanetCount *= Math.sqrt(metallicityFactor);

		// TODO: pick better minimum and maximum
		final var massDistanceFactor = Units.Tm_PER_au * Math.pow(Units.Msol_PER_Yg * parent.massYg, 1.0 / 1.7);
		final var discMin = Math.max(0.1 * massDistanceFactor, getExclusionRadius(parent));
		double discMax = this.rng.weightedDouble("disc_max", 1, 0, 100) * massDistanceFactor;
		discMax = Math.min(discMax, maxDistance);

		double startingDiscMass = parent.massYg * this.rng.weightedDouble("disc_mass", 2.0, 0.0, 0.005);
		double discMass = startingDiscMass;

		OrbitalPlane discPlane = OrbitalPlane.ZERO;
		if (parent instanceof UnaryCelestialNode unaryNode) {
			discPlane = OrbitalPlane.fromOrbitalElements(unaryNode.obliquityAngle, 0, 0);
		}

		OrbitalShape prevShape = new OrbitalShape(0, discMin);
		OrbitalShape curShape = new OrbitalShape(0, rng.weightedDouble("initial_semi_major", 5, discMin, discMax));
		// double currentSemiMajor = rng.weightedDouble("initial_semi_major", 5,
		// discMin, discMax);
		this.rng.push("generation_attempts");
		for (int i = 0; i < PLANET_ATTEMPT_COUNT; ++i) {
			this.rng.advance();

			if (i >= maxPlanetCount)
				break;

			if (curShape.semiMajor() > discMax)
				break;

			final var discWeight = Math.pow(Mth.inverseLerp(curShape.semiMajor(), discMin, discMax), 4);
			// final var spacingFactor = this.rng.weightedDouble("spacing_factor", 3, 1,
			// 1.5);
			final var spacingFactor = 1.0;
			final var eccFactor = Mth.lerp(discWeight, 10, 1);
			final var ecc = this.rng.weightedDouble("eccentricity", eccFactor, 0.0, 0.05);

			final var periapsis = 1.4 * curShape.apoapsisDistance() * spacingFactor;
			final var semiMajor = periapsis / (1 - ecc);
			prevShape = curShape;
			curShape = new OrbitalShape(ecc, semiMajor);

			final var prevBound = prevShape.apoapsisDistance();
			final var curBound = curShape.periapsisDistance();

			if (curShape.apoapsisDistance() > discMax)
				break;

			final var incFactor = Mth.lerp(discWeight, 0.03, 0.4);
			final var inclination = 2 * Math.PI * incFactor * this.rng.uniformDouble("inclination", 0.0, 0.3);
			final var orbitalPlane = OrbitalPlane.fromInclination(inclination, this.rng.rng("orbital_plane"))
					.withReferencePlane(discPlane);

			double planetMass = 0;
			for (int j = 0; j < 16; ++j) {
				this.rng.push("mass");
				planetMass = pickPlanetMass(ctx, parent, startingDiscMass);
				this.rng.pop();
				if (planetMass <= discMass)
					break;
			}

			if (planetMass > discMass)
				break;
			discMass -= planetMass;

			this.rng.push("planet");
			if (planetMass > 0.0) {
				final var childMaxDistance = Math.min(
						Formulas.hillSphereRadius(parent.massYg, planetMass, ecc, semiMajor),
						(curBound - prevBound) / (2.0 * SPACING_FACTOR));
				final var node = generateSinglePlanet(planetMass, childMaxDistance);

				// apply tidal locking
				if (parent instanceof PlanetaryCelestialNode parentNode
						&& node instanceof PlanetaryCelestialNode child) {
					final var orbitalPeriod = Formulas.orbitalPeriod(semiMajor, parentNode.massYg);
					final var lockedRate = 2 * Math.PI / orbitalPeriod;
					final var t = Formulas.timeUntilTidallyLocked(child.massYg, child.radius, child.type.rigidity,
							parentNode.massYg, semiMajor);
					child.rotationalRate = Mth.lerp(Math.pow(t, 2.0), child.rotationalRate, lockedRate);
				}

				final var phase = rng.uniformDouble("phase", 0, 2 * Math.PI);
				final var child = new CelestialNodeChild<>(parent, node, curShape, orbitalPlane, phase);
				parent.childNodes.push(child);

				generatePlanets(ctx, node, childMaxDistance);
			}
			this.rng.pop();

		}
		this.rng.pop();
	}

	// private void generateMoonsAroundPlanet(CelestialNode planet, double
	// maxDistance) {

	// // TODO: pick better minimum and maximum
	// final var discMin = getExclusionRadius(planet);
	// // final var discMax = Units.Tm_PER_au
	// // * this.rng.weightedDouble("disc_max", 2, 0.4, 1)
	// // * Math.sqrt(Units.Msol_PER_Yg * planet.massYg);
	// final var discMax = this.rng.weightedDouble("disc_max", 2, 0.4, 1) *
	// maxDistance;

	// double discMass = planet.massYg * this.rng.weightedDouble("disc_mass", 2.0,
	// 0.0, 0.7);

	// OrbitalPlane discPlane = OrbitalPlane.ZERO;
	// if (planet instanceof UnaryCelestialNode unaryNode) {
	// discPlane = OrbitalPlane.fromOrbitalElements(unaryNode.obliquityAngle, 0, 0);
	// }

	// double currentSemiMajor = rng.weightedDouble("initial_semi_major", 5,
	// discMin, discMax);
	// this.rng.push("generation_attempts");
	// for (int i = 0; i < PLANET_ATTEMPT_COUNT; ++i) {
	// this.rng.advance();

	// Assert.isTrue(!Double.isNaN(currentSemiMajor));

	// final var spacingFactor = this.rng.weightedDouble("spacing_factor", 3, 1,
	// 1.5);
	// final var ecc = this.rng.weightedDouble("eccentricity", 10, 0.0, 0.3);

	// final var periapsis = currentSemiMajor * spacingFactor;
	// final var semiMajor = periapsis / (1 - ecc);
	// final var apoapsis = 2 * semiMajor - periapsis;
	// currentSemiMajor = 1.25 * apoapsis;

	// Assert.isTrue(!Double.isNaN(semiMajor));

	// if (apoapsis > discMax)
	// break;

	// final var orbitalShape = new OrbitalShape(ecc, semiMajor);

	// final var ascendingNode =
	// this.rng.uniformDouble("longitude_of_ascending_node", 0, 2 * Math.PI);
	// final var argPeriapsis = this.rng.uniformDouble("argument_of_periapsis", 0, 2
	// * Math.PI);
	// final var inclination = this.rng.weightedDouble("inclination", 5, 0.0, 0.02 *
	// Math.PI);
	// final var orbitalPlane = OrbitalPlane.fromOrbitalElements(inclination,
	// ascendingNode, argPeriapsis)
	// .withReferencePlane(discPlane);

	// final var planetMass = this.rng.weightedDouble("mass", 3.0, 0.0, 0.001 *
	// discMass);
	// Assert.isTrue(!Double.isNaN(planetMass));

	// final var phase = rng.uniformDouble("phase", 0, 2 * Math.PI);
	// this.rng.push("planet");
	// if (planetMass > Units.Yg_PER_Mearth * 0.001) {
	// final var childMaxDistance = Math.min(
	// Formulas.hillSphereRadius(planet.massYg, planetMass, ecc, semiMajor),
	// (apoapsis - periapsis) / (4.0 * SPACING_FACTOR));
	// final var node = generatePlanet(planetMass, childMaxDistance);

	// // apply tidal locking
	// if (planet instanceof PlanetaryCelestialNode parentNode
	// && node instanceof PlanetaryCelestialNode child) {
	// final var orbitalPeriod = Formulas.orbitalPeriod(semiMajor,
	// parentNode.massYg);
	// final var lockedRate = 2 * Math.PI / orbitalPeriod;
	// final var t = Formulas.timeUntilTidallyLocked(child.massYg, child.radius,
	// child.type.rigidity,
	// parentNode.massYg, semiMajor);
	// child.rotationalRate = Mth.lerp(Math.pow(t, 2.0), child.rotationalRate,
	// lockedRate);
	// }

	// final var child = new CelestialNodeChild<>(planet, node, orbitalShape,
	// orbitalPlane, phase);
	// planet.childNodes.push(child);
	// }
	// this.rng.pop();
	// }
	// this.rng.pop();
	// }

	// // hey nerd, just so you know, for a semi-major axis a and orbital period
	// ratio
	// // R, the second semi-major axis is `a / R^(2/3)`
	// private void generatePlanetsAroundStar(Context ctx, CelestialNode parent,
	// double initialDiscMass,
	// double maxDistance) {
	// // TODO: what is metallicity used for? I think some amount of metals are
	// needed
	// // to form actual planets, but im not sure how to work that in.
	// final var systemMetallicity =
	// ctx.galaxy.densityFields.metallicity.sample(ctx.info.systemPosTm);
	// final var metallicityFactor = systemMetallicity /
	// ctx.galaxy.info.maxMetallicity();

	// final var maxPlanetCount = Mth.floor(this.rng.weightedDouble("max_planets",
	// 2.0, 0.0, 100.0));

	// // TODO: pick better minimum and maximum
	// final var massDistanceFactor = Units.Tm_PER_au * Math.pow(Units.Msol_PER_Yg *
	// parent.massYg, 1.0 / 1.7);
	// final var discMin = Math.max(0.1 * massDistanceFactor,
	// getExclusionRadius(parent));
	// double discMax = this.rng.weightedDouble("disc_max", 1, 0, 100) *
	// massDistanceFactor;
	// discMax = Math.min(discMax, maxDistance);

	// // double discMass = initialDiscMass;
	// double startingDiscMass = parent.massYg *
	// this.rng.weightedDouble("disc_mass", 2.0, 0.0, 0.01);
	// double discMass = startingDiscMass;

	// final var discPlane = OrbitalPlane.random(this.rng.rng("disc_plane"));

	// double currentSemiMajor = rng.weightedDouble("initial_semi_major", 5,
	// discMin, discMax);
	// this.rng.push("generation_attempts");
	// for (int i = 0; i < PLANET_ATTEMPT_COUNT; ++i) {
	// this.rng.advance();

	// if (i >= maxPlanetCount)
	// break;

	// final var spacingFactor = this.rng.weightedDouble("spacing_factor", 2, 1,
	// 1.5);
	// // final var ecc = this.rng.weightedDouble("eccentricity", 5);
	// final var ecc = 0.0;
	// final var inclination1 = this.rng.weightedDouble("inclination1", 5, 0.0, 0.05
	// * Math.PI);
	// final var inclination2 = this.rng.uniformDouble("inclination2", 0.2 *
	// Math.PI, 0.5 * Math.PI);
	// final var inclination = Mth.lerp(ecc, inclination1, inclination2);

	// final var prevBound = currentSemiMajor;
	// final var periapsis = currentSemiMajor * spacingFactor;
	// final var semiMajor = periapsis / (1 - ecc);
	// final var apoapsis = 2 * semiMajor - periapsis; // *
	// Mth.lerp(Math.cos(inclination), 0.2, 1.0);
	// currentSemiMajor = 1.35 * apoapsis;
	// final var curBound = currentSemiMajor;

	// if (apoapsis > discMax)
	// break;

	// final var orbitalShape = new OrbitalShape(ecc, semiMajor);

	// final var ascendingNode =
	// this.rng.uniformDouble("longitude_of_ascending_node", 0, 2 * Math.PI);
	// final var argPeriapsis = this.rng.uniformDouble("argument_of_periapsis", 0, 2
	// * Math.PI);
	// final var orbitalPlane = OrbitalPlane.fromOrbitalElements(inclination,
	// ascendingNode, argPeriapsis)
	// .withReferencePlane(discPlane);

	// final var mf = Mth.lerp(metallicityFactor, 5.0, 1.0);
	// double planetMass = 0;
	// for (int j = 0; j < 16; ++j) {
	// this.rng.push("mass");
	// planetMass = this.rng.weightedDouble(j, mf, 0.0, 0.05 * Units.Yg_PER_Msol);
	// this.rng.pop();
	// if (planetMass <= discMass)
	// break;
	// }

	// if (planetMass > discMass)
	// break;
	// discMass -= planetMass;

	// // final var planetMass = this.rng.uniformDouble("mass", 0.5, 1.0)
	// // * sweptMass(cutoutDistance, discMass, periapsis, apoapsis);
	// final var phase = rng.uniformDouble("phase", 0, 2 * Math.PI);

	// this.rng.push("planet");
	// if (planetMass > Units.Yg_PER_Mearth * 0.01) {
	// final var childMaxDistance = Math.min(
	// Formulas.hillSphereRadius(parent.massYg, planetMass, ecc, semiMajor),
	// (curBound - prevBound) / (4.0 * SPACING_FACTOR));
	// final var node = generatePlanet(planetMass, childMaxDistance);

	// // apply tidal locking
	// if (parent instanceof PlanetaryCelestialNode parentNode
	// && node instanceof PlanetaryCelestialNode child) {
	// final var orbitalPeriod = Formulas.orbitalPeriod(semiMajor,
	// parentNode.massYg);
	// final var lockedRate = 2 * Math.PI / orbitalPeriod;
	// final var t = Formulas.timeUntilTidallyLocked(child.massYg, child.radius,
	// child.type.rigidity,
	// parentNode.massYg, semiMajor);
	// child.rotationalRate = Mth.lerp(Math.pow(t, 2.0), child.rotationalRate,
	// lockedRate);
	// }

	// final var child = new CelestialNodeChild<>(parent, node, orbitalShape,
	// orbitalPlane, phase);
	// parent.childNodes.push(child);
	// }
	// this.rng.pop();

	// }
	// this.rng.pop();

	// }

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
		LOGGER.info("Attempting Single [min={}, max={}]", minDistance, maxDistance);

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
		LOGGER.info("Success [distance={}]", distance);
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
			return SPACING_FACTOR * binaryNode.orbitalShapeOuter.semiMajor() + getExclusionRadius(binaryNode.outer);
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

				LOGGER.info("Attempting P-Type [min={}, max={}]", minRadius, maxRadius);

				if (minRadius <= maxRadius) {
					final var radius = this.rng.weightedDouble("radius", 2.0, minRadius, maxRadius);
					LOGGER.info("Success [radius={}]", radius);

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
					LOGGER.info("Attempting S-Type A");
					a = mergeStarNodes(ctx, a, toInsert, closeOrbit);
				} else if (!triedInnerB) {
					triedInnerB = true;
					LOGGER.info("Attempting S-Type B");
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
			LOGGER.info("Attempt Failed");
		return res;
	}

	private @Nullable CelestialNode mergeStarNodes(Context ctx, CelestialNode existing, StellarCelestialNode toInsert,
			boolean closeOrbit) {

		if (existing instanceof StellarCelestialNode starNode) {
			return mergeSingleStar(ctx, starNode, toInsert, closeOrbit);
		} else if (existing instanceof BinaryCelestialNode binaryNode) {
			return mergeStarWithBinary(ctx, binaryNode, toInsert, closeOrbit);
		}

		LOGGER.error("tried to merge non-stellar nodes! " + existing + ", " + toInsert);

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
