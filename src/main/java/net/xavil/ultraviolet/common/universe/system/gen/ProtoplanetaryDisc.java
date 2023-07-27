package net.xavil.ultraviolet.common.universe.system.gen;

import java.util.Comparator;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.OrbitalShape;

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
	public final MutableList<Planetesimal> planetesimals = new Vector<>();
	public final DustBands dustBands;
	public final Interval planetesimalBounds;
	private int iterationsRemaining = 10000;

	public ProtoplanetaryDisc(AccreteContext ctx) {
		this.ctx = ctx;
		this.planetesimalBounds = planetesimalBounds(ctx);
		final var maxThickness = this.planetesimalBounds.higher();
		final var discThickness = ctx.rng.uniformDouble(0.05 * maxThickness, maxThickness);
		final var dustBandInterval = initialDustBandInterval(ctx);
		this.dustBands = new DustBands(dustBandInterval, discThickness);
	}

	private void doSweep(Planetesimal planetesimal, boolean sweepAll) {
		while (this.dustBands.hasDust(planetesimal.sweptDustLimits())) {
			final var sweptMass = this.dustBands.sweep(this.ctx, planetesimal);
			planetesimal.setMass(planetesimal.getMass() + sweptMass);
			// if (!sweepAll && this.ctx.rng.chance(0.05))
			// break;
		}
	}

	private boolean shouldContinuePlacing() {
		return this.dustBands.hasDust(this.planetesimalBounds);
	}

	public void collapseDisc(CelestialNode rootNode) {
		if (this.planetesimalBounds.equals(Interval.ZERO))
			return;

		// Distribute planetary masses

		while (shouldContinuePlacing() && this.iterationsRemaining >= 0) {
			this.iterationsRemaining -= 1;

			// generate planetesimal
			final var dustySemiMajor = this.ctx.rng.uniformDouble(this.planetesimalBounds);
			var heightAboveDiscCenter = this.ctx.rng.uniformDoubleAround(0, this.dustBands.bandThickness);
			// heightAboveDiscCenter = Math.signum(heightAboveDiscCenter) *
			// Math.pow(Math.abs(heightAboveDiscCenter), 2.0);
			// final var inclination = Math.atan(heightAboveDiscCenter / dustySemiMajor);
			final var inclination = 0;
			final var planetesimal = Planetesimal.random(this.ctx, dustySemiMajor, inclination,
					this.planetesimalBounds);

			// allow protoplanet seed to accumulate dust and gas before moving onto the
			// others. There is a certain percent chance on each sweep step that the
			// planetesimal does not continue to accrete mass, allowing others to sweep more
			// instead.
			doSweep(planetesimal, false);
			this.planetesimals.shuffle(this.ctx.rng);
			for (final var other : this.planetesimals.iterable()) {
				doSweep(other, false);
			}
			this.planetesimals.push(planetesimal);
			transformPlanetesimals((prev, next) -> coalescePlanetesimals(this.ctx, prev, next));
		}

		// clean up any leftovers i guess
		for (final var other : this.planetesimals.iterable()) {
			doSweep(other, true);
		}

		// TODO: catastrophic events (rogue planets n stuff)
		// TODO: planetary migration

		// Post Accretion
		// Process Planets

		for (final var planet : this.planetesimals.iterable()) {
			planet.convertToPlanetNode(rootNode);
		}
	}

	public void transformPlanetesimals(BiConsumer<MutableList<Planetesimal>, MutableList<Planetesimal>> consumer) {
		this.planetesimals.sort(Comparator.comparingDouble(planetesimal -> planetesimal.getOrbitalShape().semiMajor()));
		final var prev = MutableList.copyOf(this.planetesimals);
		this.planetesimals.clear();
		consumer.accept(prev, this.planetesimals);
	}

	public void coalescePlanetesimals(AccreteContext ctx,
			MutableList<Planetesimal> prevBodies, MutableList<Planetesimal> nextBodies) {

		boolean isFirstIteration = true;
		boolean didCoalesce;
		do {
			if (!isFirstIteration) {
				prevBodies.clear();
				prevBodies.extend(nextBodies);
				nextBodies.clear();
			}

			didCoalesce = false;
			int i = 0;
			while (i < prevBodies.size()) {
				var current = prevBodies.get(i++);
				while (i < prevBodies.size()) {
					final var next = prevBodies.get(i);

					// no intersection with the next planetesimal, just let the outer loop carry on
					// merging from its new position
					if (!current.effectLimits().intersects(next.effectLimits()))
						break;

					i += 1;
					final var coalesced = handlePlanetesimalIntersection(ctx, current, next);
					current = coalesced != null ? coalesced : current;
					didCoalesce = true;
				}
				nextBodies.push(current);
			}

			isFirstIteration = false;
		} while (didCoalesce);
	}

	/**
	 * Merge two planetisimals that have overlapping gravitational influences. This
	 * will not necessarily result in a collision, but rather may result in a binary
	 * system, or a capture of a moon.
	 * 
	 * @return The combined planetesimal. This may be one of the arguments.
	 */
	@Nullable
	public Planetesimal handlePlanetesimalIntersection(AccreteContext ctx, Planetesimal a, Planetesimal b) {
		final var larger = a.getMass() >= b.getMass() ? a : b;
		final var smaller = a.getMass() >= b.getMass() ? b : a;
		final var rocheLimit = rocheLimit(larger.getMass(), smaller.getMass(), smaller.getRadius());
		if (Math.abs(a.getOrbitalShape().semiMajor() - b.getOrbitalShape().semiMajor()) <= rocheLimit / 2.0) {
			// TODO: currently, all moons of `smaller` are completely discarded
			handlePlanetesimalCollision(ctx, a, b);
		} else {
			// ejection could maybe create objects like Eris and Haumea
			// TODO: this is not based on anything other than "objects get ejected
			// sometimes"
			// chance to eject planetesimal
			if (!ctx.rng.chance(0.2)) {
				captureMoon(ctx, a, b);
			} else {
				// maybe perturb orbit of larger planetesimal
				// maybe eject both bodies in some circumstances
			}
		}
		return larger;
	}

	// TODO: moons might attain their own debris disc and have a recursive accretion
	// process
	public void captureMoon(AccreteContext ctx, Planetesimal a, Planetesimal b) {
		final var larger = a.getMass() >= b.getMass() ? a : b;
		final var smaller = a.getMass() >= b.getMass() ? b : a;

		// TODO: maybe we could do something more interesting than transferring
		// everything to moons of the new parent
		larger.transformMoons((prevL, newL) -> smaller.transformMoons((prevS, newS) -> {
			newL.extend(prevL);
			newL.extend(prevS);
			newL.push(smaller);
		}));

		larger.setOrbitalShape(Planetesimal.calculateCombinedOrbitalShape(a, b));

		var rocheLimit = rocheLimit(larger.getMass(), smaller.getMass(), smaller.getRadius());
		var hillSphereRadius = larger.hillSphereRadius(ctx.stellarMassMsol);
		Assert.isTrue(hillSphereRadius > 0);

		// orbital chaos ensues
		larger.getMoons().forEach(moon -> {
			final var semiMajorT = Math.pow(ctx.rng.uniformDouble(), 2);
			final var randomSemiMajor = Mth.lerp(semiMajorT, 0, hillSphereRadius);
			moon.setOrbitalShape(new OrbitalShape(moon.getOrbitalShape().eccentricity(), randomSemiMajor));
		});

		// resolve chaos
		larger.transformMoons((prevMoons, newMoons) -> {
			for (var moon : prevMoons.iterable()) {
				var semiMajorT = Math.pow(ctx.rng.uniformDouble(), 2);
				var randomSemiMajor = Mth.lerp(semiMajorT, 0, hillSphereRadius);
				var randomEccentricity = Planetesimal.randomEccentricity(ctx);
				moon.setOrbitalShape(new OrbitalShape(randomEccentricity, randomSemiMajor));

				if (moon.getOrbitalShape().periapsisDistance() - (moon.getRadius() / Units.km_PER_au) <= 2
						* (larger.getRadius() / Units.km_PER_au)) {
					handlePlanetesimalCollision(ctx, larger, moon);
				} else if (moon.getOrbitalShape().periapsisDistance() <= 2 * rocheLimit) {
					larger.addRing(moon.asRing());
				} else {
					newMoons.push(moon);
				}
			}
		});

		larger.transformMoons((prevMoons, newMoons) -> {
			coalescePlanetesimals(ctx, prevMoons, newMoons);
		});
	}

	public void handlePlanetesimalCollision(AccreteContext ctx, Planetesimal a, Planetesimal b) {
		Assert.isTrue((a.getParentBody() == b.getParentBody()) || (a == b.getParentBody()));
		// Assert.isTrue(a.getMass() >= b.getMass());

		// TODO: sometimes, a collision can create a moon or maybe binary system, like
		// as in the earth/moon system.
		a.setMass(a.getMass() + b.getMass());
		if (a != b.getParentBody()) {
			var newShape = Planetesimal.calculateCombinedOrbitalShape(a, b);
			a.setOrbitalShape(newShape);
		}
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
