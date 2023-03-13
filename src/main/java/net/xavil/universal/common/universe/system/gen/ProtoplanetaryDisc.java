package net.xavil.universal.common.universe.system.gen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import net.xavil.universal.Mod;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.util.Assert;
import net.xavil.util.Units;
import net.xavil.util.math.Interval;
import net.xavil.util.math.OrbitalShape;

// TODO: this simulation is okay enough, but the algorithm is from the 80's and doesnt include more recent developments in astrophysics and whatnot.
//
// some results that i would like to be able to see:
// - moonmoons
// - binary orbits
//
// some things i think the algorithm lacks:
// - disc dissipation from stellar winds is missing
//     - this might be modelled by the dust density function in some way, but its invariant to time, soooooo...
// - does not take local metallicity or available mass into account
// - cannot generate asteroid belts
// - assumes a single-star system layout
// - does not track inclination of planetesimals' orbits
// - can sometimes generate stars that are larger than the primary star
//
// some ideas for improvment:
// - weight moon orbits closer to their barent object, so it looks bigger and prettier in the sky
// - allow moon-moon captures under certain circumstances
public class ProtoplanetaryDisc {
	public final AccreteContext ctx;
	public final List<Planetesimal> planetesimals = new ArrayList<>();
	public final DustBands dustBands;
	public final Interval planetesimalBounds;

	public ProtoplanetaryDisc(AccreteContext ctx) {
		this.ctx = ctx;
		this.planetesimalBounds = planetesimalBounds(ctx);
		var dustBandInterval = initialDustBandInterval(ctx);
		this.dustBands = new DustBands(dustBandInterval, ctx.debugConsumer);

		ctx.debugConsumer.accept(new AccreteDebugEvent.Initialize(dustBandInterval, planetesimalBounds));
	}

	public void collapseDisc(CelestialNode rootNode) {
		if (this.planetesimalBounds.equals(Interval.ZERO))
			return;

		// Distribute planetary masses

		var iterationsRemaining = 10000;
		while (this.dustBands.hasDust(this.planetesimalBounds)) {
			if (iterationsRemaining-- <= 0) {
				Mod.LOGGER.warn("ran out of iterations while collapsing disc!");
				break;
			}

			// TODO: maybe there's a more efficient way to choose the location of new
			// planets than just picking at random and seeing if there happens to be dust
			// left.

			// var dustySemiMajor = this.dustBands.pickDusty(this.ctx.rng,
			// this.planetesimalBounds);
			// if (Double.isNaN(dustySemiMajor))
			// break;
			var dustySemiMajor = this.ctx.rng.uniformDouble(this.planetesimalBounds);
			var planetesimal = Planetesimal.random(this.ctx, dustySemiMajor, this.planetesimalBounds);

			if (this.dustBands.hasDust(planetesimal.sweptDustLimits())) {
				if (ctx.debugConsumer.shouldEmitEvents()) {
					ctx.debugConsumer.accept(new AccreteDebugEvent.PlanetesimalCreated(ctx, planetesimal));
					this.ctx.debugConsumer
							.accept(new AccreteDebugEvent.UpdateOrbits(-1, Set.of(planetesimal.getId()), Set.of()));
				}
				planetesimal.accreteDust(dustBands);

				// TODO: reject planetesimals smaller than a certain mass?
				this.planetesimals.add(planetesimal);
				this.planetesimals.sort(Comparator.comparingDouble(p -> p.getOrbitalShape().semiMajor()));

				transformPlanetesimals((prev, next) -> coalescePlanetesimals(ctx, prev, next));
			}
		}

		for (var planet : this.planetesimals) {
			planet.convertToPlanetNode(rootNode);
			// var desc = planet.canSweepGas() ? "GAS GIANT" : "PLANET";
			// Mod.LOGGER.info("[{}] ({}) mass={} radius={} ({} moons)", desc,
			// planet.getId(), planet.getMass(), planet.getRadius(),
			// Lists.newArrayList(planet.getMoons()).size());
		}

		// Post Accretion
		// Process Planets
	}

