package net.xavil.ultraviolet.common.universe.system.gen;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.math.Formulas;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.OrbitalShape;
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
	public final MutableList<Planetesimal> planetesimals = new Vector<>();
	public final DustBands dustBands;
	public final Interval planetesimalBounds;
	private int iterationsRemaining = 10000;

	private SimulationNode rootNode;

	private int discResolution = 512;
	private double discMax;
	private final double[] binAreas = new double[this.discResolution];
	private final double[] gasMass = new double[this.discResolution];
	private final double[] dustMass = new double[this.discResolution];
	private final double[] stellarWindOutwards = new double[this.discResolution];
	private final double[] stellarWindInwards = new double[this.discResolution];

	private double binArea(int bin) {
		final var inner = discMax * (bin / (double) this.discResolution);
		final var outer = discMax * ((bin + 1) / (double) this.discResolution);
		return Math.PI * (Mth.square(inner) + Mth.square(outer));
	}

	private double getGasWeight() {
		return 1.5;
	}

	private double getDustWeight() {
		return 40;
	}

	private void distributeDiscMass(double discMass, double discMax, double metallicity) {
		this.discMax = discMax;
		final var totalArea = Math.PI * Mth.square(discMax);
		for (int i = 0; i < this.discResolution; ++i) {
			this.binAreas[i] = binArea(i);
			final var binMass = discMass * binArea(i) / totalArea;
			this.gasMass[i] = (1.0 - metallicity) * binMass;
			this.dustMass[i] = metallicity * binMass;
		}
	}

	private void stellarWindContribution(double[] out, int startBin, double starLuminosity, boolean forwards) {
		int binDir = forwards ? 1 : -1;
		int bin = startBin;
		double remainingPercent = 1.0;
		while (bin < this.discResolution) {
			// TODO: i have no idea what im doing

			// reduce the amount of radiation that gets to further parts of the disc
			final var combinedDensity = (this.gasMass[bin] + this.dustMass[bin]) / this.binAreas[bin];
			remainingPercent *= Math.expm1(0.1 * combinedDensity);

			final var binStart = this.discMax * (bin / (double) this.discResolution);
			final var lum = starLuminosity / (Math.PI * Mth.square(binStart));
			out[bin] += 0.1 * lum * remainingPercent;

			// update bin counter
			// the disc is symmetric around the center, so we flip the direction when
			// crossing that boundary.
			bin += binDir;
			binDir = bin == 0 ? 1 : binDir;
		}
	}

	public void step(double dtMyr) {
		// this simulation is not physically-based at all, but attempts to emulate the
		// effects of stellar winds blowing the gas and dust of the disc away.
		Arrays.fill(this.stellarWindOutwards, 0.0);
		Arrays.fill(this.stellarWindInwards, 0.0);

		for (final var node : this.rootNode.iterable()) {
			if (node.getType() == SimulationNode.NodeType.STAR && node instanceof SimulationNode.Unary starNode) {

				// get bucket the star falls into
				// fill out flow maps in both directions
				// -> take into account extinction
				// -> gas and dust absorption (stellar wind slamming into gas and dust, giving
				// the disc particles some inwards/outwards velocity)
				// -> inverse square law
				// -> luminosity of star
				// -> thickness of disc (thicker discs means more flow should happen, since they
				// can catch more of the stellar wind and this is a 1d approximation that doesnt
				// simulate anything in the vertical dimension)

				final var discPercent = starNode.semiMajor / this.discMax;
				final var starBin = Mth.floor(discPercent);
				final var binPercent = discPercent - starBin;

				// FIXME: apply flows from current bin

				final var starEnergy = 0.5 * starNode.luminosity();
				stellarWindContribution(this.stellarWindOutwards, starBin + 1, starEnergy, true);
				stellarWindContribution(this.stellarWindInwards, starBin - 1, starEnergy, false);
			}
		}

		for (int i = 0; i < this.discResolution; ++i) {
			final var outwardFlow = dtMyr * this.stellarWindOutwards[i];
			final var inwardFlow = dtMyr * this.stellarWindInwards[i];
			final var requestedFlow = outwardFlow + inwardFlow;

			// mass exitance in general
			final var actualGasFlow = Math.max(requestedFlow, this.gasMass[i]) / getGasWeight();
			final var actualDustFlow = Math.max(requestedFlow, this.dustMass[i]) / getDustWeight();
			this.gasMass[i] -= actualGasFlow;
			this.dustMass[i] -= actualDustFlow;

			// mass exitance in specific directions
			final var outwardsPercent = outwardFlow / requestedFlow;
			final var inwardsPercent = inwardFlow / requestedFlow;

			if (Mth.abs(i + 1) < this.discResolution) {
				this.gasMass[Mth.abs(i + 1)] += outwardsPercent * actualGasFlow;
				this.dustMass[Mth.abs(i + 1)] += outwardsPercent * actualDustFlow;
			}
			if (Mth.abs(i - 1) < this.discResolution) {
				this.gasMass[Mth.abs(i - 1)] += inwardsPercent * actualGasFlow;
				this.dustMass[Mth.abs(i - 1)] += inwardsPercent * actualDustFlow;
			}
		}

		final var initialMass = this.ctx.params.initialPlanetesimalMass;

		// attempt to generate clumping
		for (int i = 0; i < 16; ++i) {
			final var semiMajor = this.ctx.rng.uniformDouble(0.0, discMax);

			final var bin = Mth.floor(semiMajor / this.discMax);
			if (bin >= this.discResolution || this.dustMass[bin] < initialMass)
				continue;

			final var inclination = 0;
			this.dustMass[bin] -= initialMass;

			final var node = new SimulationNode.Unary(this.ctx);
			node.mass = initialMass;
			node.semiMajor = semiMajor;
			node.inclination = inclination;
			// node.parent = this.rootNode;

			sweep(node);
			break;
		}

		// TODO: process interations (moons, binaries)
		this.rootNode.update(this);

		// TODO: catastrophic events (rogue planets n stuff)
		// TODO: planetary migration
		// TODO: gravitational resonances
	}

	private void sweepRange(SimulationNode.Unary node, int bin, double percent) {
		final var sweptDust = percent * this.dustMass[bin];
		// TODO: softer gas sweeping cutoff
		final var sweptGas = node.mass > node.criticalMass() ? percent * this.gasMass[bin] : 0;

		node.mass += sweptDust;
		node.mass += sweptGas;
		if (node instanceof SimulationNode.Unary pnode)
			pnode.gasMass += sweptGas;

		this.dustMass[bin] -= sweptDust;
		this.gasMass[bin] -= sweptGas;
	}

	private void sweep(SimulationNode.Unary node) {
		double prevMass = node.mass;

		for (int attempt = 0; attempt < 100; ++attempt) {
			final var limits = node.effectLimits();
			final var sweepLo = this.discResolution * limits.lower() / this.discMax;
			final var sweepHi = this.discResolution * limits.higher() / this.discMax;
			final var binLo = Mth.floor(sweepLo);
			final var binHi = Mth.floor(sweepHi);

			if (binLo == binHi) {
				// binLo == binHi guarantees that this difference will never be greater than 1
				sweepRange(node, binLo, sweepHi - sweepLo);
			} else if (binHi > binLo) {
				sweepRange(node, binLo, 1.0 - (sweepLo - binLo));
				sweepRange(node, binHi, sweepHi - binHi);
				for (int i = binLo + 1; i < binHi; ++i) {
					sweepRange(node, i, 1.0);
				}
			}

			// chance to cut sweeping short, to maybe let other planets do their thing
			if (this.ctx.rng.chance(0.05))
				break;

			// stop sweeping if there's not a whole lot changing...
			if (node.mass - prevMass < 0.05)
				break;
			prevMass = node.mass;
		}
	}

	public CelestialNode build() {
		return this.rootNode.convertToCelestialNode();
	}

	public ProtoplanetaryDisc(AccreteContext ctx, double mass, double metallicity) {
		this.ctx = ctx;
		this.planetesimalBounds = planetesimalBounds(ctx);
		final var maxThickness = this.planetesimalBounds.higher();
		final var discThickness = ctx.rng.uniformDouble(0.05 * maxThickness, maxThickness);
		final var dustBandInterval = initialDustBandInterval(ctx);
		this.dustBands = new DustBands(dustBandInterval, discThickness);

		// distributeDiscMass(mass, dustBandInterval.higher(), metallicity);
		// this.rootNode = SimulationNode.Unary.star(ctx, ctx.stellarMassMsol, metallicity);
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
			// var heightAboveDiscCenter = this.ctx.rng.uniformDoubleAround(0,
			// this.dustBands.bandThickness);
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
					Planetesimal.convertToRing(larger, moon);
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
		return Formulas.rocheLimit(Units.Yg_PER_Msol * planetMass, Units.Yg_PER_Msol * moonMass, moonRadius);
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
