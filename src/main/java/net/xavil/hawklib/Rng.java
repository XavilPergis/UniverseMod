package net.xavil.hawklib;

import java.util.Random;

import net.minecraft.util.Mth;
import net.xavil.hawklib.math.Interval;

public interface Rng {

	int uniformInt(int minBound, int maxBound);

	long uniformLong(long minBound, long maxBound);

	double uniformDouble(double minBound, double maxBound);

	double normalDouble(double mean, double standardDeviation);

	default double weightedDouble(double exponent, double minValue, double maxValue) {
		return Mth.lerp(Math.pow(uniformDouble(), exponent), minValue, maxValue);
	}

	default double uniformDoubleAround(double center, double spread) {
		return uniformDouble(center - spread, center + spread);
	}

	default double uniformDouble() {
		return uniformDouble(0, 1);
	}

	default int uniformInt() {
		return uniformInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	default long uniformLong() {
		return uniformLong(Long.MIN_VALUE, Long.MAX_VALUE);
	}

	default double uniformDouble(Interval interval) {
		return uniformDouble(interval.lower, interval.higher);
	}

	default boolean chance(double chance) {
		return uniformDouble() <= chance;
	}

	static Rng fromSeed(long seed) {
		return wrap(new Random(seed));
	}

	static Rng wrap(Random random) {
		return new Rng() {
			@Override
			public double uniformDouble(double minBound, double maxBound) {
				if (minBound >= maxBound)
					maxBound = Math.nextUp(minBound);
				return random.nextDouble(minBound, maxBound);
			}

			@Override
			public double normalDouble(double mean, double standardDeviation) {
				return random.nextGaussian(mean, standardDeviation);
			}

			@Override
			public int uniformInt(int minBound, int maxBound) {
				if (minBound == maxBound)
					return minBound;
				return random.nextInt(minBound, maxBound);
			}

			@Override
			public int uniformInt() {
				return random.nextInt();
			}

			@Override
			public long uniformLong(long minBound, long maxBound) {
				if (minBound == maxBound)
					return minBound;
				return random.nextLong(minBound, maxBound);
			}

			@Override
			public long uniformLong() {
				return random.nextLong();
			}
		};
	}

}
