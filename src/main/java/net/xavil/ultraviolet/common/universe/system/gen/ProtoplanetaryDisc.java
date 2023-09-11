package net.xavil.ultraviolet.common.universe.system.gen;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.StableRandom;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.SortingStrategy;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.math.Formulas;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.OrbitalShape;
import net.xavil.ultraviolet.Mod;
import net.xavil.universegen.system.CelestialNode;

// TODO: this simulation is okay enough, but the algorithm is from the 80's and doesnt include more recent developments in astrophysics and whatnot.
//
// - photoevaporation dissipating gas from the inner star system much faster than the outer star system, causing the inner star system to mainly be compose of terrestrial planets.
// - photoevaporation depleting gas from the whole star system, causing planets that form in the outer star system later on in development to have low-mass atmospheres, or barely any atmospheres at all (basically, objects like neptune and uranus)
// - gravitational resonances disrupting the formation of planets, creating asteroid belts
// - hot jupiters
// - high-mass terrestrial worlds in the outer star system
// - planetary migration
public class ProtoplanetaryDisc {
	public final AccreteContext ctx;
	private Planetesimal rootStar;
	public final MutableList<Planetesimal> planetesimals = new Vector<>();
	public final DustBands dustBands;
	public final Interval planetesimalBounds;
	private int iterationsRemaining = 10000;

	private final StableRandom rng;

	// private SimulationNode rootNode;

	public ProtoplanetaryDisc(AccreteContext ctx, double mass) {
		this.ctx = ctx;
		this.rng = ctx.rng.split("main_disc");
		this.planetesimalBounds = planetesimalBounds(ctx);
		final var dustBandInterval = initialDustBandInterval(ctx);
		this.dustBands = new DustBands(ctx, dustBandInterval, mass);

		this.rootStar = new Planetesimal(ctx);
		this.rootStar.setMass(ctx.stellarMassMsol);
		this.rootStar.setOrbitalShape(new OrbitalShape(0, 0));

		// private double rotationalRate;
		// private final MutableList<Planetesimal> moons = new Vector<>();
		// private final MutableList<Ring> rings = new Vector<>();
		// public boolean sweptGas = false;

	}

	private void doSweep(Planetesimal planetesimal, boolean sweepAll) {
		if (!sweepAll) {
			this.dustBands.sweep(this.ctx, planetesimal);
			return;
		}

		double prevMass = planetesimal.getMass();
		while (this.dustBands.hasDust(planetesimal.sweptDustLimits())) {
			this.dustBands.sweep(this.ctx, planetesimal);
			// stop accumulating mass if we're getting diminishing returns, it is unlikely
			// to affect the outcome anyways
			if (planetesimal.getMass() / prevMass < 1.05)
				break;
			prevMass = planetesimal.getMass();
		}
	}

	private boolean shouldContinuePlacing() {
		// return this.dustBands.hasDust(this.planetesimalBounds);
		return true;
	}

