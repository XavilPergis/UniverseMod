package net.xavil.hawklib;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.math.Interval;

public interface Rng {

	int uniformInt(int minBound, int maxBound);

	long uniformLong(long minBound, long maxBound);

	double uniformDouble(double minBound, double maxBound);

	double normalDouble(double mean, double standardDeviation);

	@Nullable
	default <T> T pick(ImmutableList<T> elements) {
		if (elements.isEmpty())
			return null;
		return elements.get(this.uniformInt(0, elements.size()));
	}

	@Nullable
	default <T> T pick(T[] elements) {
		if (elements.length == 0)
			return null;
		return elements[this.uniformInt(0, elements.length)];
	}

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
		// return new Rng() {
		// 	long state = seed;

		// 	@Override
		// 	public int uniformInt() {
		// 		this.state = SplittableRng.scramble(this.state);
		// 		return (int) (this.state >>> 32);
		// 	}

		// 	@Override
		// 	public int uniformInt(int l, int h) {
		// 		final int s = h - l, m = s - 1;

		// 		// powers of 2 are fine, we just chop off some bits, and the rest are still
		// 		// randomly set.
		// 		if ((s & m) == 0)
		// 			return uniformInt() & m + l;

		// 		// range is too big to fit into an int
		// 		if (s <= 0) {
		// 			int r = uniformInt();
		// 			while (r < l || r >= h)
		// 				r = uniformInt();
		// 			return r;
		// 		}

		// 		// this will at most reject half of the input space
		// 		int r, k = (Integer.highestOneBit(s) << 1) - 1;
		// 		while ((r = (uniformInt() >>> 1) & k) > s)
		// 			;

		// 		return r + l;
		// 	}

		// 	@Override
		// 	public long uniformLong() {
		// 		this.state = SplittableRng.scramble(this.state);
		// 		return this.state;
		// 	}

		// 	@Override
		// 	public long uniformLong(long l, long h) {
		// 		final long s = h - l, m = s - 1L;

		// 		// powers of 2 are fine, we just chop off some bits, and the rest are still
		// 		// randomly set.
		// 		if ((s & m) == 0L)
		// 			return uniformLong() & m + l;

		// 		// range is too big to fit into a long
		// 		if (s <= 0L) {
		// 			long r = uniformLong();
		// 			while (r < l || r >= h)
		// 				r = uniformLong();
		// 			return r;
		// 		}

		// 		// this will at most reject half of the input space
		// 		long r, k = (Long.highestOneBit(s) << 1L) - 1L;
		// 		while ((r = (uniformLong() >>> 1L) & k) > s)
		// 			;

		// 		return r + l;
		// 	}

		// 	@Override
		// 	public double uniformDouble(double minBound, double maxBound) {
		// 		return 0x1.0p-53 * (uniformLong() >>> 11);
		// 	}

		// 	@Override
		// 	public double normalDouble(double mean, double standardDeviation) {
		// 		// TODO Auto-generated method stub
		// 		throw new UnsupportedOperationException("Unimplemented method 'normalDouble'");
		// 	}

		// };
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
