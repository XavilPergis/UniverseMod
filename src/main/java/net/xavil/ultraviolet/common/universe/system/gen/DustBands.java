package net.xavil.ultraviolet.common.universe.system.gen;

import java.util.Comparator;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.SortingStrategy;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.math.Interval;
import net.xavil.ultraviolet.Mod;

public class DustBands {

	private static final class Band {
		// public final Interval interval;
		public double bandL, bandH;
		// assume the band's mass is distributed uniformly throughout the band - not
		// really accurate, but maybe it'll be fine lol
		public double mass;

		public Band(double bandL, double bandH, double mass) {
			this.bandL = bandL;
			this.bandH = bandH;
			this.mass = mass;
		}
	}

	private static final class BandGroup {
		public final double atomicWeight;
		public MutableList<Band> bands = new Vector<>();
		public MutableList<Band> prevBands = new Vector<>();

		public BandGroup(double atomicWeight) {
			this.atomicWeight = atomicWeight;
		}

		public void setup(AccreteContext ctx, Interval initialInterval, double initialMass) {
			this.bands.clear();

			final double dr = initialInterval.size() / INITIAL_BAND_RESOLUTION;
			double prev = discDensityAt(ctx, initialInterval.min);

			double totalMass = 0;
			for (int i = 0; i < INITIAL_BAND_RESOLUTION; ++i) {
				final var tl = i / (double) INITIAL_BAND_RESOLUTION;
				final var rl = Mth.lerp(tl, initialInterval.min, initialInterval.max);
				final var rh = rl + dr;

				final var cur = discDensityAt(ctx, rh);
				final var discArea = Math.PI * (Mth.square(rh) - Mth.square(rl));
				final var bandMass = discArea * (prev + cur) / 2.0;

				Assert.isTrue(!Double.isNaN(bandMass));

				totalMass += bandMass;
				this.bands.push(new Band(rl, rh, bandMass));

				prev = cur;
			}

			// scale the mass of all bands so that the sum of all their masses becomes
			// `initialMass`.
			for (final var band : this.bands.iterable()) {
				band.mass *= initialMass / totalMass;
			}

			double tmp = 0.0;
			for (final var band : this.bands.iterable()) {
				tmp += band.mass;
			}
		}

		private static double stellarWindDistanceFactor(double d) {
			return Math.pow(2, -d);
		}

		public void step(ProtoplanetaryDisc disc, double dt) {
			final var step = BAND_STEP_FACTOR / this.atomicWeight * dt
					* Math.sqrt(disc.ctx.stellarLuminosityLsol / 10);

			for (final var band : this.bands.iterable()) {
				band.bandL += stellarWindDistanceFactor(band.bandL) * step;
				band.bandH += stellarWindDistanceFactor(band.bandH) * step;
				if (band.bandL > band.bandH) {
					final var tmp = band.bandL;
					band.bandL = band.bandH;
					band.bandH = tmp;
				}
			}

			this.bands.sort(SortingStrategy.UNSTABLE | SortingStrategy.ALMOST_SORTED,
					Comparator.comparingDouble(b -> b.bandL));
		}

		// a linear scan is likely faster for small arrays, but i think our band list is
		// large enough that this might help with perf. idk though.
		private int findStartIndex(Interval interval) {
			int boundL = 0, boundH = this.bands.size() - 1;
			while (boundL < boundH && boundH >= 0) {
				final var curI = (boundL + boundH) / 2;
				final var cur = this.bands.get(curI).bandL;

				if (cur < interval.min) {
					boundL = curI + 1;
				} else if (cur > interval.min) {
					boundH = curI - 1;
				} else {
					boundL = boundH = curI;
				}
			}

			final var bandValue = this.bands.get(boundL).bandL;
			if (interval.min > bandValue)
				return boundL + 1;

			// the band array might contain intervals with duplicate starting values, which
			// means we might not return the index of the first band we collide with, but
			// since we're dealing with floats and this whole thing is approximate anyways,
			// im not dealing with it for now.
			return boundL;
		}

