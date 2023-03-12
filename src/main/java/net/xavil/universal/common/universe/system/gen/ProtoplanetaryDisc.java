package net.xavil.universal.common.universe.system.gen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.xavil.universal.Mod;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.util.Assert;
import net.xavil.util.Units;
import net.xavil.util.math.Interval;
import net.xavil.util.math.OrbitalShape;

public class ProtoplanetaryDisc {
	public final AccreteContext ctx;
	public final List<Planetesimal> planetesimals = new ArrayList<>();
	public final DustBands dustBands;
	public final Interval planetesimalBounds;

	public ProtoplanetaryDisc(AccreteContext ctx, double protoMass) {
		this.ctx = ctx;
		this.planetesimalBounds = planetesimalBounds(ctx);
		this.dustBands = initialDustBand(ctx);
	}

	public void collapseDisc(CelestialNode rootNode) {
		// Distribute planetary masses

		ctx.debugConsumer.accept(new AccreteDebugEvent.Initialize(new Interval(0, 200 * Math.cbrt(ctx.stellarMassMsol)),
				planetesimalBounds));

		var iterationsRemaining = 1000;
		while (this.dustBands.hasDust(this.planetesimalBounds)) {
			if (iterationsRemaining-- <= 0) {
				Mod.LOGGER.warn("ran out of iterations while collapsing disc!");
				break;
			}

			// TODO: maybe there's a more efficient way to choose the location of new
			// planets than just picking at random and seeing if there happens to be dust
			// left.
			var planetesimal = Planetesimal.random(this.ctx, this.planetesimalBounds);
			if (this.dustBands.hasDust(planetesimal.sweptDustLimits())) {
				ctx.debugConsumer.accept(new AccreteDebugEvent.AddPlanetesimal(ctx, planetesimal));

				planetesimal.accreteDust(dustBands);

				// TODO: reject planetesimals smaller than a certain mass?
				this.planetesimals.add(planetesimal);
				this.planetesimals.sort(Comparator.comparingDouble(p -> p.getOrbitalShape().semiMajor()));
			}

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

			this.dustBands.defragment();
		}

		for (var planet : this.planetesimals) {
			planet.convertToPlanetNode(rootNode);
			// var desc = planet.canSweepGas() ? "GAS GIANT" : "PLANET";
			// Mod.LOGGER.info("[{}] ({}) mass={} radius={} ({} moons)", desc, planet.getId(), planet.getMass(), planet.getRadius(),
			// 		Lists.newArrayList(planet.getMoons()).size());
		}

		// Post Accretion
		// Process Planets
	}

	public boolean coalescePlanetesimals(AccreteContext ctx, List<Planetesimal> planetesimals) {
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

	public Planetesimal handlePlanetesimalIntersection(AccreteContext ctx, Planetesimal a, Planetesimal b) {
		if (b.getMass() > a.getMass()) {
			var tmp = a;
			a = b;
			b = tmp;
		}

		if (a.getParentBody() != null) {
			handlePlanetesimalCollision(ctx, a, b);
			return a;
		} else {
			var rocheLimit = rocheLimit(a.getMass(), b.getMass(), b.getRadius());
			if (Math.abs(a.getOrbitalShape().semiMajor() - b.getOrbitalShape().semiMajor()) <= 2 * rocheLimit) {
				handlePlanetesimalCollision(ctx, a, b);
				return a;
			} else {
				captureMoon(ctx, a, b);
				return a;
			}
		}
	}

	public Planetesimal captureMoon(AccreteContext ctx, Planetesimal a, Planetesimal b) {
		b.getMoons().forEach(a::addMoon);
		a.addMoon(b);

		a.setOrbitalShape(Planetesimal.calculateCombinedOrbitalShape(a, b));

		var rocheLimit = rocheLimit(a.getMass(), b.getMass(), b.getRadius());
		var hillSphereRadius = a.hillSphereRadius(ctx.stellarMassMsol);
		for (var moon : a.getMoons()) {
			var randomSemiMajor = ctx.rng.uniformDouble(0, hillSphereRadius);
			var randomEccentricity = Planetesimal.randomEccentricity(ctx);
			moon.setOrbitalShape(new OrbitalShape(randomEccentricity, randomSemiMajor));

			if (moon.getOrbitalShape().periapsisDistance() - moon.getRadius() <= 2 * a.getRadius()) {
				handlePlanetesimalCollision(ctx, a, moon);
			} else if (moon.getOrbitalShape().periapsisDistance() <= 2 * rocheLimit) {
				// turn moon into ring
			}
		}

		// FIXME
		// coalescePlanetesimals(ctx, a.moons);
		return a;
	}

	public void handlePlanetesimalCollision(AccreteContext ctx, Planetesimal a, Planetesimal b) {
		Assert.isTrue((a.getParentBody() == b.getParentBody()) || (a == b.getParentBody()));
		Assert.isTrue(a.getMass() >= b.getMass());

		ctx.debugConsumer.accept(new AccreteDebugEvent.PlanetesimalCollision(a, b));

		// TODO: sometimes, a collision can create a moon or maybe binary system, like
		// as in the earth/moon system.
		a.setMass(a.getMass() + b.getMass());
		if (a != b.getParentBody()) {
			var newShape = Planetesimal.calculateCombinedOrbitalShape(a, b);
			a.setOrbitalShape(newShape);
		}

		a.accreteDust(dustBands);
	}

	public static double rocheLimit(double planetMass, double moonMass, double moonRadius) {
		return moonRadius / Units.km_PER_au * Math.cbrt(2 * (planetMass / moonMass));
	}

	public static int orbitalZone(double distanceToStar, double luminosity) {
		if (distanceToStar < 4 * Math.sqrt(luminosity))
			return 1;
		if (distanceToStar < 15 * Math.sqrt(luminosity))
			return 2;
		return 3;
	}

	private static Interval planetesimalBounds(AccreteContext ctx) {
		var inner = 0.3 * Math.cbrt(ctx.stellarMassMsol);
		var outer = 50 * Math.cbrt(ctx.stellarMassMsol);
		return new Interval(inner, outer);
	}

	private static DustBands initialDustBand(AccreteContext ctx) {
		var outer = 200 * Math.cbrt(ctx.stellarMassMsol);
		return new DustBands(new Interval(0, outer), ctx.debugConsumer);
	}
}
