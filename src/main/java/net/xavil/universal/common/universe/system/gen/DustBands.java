package net.xavil.universal.common.universe.system.gen;

import java.util.ArrayList;
import java.util.List;

public class DustBands {

	private record Band(Interval interval, boolean hasGas, boolean hasDust) {

		public boolean canMergeWithNext(Band band) {
			return this.interval.outer() == band.interval.inner()
					&& this.hasGas == band.hasGas
					&& this.hasDust == band.hasDust;
		}

	}

	private final List<Band> bands = new ArrayList<>();

	public DustBands(Interval initialInterval) {
		this.bands.add(new Band(initialInterval, true, true));
	}

	public boolean hasDust(Interval interval) {
		return bands.stream().anyMatch(band -> band.hasDust && band.interval.intersectsWith(interval));
	}

	public void removeMaterial(Interval interval, boolean removeGas) {
		final var prevBands = new ArrayList<>(bands);
		this.bands.clear();
		for (var band : prevBands) {
			if (!band.interval.intersectsWith(interval)) {
				this.bands.add(band);
			} else if (interval.contains(band.interval)) {
				if (!removeGas)
					this.bands.add(new Band(band.interval, true, false));
			} else if (band.interval.contains(interval)) {
				var innerInterval = new Interval(band.interval.inner(), interval.inner());
				var outerInterval = new Interval(interval.outer(), band.interval.outer());
				this.bands.add(new Band(innerInterval, band.hasGas, band.hasDust));
				if (!removeGas)
					this.bands.add(new Band(interval, true, false));
				this.bands.add(new Band(outerInterval, band.hasGas, band.hasDust));
			} else if (band.interval.contains(interval.inner())) {
				var innerInterval = new Interval(band.interval.inner(), interval.inner());
				this.bands.add(new Band(innerInterval, band.hasGas, band.hasDust));
				if (!removeGas) {
					var midInterval = new Interval(interval.inner(), band.interval.outer());
					this.bands.add(new Band(midInterval, true, false));
				}
			} else if (band.interval.contains(interval.outer())) {
				if (!removeGas) {
					var midInterval = new Interval(band.interval.inner(), interval.outer());
					this.bands.add(new Band(midInterval, true, false));
				}
				var outerInterval = new Interval(interval.outer(), band.interval.outer());
				this.bands.add(new Band(outerInterval, band.hasGas, band.hasDust));
			}
		}
	}

	public void defragment() {
		final var prevBands = new ArrayList<>(bands);
		this.bands.clear();

		var i = 0;
		while (i < prevBands.size()) {
			var currentBand = prevBands.get(i++);
			while (i < prevBands.size()) {
				var nextBand = prevBands.get(i++);
				if (!currentBand.canMergeWithNext(nextBand))
					break;
				var mergedInterval = new Interval(currentBand.interval.inner(), nextBand.interval.outer());
				currentBand = new Band(mergedInterval, currentBand.hasGas, currentBand.hasDust);
			}
			this.bands.add(currentBand);
		}
	}

	public double sweep(AccreteContext ctx, Planetesimal planetesimal) {

 		double accumulatedMass = 0;
		for (var band : this.bands) {
			var sweptInterval = planetesimal.sweptDustLimits(ctx);
			// var intersectedWidth = sweptInterval.intersection(band.interval);

			if (!band.hasDust || !sweptInterval.intersectsWith(band.interval))
				continue;

			double outerWidth = Math.max(0, sweptInterval.outer() - band.interval.outer());
			double innerWidth = Math.max(0, band.interval.inner() - sweptInterval.inner());

			double intersectedWidth = sweptInterval.size() - outerWidth - innerWidth;

			double term1 = 4 * Math.PI * Math.pow(planetesimal.orbitalShape.semiMajor(), 2);
			// TODO: the `outerWidth - innerWidth` here looks sussy, i dont understand why
			// its like that in the implementation im stealing everything from
			double term2 = 1 - planetesimal.orbitalShape.eccentricity() * (outerWidth - innerWidth) / sweptInterval.size();
			double volume = term1 * term2 * intersectedWidth * Planetesimal.reducedMass(planetesimal.mass);

			double density = getDensity(ctx, planetesimal, band.hasGas);

			accumulatedMass += density * volume;
		}

		var sweptGas = planetesimal.canSweepGas(ctx);
		planetesimal.isGasGiant |= sweptGas;
		removeMaterial(planetesimal.sweptDustLimits(ctx), sweptGas);

		return accumulatedMass;
	}

	private static double getDensity(AccreteContext ctx, Planetesimal planetesimal, boolean hasGas) {
		double dustDensity = dustDensity(ctx, planetesimal.orbitalShape.semiMajor());
		double criticalMass = Planetesimal.criticalMass(ctx, planetesimal.orbitalShape);
		if (!hasGas || !planetesimal.canSweepGas(ctx))
			return dustDensity;

			double k = ctx.params.dustToGasRatio;
		return k * dustDensity / (1 + Math.sqrt(criticalMass / planetesimal.mass) * (k - 1));
	}

	public static double dustDensity(AccreteContext ctx, double orbitalRadius) {
		return ctx.params.eccentricityCoefficient * Math.sqrt(ctx.stellarMassMsol)
				* Math.exp(-ctx.params.dustDensityAlpha * Math.pow(orbitalRadius, 1 / ctx.params.dustDensityN));
	}

}
