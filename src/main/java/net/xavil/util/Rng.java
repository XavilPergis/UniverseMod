package net.xavil.util;

import java.util.Random;

import net.xavil.util.math.Interval;

public interface Rng {

	int uniformInt(int minBound, int maxBound);

	double uniformDouble(double minBound, double maxBound);

	default double uniformDouble() {
		return uniformDouble(0, 1);
	}

	default double uniformInt() {
		return uniformInt(0, 1);
	}

	default double uniformDouble(Interval interval) {
		return uniformDouble(interval.lower(), interval.higher());
	}

	static Rng wrap(Random random) {
		return new Rng() {
			@Override
			public double uniformDouble(double minBound, double maxBound) {
				return random.nextDouble();
			}

			@Override
			public int uniformInt(int minBound, int maxBound) {
				return random.nextInt();
			}
		};
	}

}
