package net.xavil.ultraviolet.common.universe.system.gen;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.SplittableRng;
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
	public Planetesimal root;
	public final DustBands dustBands;
	public final Interval planetesimalBounds;

	public int totalIterationCount = 10000;
	public int iterationsRemaining = totalIterationCount;

	public final StableRandom rng;

	public double nextRoguePlanetTime;
	public double nextPlanetTime;

	public ProtoplanetaryDisc(AccreteContext ctx, double mass) {
		this.ctx = ctx;
		this.rng = ctx.rng.split("main_disc");
		this.planetesimalBounds = planetesimalBounds(ctx);
		final var dustBandInterval = initialDustBandInterval(ctx);
		this.dustBands = new DustBands(ctx, dustBandInterval, mass);

		this.root = new Planetesimal(ctx);
		this.root.sweptGas = true;
		this.root.mass = ctx.stellarMassMsol;
		this.root.stellarProperties.load(new SplittableRng(this.rng.uniformLong("star_props")),
				this.root.mass * Units.Msol_PER_Yg, this.root.age);

		nextRoguePlanetTime = this.rng.uniformDouble("rogue_planet_time", 0, this.ctx.systemAgeMya);
		nextPlanetTime = 0;
	}

	private boolean shouldContinuePlacing() {
		return this.iterationsRemaining >= 0 && this.ctx.currentSystemAgeMya <= Math.min(this.ctx.systemAgeMya, 1000);
	}

	private void placePlanetesimal(StableRandom rng) {
		final var semiMajor = rng.uniformDouble("semi_major",
				this.planetesimalBounds.min, this.planetesimalBounds.max);
		final var maxInclination = Math.atan(1 / (15 * semiMajor));
		final var inclination = rng.weightedDouble("inclination", 8, 0, maxInclination);

		final var planetesimal = Planetesimal.random(this.ctx, semiMajor, inclination);
		planetesimal.stellarProperties.load(new SplittableRng(rng.uniformLong("star_props")),
				planetesimal.mass * Units.Msol_PER_Yg, planetesimal.age);
		// if (this.dustBands.hasDust(this.planetesimalBounds)) {
		this.root.addSattelite(planetesimal);
		// }
	}

	private void placeRoguePlanetesimal(StableRandom rng) {
		final var semiMajor = rng.uniformDouble("semi_major",
				this.planetesimalBounds.min, this.planetesimalBounds.max);
		final var inclination = rng.uniformDouble("inclination", 0, Math.PI);

		final var planetesimal = Planetesimal.random(this.ctx, semiMajor, inclination);

		// planetesimal.mass = rng.weightedDouble("mass", 8,
		// this.ctx.params.initialPlanetesimalMass, 0.3);
		planetesimal.mass = rng.weightedDouble("mass", 1, 0.01, 0.3);
		planetesimal.sweptGas |= planetesimal.mass > Units.Mjupiter_PER_Yg * Units.Yg_PER_Msol
				|| (planetesimal.canSweepGas() && rng.chance("is_gas_giant", 0.75));

		planetesimal.stellarProperties.load(new SplittableRng(rng.uniformLong("star_props")),
				planetesimal.mass * Units.Msol_PER_Yg, planetesimal.age);

		planetesimal.orbitalShape = planetesimal.orbitalShape
				.withEccentricity(rng.uniformDouble("eccentricity", 0, 0.7));

		planetesimal.age = rng.uniformDouble("age", 0, this.ctx.galaxy.info.ageMya);

		this.root.addSattelite(planetesimal);
	}

	public void step() {
		final var stepRng = this.rng.split("step");
		final var attemptRng = stepRng.split(this.iterationsRemaining);

		this.iterationsRemaining -= 1;

		final var placementRng = attemptRng.split("placement");
		if (this.ctx.currentSystemAgeMya >= this.nextRoguePlanetTime) {
			this.nextRoguePlanetTime = this.ctx.currentSystemAgeMya
					+ placementRng.uniformDouble("rogue_planet_time", 0, this.ctx.systemAgeMya);
			if (placementRng.chance("rogue_planet_chance", 0.67)) {
				placeRoguePlanetesimal(placementRng.split("rogue_planetesimal"));
			}
		}

		while (this.ctx.currentSystemAgeMya >= this.nextPlanetTime) {
			this.nextPlanetTime += placementRng.weightedDouble("planet_time", 2, 0.01, 5);
			placePlanetesimal(placementRng.split("planetesimal"));
		}

		this.root.sattelites.shuffle(Rng.fromSeed(attemptRng.uniformLong("shuffle_seed")));
		// TODO: maybe keep track of which planets need to accrete in a separate list so
		// we don't need to spend time trying to accrete when accretion would do
		// basically nothing.
		this.dustBands.sweep(this.ctx, this.root);

		this.root.transformMoons((prev, next) -> {
			this.root = coalescePlanetesimals(this.ctx, attemptRng, this.root, prev, next);
		});

		final var dt = attemptRng.weightedDouble("dt", 2, 0.1, 1);
		this.dustBands.step(this, dt);
		this.ctx.currentSystemAgeMya += dt;
	}

	@Nullable
	public CelestialNode collapseDisc() {
		if (this.planetesimalBounds.equals(Interval.ZERO))
			return null;

		while (shouldContinuePlacing()) {
			if (Thread.interrupted())
				return null;
			step();
		}

		// TODO: supernovae
		// TODO: planetary migration

		return this.root.convertToCelestialNode();
	}

	// public void transformPlanetesimals(BiConsumer<MutableList<Planetesimal>,
	// MutableList<Planetesimal>> consumer) {
	// this.root.sattelites.sort(SortingStrategy.UNSTABLE,
	// Comparator.comparingDouble(planetesimal ->
	// planetesimal.orbitalShape.semiMajor()));
	// final var prev = new Vector<>(this.root.sattelites);
	// this.root.sattelites.clear();
	// for (final var sattelite : prev.iterable()) {
	// sattelite.satteliteOf = null;
	// }
	// consumer.accept(prev, this.root.sattelites);
	// for (final var sattelite : this.root.sattelites.iterable()) {
	// sattelite.satteliteOf = null;
	// }
	// }

	private Planetesimal makeBinary(Planetesimal a, Planetesimal b) {
		final var binary = new Planetesimal(this.ctx);
		binary.isBinary = true;
		if (rng.split(binary.id).chance("binary_order", 0.5)) {
			binary.binaryA = a;
			binary.binaryB = b;
		} else {
			binary.binaryA = b;
			binary.binaryB = a;
		}
		binary.mass = binary.binaryA.mass + binary.binaryB.mass;

		final var maxMass = Math.max(binary.binaryA.mass, binary.binaryB.mass);
		final var massRatioA = binary.binaryA.mass / maxMass;
		final var massRatioB = binary.binaryB.mass / maxMass;

		binary.binaryA.orbitalShape = binary.binaryA.orbitalShape
				.withSemiMajor(massRatioA * binary.binaryA.orbitalShape.semiMajor());
		binary.binaryB.orbitalShape = binary.binaryB.orbitalShape
				.withSemiMajor(massRatioB * binary.binaryB.orbitalShape.semiMajor());

		return binary;
	}

	private double binaryGetApoapsis(Planetesimal binary) {
		Assert.isTrue(binary.isBinary);
		return Math.max(
				binary.binaryA.orbitalShape.apoapsisDistance(),
				binary.binaryB.orbitalShape.apoapsisDistance());
	}

	private boolean swapIfChildLarger(Planetesimal parent, Planetesimal child) {
		if (parent != child.satteliteOf)
			return false;
		// Assert.isTrue(parent == child.satteliteOf);
		if (parent.mass >= child.mass)
			return false;

		parent.sattelites.remove(parent.sattelites.indexOf(child));
		child.sattelites.push(parent);

		final var tmp1 = parent.satteliteOf;
		parent.satteliteOf = child.satteliteOf;
		child.satteliteOf = tmp1;
		final var tmp2 = parent.orbitalShape;
		parent.orbitalShape = child.orbitalShape;
		child.orbitalShape = tmp2;

		return true;
	}

	private static final double BINARY_STABILITY_FACTOR = 2.0;

	private Planetesimal promoteBinaryThreeway(StableRandom rng, Planetesimal parent, Planetesimal child) {
		Assert.isTrue(parent.isBinary && !child.isBinary);

		boolean mergeWithA = false, mergeWithB = false;

		final var binAEffects = child.effectLimits().intersects(parent.binaryA.effectLimits());
		final var binBEffects = child.effectLimits().intersects(parent.binaryB.effectLimits());
		if (binAEffects && binBEffects) {
			final var sa = parent.binaryA.orbitalShape.semiMajor();
			final var sb = parent.binaryB.orbitalShape.semiMajor();
			final var sc = child.orbitalShape.semiMajor();
			mergeWithA = Math.abs(sc - sa) <= Math.abs(sc - sb);
			mergeWithB = !mergeWithA;
		} else {
			mergeWithA = binAEffects;
			mergeWithB = binBEffects;
		}

		if (mergeWithA || mergeWithB) {
			final var bin = mergeWithA ? parent.binaryA : parent.binaryB;
			bin.addSattelite(child);
			bin.transformMoons((prev, next) -> coalescePlanetesimals(ctx, rng.split(bin.id), bin, prev, next));
		}

		return parent;
	}

	private Planetesimal tryPromoteBinary(StableRandom rng, Planetesimal parent, Planetesimal child) {
		if (child.mass <= 0.05 * parent.mass) {
			// no new binary nodes will be created here

			if (!parent.isBinary && !child.isBinary)
				return parent;

			if (parent.isBinary && !child.isBinary) {
				return promoteBinaryThreeway(rng, parent, child);
			}

			Mod.LOGGER.warn("discarding non-merge");
			return parent;
		}

		if (!parent.isBinary && !child.isBinary) {
			final var binary = makeBinary(parent, child);
			binary.orbitalShape = parent.orbitalShape;
			return binary;
		}

		if (parent.isBinary && !child.isBinary) {
			if (child.orbitalShape.periapsisDistance() > BINARY_STABILITY_FACTOR * binaryGetApoapsis(parent)) {
				final var binary = makeBinary(parent, child);
				binary.orbitalShape = parent.orbitalShape;
				return binary;
			}

			// merge with inner
			// if outer is not stability limit, eject it

			// return promoteBinaryThreeway(rng, parent, child);
		}

		Mod.LOGGER.warn("discarding merge parentBinary={}, childBinary={}", parent.isBinary, child.isBinary);
		return parent;
	}

	public Planetesimal coalescePlanetesimals(AccreteContext ctx, StableRandom rng,
			Planetesimal parent,
			MutableList<Planetesimal> prevBodies, MutableList<Planetesimal> nextBodies) {

		boolean isFirstIteration = true;
		boolean didCoalesce;
		do {
			if (!isFirstIteration) {
				prevBodies.clear();
				prevBodies.extend(nextBodies);
				nextBodies.clear();
			}

			prevBodies.sort(Comparator.comparingDouble(p -> p.orbitalShape.semiMajor()));

			didCoalesce = false;
			int i = 0;
			while (i < prevBodies.size()) {
				var current = prevBodies.get(i++);
				// final var parent = current.satteliteOf;

				// interactions with parent
				if (parent != null) {
					// // always keep the parent mass larger than the current mass
					// if (swapIfChildLarger(parent, current)) {
					// final var tmp = parent;
					// parent = current;
					// current = tmp;
					// }

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

					// parent = tryPromoteBinary(rng, parent, current);
				}

				// interactions with siblings
				while (i < prevBodies.size()) {
					final var next = prevBodies.get(i);

					// no intersection with the next planetesimal, just let the outer loop carry on
					// merging from its new position
					if (!current.effectLimits().intersects(next.effectLimits()))
						break;

					i += 1;
					final var coalesced = handleSiblingIntersection(ctx, rng, current, next);
					current = coalesced != null ? coalesced : current;
					didCoalesce = true;
				}
				nextBodies.push(current);
			}

			isFirstIteration = false;
		} while (didCoalesce);

		return parent;
	}

	/**
	 * Merge two planetisimals that have overlapping gravitational influences. This
	 * will not necessarily result in a collision, but rather may result in a binary
	 * system, or a capture of a moon.
	 * 
	 * @return The combined planetesimal. This may be one of the arguments.
	 */
	@Nullable
	public Planetesimal handleSiblingIntersection(AccreteContext ctx, StableRandom rng,
			Planetesimal a, Planetesimal b) {
		final var larger = a.mass >= b.mass ? a : b;
		final var smaller = a.mass >= b.mass ? b : a;

		if (rng.chance("collide", 0.0)) {
			if (Math.abs(a.orbitalShape.semiMajor() - b.orbitalShape.semiMajor()) <= larger.getRadius()
					/ Units.km_PER_au) {
				return handleCollision(ctx, rng, a, b);
			}
		}
		return handleGravitationalCapture(ctx, rng.split("capture"), a, b);
	}

	// TODO: moons might attain their own debris disc and have a recursive accretion
	// process
	public Planetesimal handleGravitationalCapture(AccreteContext ctx, StableRandom rng, Planetesimal a,
			Planetesimal b) {
		final var larger = a.mass >= b.mass ? a : b;
		final var smaller = a.mass >= b.mass ? b : a;

		// TODO: maybe we could do something more interesting than transferring
		// everything to moons of the new parent
		larger.transformMoons((prevL, newL) -> smaller.transformMoons((prevS, newS) -> {
			newL.extend(prevL.iter().filter(p -> rng.split(p.id).chance("ejected", 0.5)));
			newL.extend(prevS.iter().filter(p -> rng.split(p.id).chance("ejected", 0.5)));
			newL.push(smaller);
			// Mod.LOGGER.info("capture! {} + {} -> {}", prevL.size(), prevS.size(),
			// newL.size());
		}));

		larger.orbitalShape = Planetesimal.calculateCombinedOrbitalShape(a, b);

		return moonStuff(ctx, rng.split("resolve"), larger);
	}

	private Planetesimal moonStuff(AccreteContext ctx, StableRandom rng, Planetesimal parent) {

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

		final var res = new Object() {
			Planetesimal newParent = parent;
		};

		final var coalesceRng = rng.split("coalesce");
		parent.transformMoons((prevMoons, newMoons) -> {
			// Mod.LOGGER.info("moon count before = {}", prevMoons.size());
			res.newParent = coalescePlanetesimals(ctx, coalesceRng, parent, prevMoons, newMoons);
			// Mod.LOGGER.info("moon count after = {}", newMoons.size());
		});

		return res.newParent;
	}

	public Planetesimal handleCollision(AccreteContext ctx, StableRandom rng, Planetesimal a, Planetesimal b) {

		a.mass += b.mass;
		a.stellarProperties.load(new SplittableRng(a.rng.uniformLong("star_props")), a.mass * Units.Msol_PER_Yg, a.age);

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

		return moonStuff(ctx, rng.split("resolve"), a);
	}

	public static double rocheLimit(double planetMass, double moonMass, double moonRadius) {
		return Formulas.rocheLimit(Units.Yg_PER_Msol * planetMass, Units.Yg_PER_Msol * moonMass, moonRadius)
				/ Units.Tm_PER_au;
	}

	private static Interval planetesimalBounds(AccreteContext ctx) {
		var outer = 20 * Math.sqrt(ctx.stellarMassMsol);
		return new Interval(0.0, outer);
	}

	private static Interval initialDustBandInterval(AccreteContext ctx) {
		// var outer = 200 * Math.cbrt(ctx.stellarMassMsol);
		return new Interval(0, 200 * Math.sqrt(ctx.stellarMassMsol));
	}
}
