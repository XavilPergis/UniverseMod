package net.xavil.universal.common.universe.system.gen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.xavil.universal.Mod;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.util.Units;
import net.xavil.util.math.Interval;
import net.xavil.util.math.OrbitalShape;

public class ProtoplanetaryDisc {
	public final AccreteContext ctx;
	public final List<Planetesimal> planetesimals = new ArrayList<>();
	public final DustBands dustBands;
	public final Interval planetesimalBounds;

	public ProtoplanetaryDisc(AccreteContext ctx) {
		this.ctx = ctx;
		this.planetesimalBounds = planetesimalBounds(ctx);
		this.dustBands = initialDustBand(ctx);
	}

	public void collapseDisc(CelestialNode rootNode) {
		// Distribute planetary masses

		var iterationsRemaining = 1000;
		while (this.dustBands.hasDust(this.planetesimalBounds)) {
			if (iterationsRemaining-- <= 0)
				break;

			// TODO: maybe there's a more efficient way to choose the location of new
			// planets than just picking at random and seeing if there happens to be dust
			// left.
			var planetesimal = Planetesimal.random(this.ctx, this.planetesimalBounds);
			if (this.dustBands.hasDust(planetesimal.sweptDustLimits(this.ctx))) {
				Mod.LOGGER.info("placing new planetesimal at {} au", planetesimal.orbitalShape.semiMajor());
				planetesimal.accreteDust(this.ctx, this.dustBands);
				this.dustBands.defragment();

				planetesimal.radius = kothariRadius(this.ctx, planetesimal);

				// TODO: reject planetesimals smaller than a certain mass?
				this.planetesimals.add(planetesimal);
				this.planetesimals.sort(Comparator.comparingDouble(p -> p.orbitalShape.semiMajor()));

				// This loop is here to slightly mitigate a potential corner case, wherein the
				// merging of two bodies creates a new body whose effect radius overlaps with
				// the previous body. Since we only process intersections forwards, we don't
				// catch those cases and may need to retry coalescence to merge backwards. I
				// suspect this case would be quite rare, so we're gonna limit the amount of
				// retries to a small number and hope for the best!
				var retryAttempts = 0;
				while (retryAttempts < 10) {
					if (!coalescePlanetesimals(ctx, this.planetesimals))
						break;
					retryAttempts += 1;
				}
			}
		}

		for (var planet : this.planetesimals) {
			planet.convertToPlanetNode(rootNode);
		}

		// Post Accretion
		// Process Planets
	}

	public static boolean coalescePlanetesimals(AccreteContext ctx, List<Planetesimal> planetesimals) {
		final var prevPlanets = new ArrayList<>(planetesimals);
		planetesimals.clear();

		boolean didCoalesce = false;
		var i = 0;
		while (i < prevPlanets.size()) {
			var current = prevPlanets.get(i++);
			while (i < prevPlanets.size()) {
				var next = prevPlanets.get(i);

				// no intersection with the next planetesimal, just let the outer loop carry on
				// merging from its new position
				if (!current.effectLimits().intersects(next.effectLimits()))
					break;

				i += 1;
				didCoalesce = true;
				current = handlePlanetesimalIntersection(ctx, current, next);
			}
			planetesimals.add(current);
		}

		return didCoalesce;
	}

	public static Planetesimal handlePlanetesimalIntersection(AccreteContext ctx, Planetesimal a, Planetesimal b) {
		if (b.mass > a.mass) {
			var tmp = a;
			a = b;
			b = tmp;
		}

		Mod.LOGGER.info("coalescing planetesimals at {} au and {} au", a.orbitalShape.semiMajor(),
				b.orbitalShape.semiMajor());

		if (a.moonOf != null) {
			return handlePlanetesimalCollision(ctx, a, b);
		} else {
			var rocheLimit = rocheLimit(a.mass, b.mass, b.radius);
			if (Math.abs(a.orbitalShape.semiMajor() - b.orbitalShape.semiMajor()) <= 2 * rocheLimit) {
				return handlePlanetesimalCollision(ctx, a, b);
			} else {
				return captureMoon(ctx, a, b);
			}
		}
	}