	public void transformPlanetesimals(BiConsumer<List<Planetesimal>, List<Planetesimal>> consumer) {
		var prev = List.copyOf(this.planetesimals);
		this.planetesimals.clear();
		consumer.accept(prev, this.planetesimals);

		if (this.ctx.debugConsumer.shouldEmitEvents()) {
			var prevSet = prev.stream().map(p -> p.getId()).collect(Collectors.toSet());
			var curSet = this.planetesimals.stream().map(p -> p.getId()).collect(Collectors.toSet());
			var added = Sets.difference(curSet, prevSet);
			var removed = Sets.difference(prevSet, curSet);
			if (!added.isEmpty() || !removed.isEmpty()) {
				this.ctx.debugConsumer.accept(new AccreteDebugEvent.UpdateOrbits(-1, added, removed));
			}
		}
	}

	public void coalescePlanetesimals(AccreteContext ctx, List<Planetesimal> prevPlanets,
			List<Planetesimal> newPlanets) {
		int i = 0;
		while (i < prevPlanets.size()) {
			var current = prevPlanets.get(i++);
			while (i < prevPlanets.size()) {
				var next = prevPlanets.get(i);

				// no intersection with the next planetesimal, just let the outer loop carry on
				// merging from its new position
				if (!current.effectLimits().intersects(next.effectLimits()))
					break;

				i += 1;
				handlePlanetesimalIntersection(ctx, current, next);
				current = current.getMass() >= next.getMass() ? current : next;
			}
			newPlanets.add(current);
		}
	}

	public void handlePlanetesimalIntersection(AccreteContext ctx, Planetesimal a, Planetesimal b) {
		if (b.getMass() > a.getMass()) {
			var tmp = a;
			a = b;
			b = tmp;
		}

		if (a.getParentBody() != null) {
			handlePlanetesimalCollision(ctx, a, b);
		} else {
			var rocheLimit = rocheLimit(a.getMass(), b.getMass(), b.getRadius());
			if (Math.abs(a.getOrbitalShape().semiMajor() - b.getOrbitalShape().semiMajor()) <= 2 * rocheLimit) {
				handlePlanetesimalCollision(ctx, a, b);
			} else {
				captureMoon(ctx, a, b);
			}
		}
	}

	public void captureMoon(AccreteContext ctx, Planetesimal a, Planetesimal b) {
		a.transformMoons((prevA, newA) -> b.transformMoons((prevB, newB) -> {
			newA.addAll(prevA);
			newA.addAll(prevB);
			newA.add(b);
		}));

		a.setOrbitalShape(Planetesimal.calculateCombinedOrbitalShape(a, b));

		var rocheLimit = rocheLimit(a.getMass(), b.getMass(), b.getRadius());
		var hillSphereRadius = a.hillSphereRadius(ctx.stellarMassMsol);
		Assert.isTrue(hillSphereRadius > 0);

		a.transformMoons((prevMoons, newMoons) -> {
			for (var moon : prevMoons) {
				var randomSemiMajor = ctx.rng.uniformDouble(0, hillSphereRadius);
				var randomEccentricity = Planetesimal.randomEccentricity(ctx);
				moon.setOrbitalShape(new OrbitalShape(randomEccentricity, randomSemiMajor));

				if (moon.getOrbitalShape().periapsisDistance() - moon.getRadius() <= 2 * a.getRadius()) {
					handlePlanetesimalCollision(ctx, a, moon);
					newMoons.add(moon);
				} else if (moon.getOrbitalShape().periapsisDistance() <= 2 * rocheLimit) {
					a.addRing(moon.asRing());
				}
			}
		});

		a.transformMoons((prevMoons, newMoons) -> {
			coalescePlanetesimals(ctx, prevMoons, newMoons);
		});
	}

	public void handlePlanetesimalCollision(AccreteContext ctx, Planetesimal a, Planetesimal b) {
		Assert.isTrue((a.getParentBody() == b.getParentBody()) || (a == b.getParentBody()));
		Assert.isTrue(a.getMass() >= b.getMass());

		// ctx.debugConsumer.accept(new AccreteDebugEvent.PlanetesimalCollision(a, b));

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
		var inner = 0.5 * Math.sqrt(ctx.stellarMassMsol);
		var outer = 20 * Math.sqrt(ctx.stellarMassMsol);
		var idealInterval = new Interval(inner, outer);
		return idealInterval.intersection(ctx.stableOrbitInterval);
	}

	private static Interval initialDustBandInterval(AccreteContext ctx) {
		// var outer = 200 * Math.cbrt(ctx.stellarMassMsol);
		return new Interval(0, 200 * Math.sqrt(ctx.stellarMassMsol));
	}
}
