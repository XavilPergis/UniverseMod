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
	private Planetesimal root;
	public final DustBands dustBands;
	public final Interval planetesimalBounds;

	private int totalIterationCount = 10000;
	private int iterationsRemaining = totalIterationCount;

	private final StableRandom rng;

	// private SimulationNode rootNode;

	public ProtoplanetaryDisc(AccreteContext ctx, double mass) {
		this.ctx = ctx;
		this.rng = ctx.rng.split("main_disc");
		this.planetesimalBounds = planetesimalBounds(ctx);
		final var dustBandInterval = initialDustBandInterval(ctx);
		this.dustBands = new DustBands(ctx, dustBandInterval, mass);

		this.root = new Planetesimal(ctx);
		this.root.mass = ctx.stellarMassMsol;
		this.root.stellarProperties.load(this.root.mass * Units.Msol_PER_Yg, this.root.age);
	}

	private void doSweep(Planetesimal planetesimal, boolean sweepAll) {
		if (!sweepAll) {
			this.dustBands.sweep(this.ctx, planetesimal);
			return;
		}

		double prevMass = planetesimal.mass;
		while (this.dustBands.hasDust(planetesimal.sweptDustLimits())) {
			this.dustBands.sweep(this.ctx, planetesimal);
			// stop accumulating mass if we're getting diminishing returns, it is unlikely
			// to affect the outcome anyways
			if (planetesimal.mass / prevMass < 1.05)
				break;
			prevMass = planetesimal.mass;
		}
	}

	private boolean shouldContinuePlacing() {
		return this.iterationsRemaining >= 0 && this.ctx.currentSystemAgeMya <= Math.min(this.ctx.systemAgeMya, 1000);
	}

	private void placePlanetesimal(StableRandom rng) {
		final var semiMajor = rng.uniformDouble("semi_major",
				this.planetesimalBounds.lower, this.planetesimalBounds.higher);
		final var maxInclination = Math.atan(1 / (15 * semiMajor));
		final var inclination = rng.weightedDouble("inclination", 8, 0, maxInclination);

		final var planetesimal = Planetesimal.random(this.ctx, semiMajor, inclination);
		planetesimal.stellarProperties.load(planetesimal.mass * Units.Msol_PER_Yg, planetesimal.age);
		if (this.dustBands.hasDust(this.planetesimalBounds)) {
			this.root.addSattelite(planetesimal);
		}
	}

	private void placeRoguePlanetesimal(StableRandom rng) {
		final var semiMajor = rng.uniformDouble("semi_major",
				this.planetesimalBounds.lower, this.planetesimalBounds.higher);
		final var inclination = rng.uniformDouble("inclination", 0, Math.PI);

		final var planetesimal = Planetesimal.random(this.ctx, semiMajor, inclination);

		planetesimal.mass = rng.weightedDouble("mass", 3, this.ctx.params.initialPlanetesimalMass, 0.3);
		planetesimal.sweptGas |= planetesimal.mass > Units.Mjupiter_PER_Yg * Units.Yg_PER_Msol
				|| (planetesimal.canSweepGas() && rng.chance("is_gas_giant", 0.75));

		planetesimal.stellarProperties.load(planetesimal.mass * Units.Msol_PER_Yg, planetesimal.age);

		planetesimal.orbitalShape = planetesimal.orbitalShape
				.withEccentricity(rng.uniformDouble("eccentricity", 0, 0.7));

		planetesimal.age = rng.uniformDouble("age", 0, this.ctx.galaxy.info.ageMya);

		this.root.addSattelite(planetesimal);
	}

	public void collapseDisc(CelestialNode rootNode) {
		if (this.planetesimalBounds.equals(Interval.ZERO))
			return;

		// Distribute planetary masses
		{
			final var placementRng = this.rng.split("placement");
			for (int i = 0; i < 1000; ++i) {
				placePlanetesimal(placementRng.split(i));
			}
		}

		double nextRoguePlanetTime = this.rng.uniformDouble("rogue_planet_time", 0, this.ctx.systemAgeMya);
		double nextPlanetTime = 0;

		final var stepRng = this.rng.split("step");
		while (shouldContinuePlacing()) {
			this.iterationsRemaining -= 1;
			final var attemptRng = stepRng.split(this.iterationsRemaining);

			final var placementRng = attemptRng.split("placement");
			if (this.ctx.currentSystemAgeMya >= nextRoguePlanetTime) {
				nextRoguePlanetTime = this.ctx.currentSystemAgeMya
						+ placementRng.uniformDouble("rogue_planet_time", 0, this.ctx.systemAgeMya);
				if (placementRng.chance("rogue_planet_chance", 0.67)) {
					placeRoguePlanetesimal(placementRng.split("rogue_planetesimal"));
				}
			}

			while (this.ctx.currentSystemAgeMya >= nextPlanetTime) {
				nextPlanetTime += placementRng.weightedDouble("planet_time", 0.75, 0, 10);
				placePlanetesimal(placementRng.split("planetesimal"));
			}

			this.root.sattelites.shuffle(Rng.fromSeed(attemptRng.uniformLong("shuffle_seed")));
			for (final var other : this.root.sattelites.iterable()) {
				// TODO: maybe keep track of which planets need to accrete in a separate list so
				// we don't need to spend time trying to accrete when accretion would do
				// basically nothing.
				doSweep(other, false);
			}

			transformPlanetesimals((prev, next) -> coalescePlanetesimals(this.ctx, attemptRng, prev, next));

			final var dt = attemptRng.uniformDouble("dt", 0.5, 5);
			this.dustBands.step(this, dt);
			this.ctx.currentSystemAgeMya += dt;
		}

		// clean up any leftovers i guess
		for (final var other : this.root.sattelites.iterable()) {
			doSweep(other, true);
		}

		// TODO: supernovae
		// TODO: planetary migration

		// Post Accretion
		// Process Planets

		for (final var planet : this.root.sattelites.iterable()) {
			planet.convertToPlanetNode(rootNode);
		}
	}

	public void transformPlanetesimals(BiConsumer<MutableList<Planetesimal>, MutableList<Planetesimal>> consumer) {
		this.root.sattelites.sort(SortingStrategy.UNSTABLE,
				Comparator.comparingDouble(planetesimal -> planetesimal.orbitalShape.semiMajor()));
		final var prev = new Vector<>(this.root.sattelites);
		this.root.sattelites.clear();
		consumer.accept(prev, this.root.sattelites);
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
				final var parent = current.satteliteOf;

				// interactions with parent
				if (parent != null) {
					// collision
					if (current.orbitalShape.periapsisDistance()
							- current.getRadius() / Units.km_PER_au < parent.getRadius() / Units.km_PER_au) {
						handleCollision(ctx, rng, parent, current);
						continue;
					}

					// ring
					final var rocheLimit = rocheLimit(parent.mass, current.mass, current.getRadius());
					if (current.orbitalShape.periapsisDistance() <= rocheLimit) {
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
		final var larger = a.mass >= b.mass ? a : b;
		final var smaller = a.mass >= b.mass ? b : a;

		if (Math.abs(a.orbitalShape.semiMajor() - b.orbitalShape.semiMajor()) <= larger.getRadius()
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
		final var larger = a.mass >= b.mass ? a : b;
		final var smaller = a.mass >= b.mass ? b : a;

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

		larger.orbitalShape = Planetesimal.calculateCombinedOrbitalShape(a, b);

		moonStuff(ctx, rng.split("resolve"), larger);
	}

	private void moonStuff(AccreteContext ctx, StableRandom rng, Planetesimal parent) {

		// final var hillSphereRadius = parent.hillSphereRadius(ctx.stellarMassMsol);
		// Assert.isTrue(hillSphereRadius > 0);

		// orbital chaos ensues
		final var randomizeOrbitsRng = rng.split("randomize_orbits");
		parent.sattelites.forEach(moon -> {
			final var hillSphereRadius = Formulas.hillSphereRadius(
					Units.Yg_PER_Msol * parent.mass,
					Units.Yg_PER_Msol * moon.mass,
					moon.orbitalShape.eccentricity(),
					Units.Tm_PER_au * moon.orbitalShape.semiMajor()) / Units.Tm_PER_au;
			final var moonRng = randomizeOrbitsRng.split(moon.id);
			final var randomSemiMajor = moonRng.weightedDouble("semi_major", 1, 0, hillSphereRadius);
			moon.orbitalShape = moon.orbitalShape.withSemiMajor(randomSemiMajor);
		});

		final var coalesceRng = rng.split("coalesce");
		parent.transformMoons((prevMoons, newMoons) -> {
			// Mod.LOGGER.info("moon count before = {}", prevMoons.size());
			coalescePlanetesimals(ctx, coalesceRng, prevMoons, newMoons);
			// Mod.LOGGER.info("moon count after = {}", newMoons.size());
		});
	}

	public void handleCollision(AccreteContext ctx, StableRandom rng, Planetesimal a, Planetesimal b) {

		a.mass += b.mass;
		a.stellarProperties.load(a.mass * Units.Msol_PER_Yg, a.age);

		if (a.satteliteOf == b.satteliteOf) {
			final var newShape = Planetesimal.calculateCombinedOrbitalShape(a, b);
			a.orbitalShape = newShape;
		} else if (a == b.satteliteOf) {
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

	private static Interval planetesimalBounds(AccreteContext ctx) {
		var outer = 20 * Math.sqrt(ctx.stellarMassMsol);
		var idealInterval = new Interval(0.0, outer);
		return idealInterval.intersection(ctx.stableOrbitInterval);
	}

	private static Interval initialDustBandInterval(AccreteContext ctx) {
		// var outer = 200 * Math.cbrt(ctx.stellarMassMsol);
		return new Interval(0, 200 * Math.sqrt(ctx.stellarMassMsol));
	}
}