		public double removeMaterial(Interval interval) {
			double accumulatedMass = 0.0;

			// final var tmp = this.prevBands;
			// this.prevBands = this.bands;
			// this.bands = tmp;

			int i = findStartIndex(interval);

			// outside the upper edge of all the bands
			if (i >= this.bands.size())
				return accumulatedMass;

			for (; i < this.bands.size(); ++i) {
				final var band = this.bands.get(i);

				// after we stop intersecting a band, we won't intersect any others
				if (interval.max < band.bandL)
					return accumulatedMass;
				Assert.isTrue(interval.intersects(band.bandL, band.bandH));

				final var sweepRatio = 0.5;

				final var removalSize = Math.max(0, Math.min(band.bandH, interval.max)
						- Math.max(band.bandL, interval.min));
				accumulatedMass += sweepRatio * band.mass * (removalSize / Interval.size(band.bandL, band.bandH));

				if (Interval.contains(band.bandL, band.bandH, interval.min)) {
					final var bandMass = band.mass * (interval.min - band.bandL)
							/ Interval.size(band.bandL, band.bandH);
					this.bands.insert(i, new Band(band.bandL, interval.min, bandMass));
					i += 1;
				}

				if (Interval.contains(band.bandL, band.bandH, interval.max)) {
					final var bandMass = band.mass * (band.bandH - interval.max)
							/ Interval.size(band.bandL, band.bandH);
					this.bands.insert(i, new Band(interval.max, band.bandH, bandMass));
					i += 1;
				}

				final var innerMass = band.mass * (1.0 - sweepRatio);
				if (innerMass > 1e-14) {
					band.mass = innerMass;
				}
			}

			// this.bands.sort(SortingStrategy.UNSTABLE, Comparator.comparingDouble(b ->
			// b.bandL));

			return accumulatedMass;
		}

	}

	private static final int INITIAL_BAND_RESOLUTION = 256;
	private static final double BAND_STEP_FACTOR = 0.07;
	private final BandGroup gasBands = new BandGroup(1.0);
	private final BandGroup dustBands = new BandGroup(20.0);

	public DustBands(AccreteContext ctx, Interval initialInterval, double initialMass) {
		initialMass *= Units.Msol_PER_Yg;
		final var md = ctx.systemMetallicity * initialMass;
		final var mg = initialMass - md;
		this.dustBands.setup(ctx, initialInterval, md);
		this.gasBands.setup(ctx, initialInterval, mg);
	}

	public void step(ProtoplanetaryDisc disc, double dt) {
		this.gasBands.step(disc, dt);
		this.dustBands.step(disc, dt);
	}

	// public boolean hasDust(Interval interval) {
	// return this.dustBands.bands.iter().any(band ->
	// interval.intersects(band.bandL, band.bandH));
	// }

	public void sweep(AccreteContext ctx, Planetesimal node, double periapsis, double apoapsis) {

		// nodes in very close proximity to their parents (like moons) should probably
		// not sweep

		if (node.isBinary) {
			final var apA = node.binaryA.orbitalShape.apoapsisDistance();
			final var apB = node.binaryB.orbitalShape.apoapsisDistance();
			sweep(ctx, node.binaryA, Math.max(0, periapsis - apA), apoapsis + apA);
			sweep(ctx, node.binaryB, Math.max(0, periapsis - apB), apoapsis + apB);
			return;
		}

		if (node.shouldContinueSweeping) {
			final var dustSweepInterval = node.sweptDustLimits(periapsis, apoapsis);
			final var criticalMass = node.criticalMass();

			double accumulatedMass = this.dustBands.removeMaterial(dustSweepInterval);

			if (node.mass + accumulatedMass >= criticalMass) {
				final var r = node.orbitalShape.semiMajor();
				final var m = Math.sqrt(criticalMass / node.mass);

				final var l = r - (m / (m + 1)) * (r - dustSweepInterval.min);
				final var h = r + (m / (m + 1)) * (dustSweepInterval.max - r);

				final var gasSweepInterval = new Interval(l, h);

				final var gasMass = this.gasBands.removeMaterial(gasSweepInterval);
				node.sweptGas |= gasMass > 0.0;
				accumulatedMass += gasMass;
			}

			node.mass += accumulatedMass;
			node.stellarProperties.load(new SplittableRng(node.rng.seed), node.mass * Units.Msol_PER_Yg, node.age);
		}

		for (final var child : node.sattelites.iterable()) {
			sweep(ctx, child,
					periapsis + child.orbitalShape.periapsisDistance(),
					apoapsis + child.orbitalShape.apoapsisDistance());
		}
	}

	public void sweep(AccreteContext ctx, Planetesimal node) {
		sweep(ctx, node, 0, 0);
	}

	// the mass density of the dust at a given distance from the center
	private static double discDensityAt(AccreteContext ctx, double d) {
		// inflection point
		final var k = 0.25 * Math.pow(ctx.stellarMassMsol, 0.5);
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
		if (d <= maskLimitL) {
			mask = 0;
		} else if (d >= maskLimitH) {
			mask = 1;
		} else {
			// m\left(x\right)=\frac{x^{kn}}{k^{kn}+x^{kn}}\left\{x\ge0\right\}
			mask = Math.pow(d, kn) / (Math.pow(k, kn) + Math.pow(d, kn));
		}

		// f\left(x\right)=Ee^{-ax^{\frac{1}{N}}}\left\{x\ge0\right\}
		final var density = ctx.params.eccentricityCoefficient * Math.sqrt(ctx.stellarMassMsol)
				* Math.exp(-ctx.params.dustDensityAlpha * Math.pow(d, 1 / ctx.params.dustDensityN));

		return mask * density;
	}

}