	public static Planetesimal captureMoon(AccreteContext ctx, Planetesimal a, Planetesimal b) {
		a.moons.add(b);
		a.moons.addAll(b.moons);
		for (var moon : a.moons)
			moon.moonOf = a;

		a.orbitalShape = calculateCombinedOrbitalShape(a, b);

		var rocheLimit = rocheLimit(a.mass, b.mass, b.radius);
		var hillSphereRadius = a.hillSphereRadius(ctx.stellarMassMsol);
		for (var moon : a.moons) {
			var randomSemiMajor = ctx.random.nextDouble(0, hillSphereRadius);
			var randomEccentricity = Planetesimal.randomEccentricity(ctx);
			moon.orbitalShape = new OrbitalShape(randomEccentricity, randomSemiMajor);

			if (moon.orbitalShape.periapsisDistance() - moon.radius <= 2 * a.radius) {
				a = handlePlanetesimalCollision(ctx, a, moon);
			} else if (moon.orbitalShape.periapsisDistance() <= 2 * rocheLimit) {
				// turn moon into ring
			}
		}

		coalescePlanetesimals(ctx, a.moons);
		return a;
	}

	public static Planetesimal handlePlanetesimalCollision(AccreteContext ctx, Planetesimal a, Planetesimal b) {
		// TODO: sometimes, a collision can create a moon or maybe binary system, like
		// as in the earth/moon system.
		a.mass += b.mass;
		a.isGasGiant |= b.isGasGiant;
		a.orbitalShape = calculateCombinedOrbitalShape(a, b);
		a.radius = kothariRadius(ctx, a);
		return a;
	}

	public static OrbitalShape calculateCombinedOrbitalShape(Planetesimal a, Planetesimal b) {
		var combinedMass = a.mass + b.mass;
		var newSemiMajor = combinedMass / (a.mass / a.orbitalShape.semiMajor() + b.mass / b.orbitalShape.semiMajor());
		var ta = a.mass * Math.sqrt(a.orbitalShape.semiMajor() * (1 - Math.pow(a.orbitalShape.eccentricity(), 2)));
		var tb = b.mass * Math.sqrt(b.orbitalShape.semiMajor() * (1 - Math.pow(b.orbitalShape.eccentricity(), 2)));
		var tCombined = ta + tb / (combinedMass * Math.sqrt(newSemiMajor));
		var newEccentricity = Math.sqrt(Math.abs(1 - (tCombined * tCombined)));
		return new OrbitalShape(newEccentricity, combinedMass);
	}

	public static double rocheLimit(double planetMass, double moonMass, double moonRadius) {
		return moonRadius / Units.km_PER_au * Math.cbrt(2 * (planetMass / moonMass));
	}

	public static double kothariRadius(AccreteContext ctx, Planetesimal planetesimal) {
		var distanceToStar = planetesimal.distanceToStar();
		int zone = 0;
		if (distanceToStar < 4 * Math.sqrt(ctx.stellarLuminosityLsol)) {
			zone = 1;
		} else if (distanceToStar < 15 * Math.sqrt(ctx.stellarLuminosityLsol)) {
			zone = 2;
		} else {
			zone = 3;
		}

		double atomicWeight = 0;
		double atomicNum = 0;
		if (zone == 1 && planetesimal.isGasGiant) {
			atomicWeight = 9.5;
			atomicNum = 4.5;
		} else if (zone == 1 && !planetesimal.isGasGiant) {
			atomicWeight = 15.0;
			atomicNum = 8.0;
		} else if (zone == 2 && planetesimal.isGasGiant) {
			atomicWeight = 2.47;
			atomicNum = 2.0;
		} else if (zone == 2 && !planetesimal.isGasGiant) {
			atomicWeight = 10.0;
			atomicNum = 5.0;
		} else if (zone == 3 && planetesimal.isGasGiant) {
			atomicWeight = 7.0;
			atomicNum = 4.0;
		} else if (zone == 3 && !planetesimal.isGasGiant) {
			atomicWeight = 10.0;
			atomicNum = 5.0;
		}

		final double BETA = 7;
		final double A1 = 1;
		final double A2 = 1;

		// A - atomicWeight
		// Z - atomicNum
		double numerator = 2.0 * BETA * A1 * atomicNum * Math.cbrt(planetesimal.mass);
		double denominator1 = A1 * A2 * atomicNum * atomicNum * Math.cbrt(atomicWeight);
		double denominator2 = A2 * A2 * Math.pow(atomicWeight, 5.0 / 3.0)
				* Math.pow(planetesimal.mass, 2.0 / 3.0);

		double radiusCm = numerator / (denominator1 + denominator2);
		// return radiusCm * (Units.CENTI / Units.KILO);
		return radiusCm * Units.m_PER_Rsol / 1000;
	}

	private static Interval planetesimalBounds(AccreteContext ctx) {
		var inner = 0.3 * Math.cbrt(ctx.stellarMassMsol);
		var outer = 50 * Math.cbrt(ctx.stellarMassMsol);
		return new Interval(inner, outer);
	}

	private static DustBands initialDustBand(AccreteContext ctx) {
		var outer = 200 * Math.cbrt(ctx.stellarMassMsol);
		return new DustBands(new Interval(0, outer));
	}
}
