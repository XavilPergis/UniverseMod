package net.xavil.ultraviolet.common.universe.system.gen;

import java.util.ArrayList;
import java.util.List;

import net.xavil.hawklib.Rng;
import net.xavil.hawklib.math.Interval;

public class DustBands {

	public record Band(Interval interval, boolean hasGas, boolean hasDust) {

		public boolean canMergeWithNext(Band band) {
			return this.interval.higher() == band.interval.lower()
					&& this.hasGas == band.hasGas
					&& this.hasDust == band.hasDust;
		}

	}

	public final AccreteDebugEvent.Consumer debugConsumer;
	public final List<Band> bands = new ArrayList<>();

	public DustBands(Interval initialInterval, AccreteDebugEvent.Consumer debugConsumer) {
		this.bands.add(new Band(initialInterval, true, true));
		this.debugConsumer = debugConsumer;
	}

	public boolean hasDust(Interval interval) {
		return bands.stream().anyMatch(band -> band.hasDust && band.interval.intersects(interval));
	}

	public double pickDusty(Rng rng, Interval limitInterval) {
		var candidateBands = new ArrayList<Band>();
		for (var band : this.bands) {
			if (!band.interval.intersects(limitInterval))
				continue;
			if (band.hasDust)
				candidateBands.add(band);
		}
		if (candidateBands.isEmpty())
			return Double.NaN;
		final var chosenBand = candidateBands.get(rng.uniformInt(0, candidateBands.size() - 1));
		final var interval = limitInterval.intersection(chosenBand.interval);
		return rng.uniformDouble(interval);
	}

	public void removeMaterial(Interval interval, boolean removeGas, boolean removeDust) {
		final var prevBands = new ArrayList<>(bands);
		this.bands.clear();
		boolean changedAnything = false;
		for (var band : prevBands) {
			if (!band.interval.intersects(interval)) {
				this.bands.add(band);
			} else if (interval.contains(band.interval)) {
				boolean newGas = !removeGas && band.hasGas, newDust = !removeDust && band.hasDust;
				if (newGas || newDust)
					this.bands.add(new Band(band.interval, newGas, newDust));
				changedAnything = true;
			} else if (band.interval.contains(interval)) {
				boolean newGas = !removeGas && band.hasGas, newDust = !removeDust && band.hasDust;
				var innerInterval = new Interval(band.interval.lower(), interval.lower());
				var outerInterval = new Interval(interval.higher(), band.interval.higher());
				this.bands.add(new Band(innerInterval, band.hasGas, band.hasDust));
				if (newGas || newDust)
					this.bands.add(new Band(interval, newGas, newDust));
				this.bands.add(new Band(outerInterval, band.hasGas, band.hasDust));
				changedAnything = true;
			} else if (band.interval.contains(interval.lower())) {
				boolean newGas = !removeGas && band.hasGas, newDust = !removeDust && band.hasDust;
				var innerInterval = new Interval(band.interval.lower(), interval.lower());
				this.bands.add(new Band(innerInterval, band.hasGas, band.hasDust));
				if (newGas || newDust) {
					var midInterval = new Interval(interval.lower(), band.interval.higher());
					this.bands.add(new Band(midInterval, true, false));
				}
				changedAnything = true;
			} else if (band.interval.contains(interval.higher())) {
				boolean newGas = !removeGas && band.hasGas, newDust = !removeDust && band.hasDust;
				if (newGas || newDust) {
					var midInterval = new Interval(band.interval.lower(), interval.higher());
					this.bands.add(new Band(midInterval, true, false));
				}
				var outerInterval = new Interval(interval.higher(), band.interval.higher());
				this.bands.add(new Band(outerInterval, band.hasGas, band.hasDust));
				changedAnything = true;
			}
		}

		if (changedAnything) {
			this.debugConsumer.accept(new AccreteDebugEvent.Sweep(interval, removeGas, removeDust));
			defragment();
		}
	}

	public void removeMaterial(Interval interval, boolean removeGas) {
		removeMaterial(interval, removeGas, true);
	}

	private void defragment() {
		final var prevBands = new ArrayList<>(bands);
		this.bands.clear();

		var i = 0;
		while (i < prevBands.size()) {
			var currentBand = prevBands.get(i++);
			while (i < prevBands.size()) {
				var nextBand = prevBands.get(i);
				if (!currentBand.canMergeWithNext(nextBand))
					break;
				i += 1;
				var mergedInterval = new Interval(currentBand.interval.lower(), nextBand.interval.higher());
				currentBand = new Band(mergedInterval, currentBand.hasGas, currentBand.hasDust);
			}
			this.bands.add(currentBand);
		}
	}

	public double sweep(AccreteContext ctx, Planetesimal planetesimal) {

		double accumulatedMass = 0;
		for (var band : this.bands) {
			var sweptInterval = planetesimal.sweptDustLimits();

			if (!band.hasDust || !sweptInterval.intersects(band.interval))
				continue;

			double outerWidth = Math.max(0, sweptInterval.higher() - band.interval.higher());
			double innerWidth = Math.max(0, band.interval.lower() - sweptInterval.lower());

			double intersectedWidth = sweptInterval.size() - outerWidth - innerWidth;

			double term1 = 4 * Math.PI * Math.pow(planetesimal.getOrbitalShape().semiMajor(), 2);
			// TODO: the `outerWidth - innerWidth` here looks sussy, i dont understand why
			// its like that in the implementation im stealing everything from
			double term2 = 1
					- planetesimal.getOrbitalShape().eccentricity() * (outerWidth - innerWidth) / sweptInterval.size();
			double volume = term1 * term2 * intersectedWidth * Planetesimal.reducedMass(planetesimal.getMass());

			double density = getDensity(ctx, planetesimal, band.hasGas);

			accumulatedMass += density * volume;
		}

		final var sweptInterval = planetesimal.sweptDustLimits();
		removeMaterial(sweptInterval, planetesimal.canSweepGas());

		return accumulatedMass;
	}

	private static double getDensity(AccreteContext ctx, Planetesimal planetesimal, boolean hasGas) {
		double dustDensity = dustDensity(ctx, planetesimal.getOrbitalShape().semiMajor());
		double criticalMass = planetesimal.criticalMass();
		if (!hasGas || !planetesimal.canSweepGas())
			return dustDensity;

		double k = ctx.params.dustToGasRatio;
		return k * dustDensity / (1 + Math.sqrt(criticalMass / planetesimal.getMass()) * (k - 1));
	}

	public static double dustDensity(AccreteContext ctx, double orbitalRadius) {
		return ctx.params.eccentricityCoefficient * Math.sqrt(ctx.stellarMassMsol)
				* Math.exp(-ctx.params.dustDensityAlpha * Math.pow(orbitalRadius, 1 / ctx.params.dustDensityN));
	}

}
