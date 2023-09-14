package net.xavil.ultraviolet.common.universe.system.gen;

import java.util.Comparator;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.SortingStrategy;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.math.Interval;
import net.xavil.ultraviolet.Mod;

public class DustBands {

	private static final class Band {
		public final Interval interval;
		// assume the band's mass is distributed uniformly throughout the band - not
		// really accurate, but maybe it'll be fine lol
		public double mass;

		public Band(Interval interval, double mass) {
			this.interval = interval;
			this.mass = mass;
		}
	}

	private static final int INITIAL_BAND_RESOLUTION = 256;
	private final MutableList<Band> gasBands = new Vector<>();
	private final MutableList<Band> dustBands = new Vector<>();

	public DustBands(AccreteContext ctx, Interval initialInterval, double initialMass) {
		initialMass *= Units.Msol_PER_Yg;
		final var md = ctx.systemMetallicity * initialMass;
		final var mg = initialMass - md;
		setupBands(ctx, initialInterval, md, this.dustBands);
		setupBands(ctx, initialInterval, mg, this.gasBands);
	}

	private void setupBands(AccreteContext ctx, Interval initialInterval, double initialMass, MutableList<Band> bands) {
		final double dr = initialInterval.size() / INITIAL_BAND_RESOLUTION;
		double prev = discDensityAt(ctx, initialInterval.lower);

		double totalMass = 0;
		for (int i = 0; i < INITIAL_BAND_RESOLUTION; ++i) {
			final var tl = i / (double) INITIAL_BAND_RESOLUTION;
			final var rl = Mth.lerp(tl, initialInterval.lower, initialInterval.higher);
			final var rh = rl + dr;

			final var cur = discDensityAt(ctx, rh);
			final var discArea = Math.PI * (Mth.square(rh) - Mth.square(rl));
			final var bandMass = discArea * (prev + cur) / 2.0;

			Assert.isTrue(!Double.isNaN(bandMass));

			totalMass += bandMass;
			bands.push(new Band(new Interval(rl, rh), bandMass));

			prev = cur;
		}

		// scale the mass of all bands so that the sum of all their masses becomes
		// `initialMass`.
		for (final var band : bands.iterable()) {
			band.mass *= initialMass / totalMass;
		}

		double tmp = 0.0;
		for (final var band : bands.iterable()) {
			tmp += band.mass;
		}
		Mod.LOGGER.info("set up dust bands: wantedMass={}, actualMass={}", initialMass, tmp);
	}

	private static double stellarWindDistanceFactor(double d) {
		return Math.pow(2, -d);
	}

	public void step(ProtoplanetaryDisc disc, double dt) {
		final var prevDustBands = new Vector<>(this.dustBands);
		final var prevGasBands = new Vector<>(this.gasBands);

		final var dustStep = 0.001 * dt * Math.sqrt(disc.ctx.stellarLuminosityLsol / 10);
		final var gasStep = 0.07 * dt * Math.sqrt(disc.ctx.stellarLuminosityLsol / 10);

		this.dustBands.clear();
		this.gasBands.clear();

		for (final var band : prevDustBands.iterable()) {
			final double bl = band.interval.lower, bh = band.interval.higher;
			this.dustBands.push(new Band(new Interval(
					bl + stellarWindDistanceFactor(bl) * dustStep,
					bh + stellarWindDistanceFactor(bh) * dustStep), band.mass));
		}

		for (final var band : prevGasBands.iterable()) {
			final double bl = band.interval.lower, bh = band.interval.higher;
			this.gasBands.push(new Band(new Interval(
					bl + stellarWindDistanceFactor(bl) * gasStep,
					bh + stellarWindDistanceFactor(bh) * gasStep), band.mass));
		}

		this.dustBands.sort(SortingStrategy.UNSTABLE | SortingStrategy.ALMOST_SORTED,
				Comparator.comparingDouble(b -> b.interval.lower));
		this.gasBands.sort(SortingStrategy.UNSTABLE | SortingStrategy.ALMOST_SORTED,
				Comparator.comparingDouble(b -> b.interval.lower));
	}

	public boolean hasDust(Interval interval) {
		return this.dustBands.iter().any(band -> band.interval.intersects(interval));
	}

	public double removeMaterial(Interval interval, MutableList<Band> bands) {
		double accumulatedMass = 0.0;

		final var prevBands = new Vector<>(bands);
		bands.clear();

		for (final var band : prevBands.iterable()) {
			if (!band.interval.intersects(interval)) {
				bands.push(band);
				continue;
			}
			final var removalInterval = band.interval.intersection(interval);
			accumulatedMass += band.mass * removalInterval.size() / band.interval.size();
			if (band.interval.contains(interval.lower)) {
				final var innerInterval = new Interval(band.interval.lower, interval.lower);
				final var bandMass = band.mass * innerInterval.size() / band.interval.size();
				bands.push(new Band(innerInterval, bandMass));
			}
			if (band.interval.contains(interval.higher)) {
				final var outerInterval = new Interval(interval.higher, band.interval.higher);
				final var bandMass = band.mass * outerInterval.size() / band.interval.size();
				bands.push(new Band(outerInterval, bandMass));
			}
		}

		return accumulatedMass;
	}

	public void sweep(AccreteContext ctx, Planetesimal node) {
		final var dustSweepInterval = node.sweptDustLimits();
		final var criticalMass = node.criticalMass();

		double accumulatedMass = removeMaterial(dustSweepInterval, this.dustBands);

		if (node.mass + accumulatedMass >= criticalMass) {
			final var r = node.orbitalShape.semiMajor();
			final var m = Math.sqrt(criticalMass / node.mass);

			final var l = r - (m / (m + 1)) * (r - dustSweepInterval.lower);
			final var h = r + (m / (m + 1)) * (dustSweepInterval.higher - r);

			final var gasSweepInterval = new Interval(l, h);

			final var gasMass = removeMaterial(gasSweepInterval, this.gasBands);
			node.sweptGas |= gasMass > 0.0;
			accumulatedMass += gasMass;
		}

		node.mass += accumulatedMass;
		node.stellarProperties.load(node.mass * Units.Msol_PER_Yg, node.age);
	}

	// the "height factor" of the disc at a given distance from the center
	// TODO: probably make this return the actual disc height
	private static double discHeightAt(AccreteContext ctx, double d) {
		return 1;
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