	public void collapseDisc(CelestialNode rootNode) {
		if (this.planetesimalBounds.equals(Interval.ZERO))
			return;

		// Distribute planetary masses
		for (int i = 0; i < 5000; ++i) {
			final var attemptRng = this.rng.split(i);

			// generate planetesimal
			final var semiMajor = attemptRng.uniformDouble("semi_major",
					this.planetesimalBounds.lower, this.planetesimalBounds.higher);
			// var heightAboveDiscCenter = this.this.rng.uniformDoubleAround(0,
			// this.dustBands.);
			// heightAboveDiscCenter = Math.signum(heightAboveDiscCenter) *
			// Math.pow(Math.abs(heightAboveDiscCenter), 2.0);
			final var maxInclination = Math.atan(1 / (15 * semiMajor));
			final var inclination = attemptRng.weightedDouble("inclination", 8, 0, maxInclination);

			final var planetesimal = Planetesimal.random(this.ctx, semiMajor, inclination);
			// if (this.dustBands.hasDust(planetesimal.effectLimits())) {
			this.planetesimals.push(planetesimal);
			// }
		}

		while (shouldContinuePlacing() && this.iterationsRemaining >= 0) {
			this.iterationsRemaining -= 1;
			final var attemptRng = this.rng.split(this.iterationsRemaining);

			// allow protoplanet seed to accumulate dust and gas before moving onto the
			// others. There is a certain percent chance on each sweep step that the
			// planetesimal does not continue to accrete mass, allowing others to sweep more
			// instead.
			// doSweep(planetesimal, false);
			this.planetesimals.shuffle(Rng.fromSeed(attemptRng.uniformLong("shuffle_seed")));
			for (final var other : this.planetesimals.iterable()) {
				doSweep(other, false);
			}
			// this.planetesimals.push(planetesimal);
			transformPlanetesimals((prev, next) -> coalescePlanetesimals(this.ctx, attemptRng, prev, next));

			final var dt = attemptRng.uniformDouble("dt", 0.5, 5);
			this.dustBands.step(this, dt);
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
		this.planetesimals.sort(SortingStrategy.UNSTABLE,
				Comparator.comparingDouble(planetesimal -> planetesimal.getOrbitalShape().semiMajor()));
		final var prev = new Vector<>(this.planetesimals);
		this.planetesimals.clear();
		consumer.accept(prev, this.planetesimals);
	}

	public void coalescePlanetesimals(AccreteContext ctx, StableRandom rng,
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
				final var parent = current.getParentBody();

				// interactions with parent
				if (parent != null) {
					// collision
					if (current.getOrbitalShape().periapsisDistance()
							- current.getRadius() / Units.km_PER_au < parent.getRadius() / Units.km_PER_au) {
						handleCollision(ctx, rng, parent, current);
						continue;
					}

					// ring
					final var rocheLimit = rocheLimit(parent.getMass(), current.getMass(), current.getRadius());
					if (current.getOrbitalShape().periapsisDistance() <= rocheLimit) {
						Planetesimal.convertToRing(parent, current);
						continue;
					}
				}

				// interactions with siblings
				while (i < prevBodies.size()) {
					final var next = prevBodies.get(i);

					// no intersection with the next planetesimal, just let the outer loop carry on
					// merging from its new position
					if (!current.effectLimits().intersects(next.effectLimits()))
						break;

					i += 1;
					final var coalesced = handlePlanetesimalIntersection(ctx, rng, current, next);
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
	public Planetesimal handlePlanetesimalIntersection(AccreteContext ctx, StableRandom rng, Planetesimal a,
			Planetesimal b) {
		final var larger = a.getMass() >= b.getMass() ? a : b;
		final var smaller = a.getMass() >= b.getMass() ? b : a;

		// final var rocheLimit = rocheLimit(larger.getMass(), smaller.getMass(),
		// smaller.getRadius());
		// if (Math.abs(a.getOrbitalShape().semiMajor() -
		// b.getOrbitalShape().semiMajor()) <= rocheLimit / 2.0) {
		// if (Math.abs(a.getOrbitalShape().semiMajor() -
		// b.getOrbitalShape().semiMajor()) <= larger.getRadius()
		// / Units.km_PER_au) {
		if (Math.abs(a.getOrbitalShape().semiMajor() - b.getOrbitalShape().semiMajor()) <= larger.getRadius()
				/ Units.km_PER_au) {
			handleCollision(ctx, rng, a, b);
		} else {
			handleGravitationalCapture(ctx, rng, a, b);
		}
		return larger;
	}

	// TODO: moons might attain their own debris disc and have a recursive accretion
	// process
	public void handleGravitationalCapture(AccreteContext ctx, StableRandom rng, Planetesimal a, Planetesimal b) {
		final var larger = a.getMass() >= b.getMass() ? a : b;
		final var smaller = a.getMass() >= b.getMass() ? b : a;

		// TODO: maybe we could do something more interesting than transferring
		// everything to moons of the new parent
		larger.transformMoons((prevL, newL) -> smaller.transformMoons((prevS, newS) -> {
			// newL.extend(prevL.iter().filter(p -> rng.split(p.getId()).chance("ejected",
			// 0.5)));
			newL.extend(prevL);
			newL.extend(prevS);
			newL.push(smaller);
			// Mod.LOGGER.info("capture! {} + {} -> {}", prevL.size(), prevS.size(),
			// newL.size());
		}));

		larger.setOrbitalShape(Planetesimal.calculateCombinedOrbitalShape(a, b));

		moonStuff(ctx, rng.split("resolve"), larger);
	}

	private void moonStuff(AccreteContext ctx, StableRandom rng, Planetesimal parent) {

		// final var hillSphereRadius = parent.hillSphereRadius(ctx.stellarMassMsol);
		// Assert.isTrue(hillSphereRadius > 0);

		// orbital chaos ensues
		final var randomizeOrbitsRng = rng.split("randomize_orbits");
		parent.getSattelites().forEach(moon -> {
			final var hillSphereRadius = Formulas.hillSphereRadius(
					Units.Yg_PER_Msol * parent.getMass(),
					Units.Yg_PER_Msol * moon.getMass(),
					moon.getOrbitalShape().eccentricity(),
					Units.Tm_PER_au * moon.getOrbitalShape().semiMajor()) / Units.Tm_PER_au;
			final var moonRng = randomizeOrbitsRng.split(moon.getId());
			final var randomSemiMajor = moonRng.weightedDouble("semi_major", 2, 0, hillSphereRadius);
			moon.setOrbitalShape(new OrbitalShape(moon.getOrbitalShape().eccentricity(), randomSemiMajor));
		});

		final var coalesceRng = rng.split("coalesce");
		parent.transformMoons((prevMoons, newMoons) -> {
			// Mod.LOGGER.info("moon count before = {}", prevMoons.size());
			coalescePlanetesimals(ctx, coalesceRng, prevMoons, newMoons);
			// Mod.LOGGER.info("moon count after = {}", newMoons.size());
		});
	}

	public void handleCollision(AccreteContext ctx, StableRandom rng, Planetesimal a, Planetesimal b) {

		a.setMass(a.getMass() + b.getMass());

		if (a.getParentBody() == b.getParentBody()) {
			final var newShape = Planetesimal.calculateCombinedOrbitalShape(a, b);
			a.setOrbitalShape(newShape);
		} else if (a == b.getParentBody()) {
		}

		a.transformMoons((prevL, newL) -> b.transformMoons((prevS, newS) -> {
			newL.extend(prevL);
			newL.extend(prevS);
			// Mod.LOGGER.info("collide! {} + {} -> {}", prevL.size(), prevS.size(),
			// newL.size());
		}));

		moonStuff(ctx, rng.split("resolve"), a);

	}

	public static double rocheLimit(double planetMass, double moonMass, double moonRadius) {
		return Formulas.rocheLimit(Units.Yg_PER_Msol * planetMass, Units.Yg_PER_Msol * moonMass, moonRadius)
				/ Units.Tm_PER_au;
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
