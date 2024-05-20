package net.xavil.hawklib;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.math.Interval;

/**
 * A source of pseudo-random numbers that is not sensitive to the ordering or
 * amount of rng calls, good for ensuring the stability of
 * procedurally-generated assets while still allowing for their evolution.
 */
public final class SplittableRng {
	private long current;

	private long[] seedStack = null;
	private int seedStackSize;

	public SplittableRng(long seed) {
		this.current = scramble(seed);
	}

	public void setCurrent(long seed) {
		this.current = scramble(seed);
	}

	public void setSeedRaw(long seed) {
		this.current = seed;
	}

	public void advance() {
		// equivalent to `advanceWith(0)`
		this.current = scramble(this.current);
	}

	public void advanceWith(long key) {
		this.current = uniformLong(key);
	}

	public void advanceWith(String key) {
		this.current = uniformLong(key);
	}

	// https://jonkagstrom.com/bit-mixer-construction/
	/**
	 * Scrambles the input bits of the given integer. This should agitate the input
	 * value so that small changes in the input result in random-looking changes in
	 * the output.
	 * 
	 * @param value The value to scramble.
	 * @return The scrambled value.
	 */
	// NOTE: i have not tested this extensively, and might have issues with cycles
	// or randomization when used on its output repeatedly. Seems to be fine for
	// most of my use cases, though.
	public static long scramble(long value) {
		long k = value;
		k *= 0xff51afd7ed558ccdL;
		k ^= k >>> 32;
		k *= 0xff51afd7ed558ccdL;
		k += k >>> 32;
		return k;
	}

	private int newCapacity(int currentCapacity) {
		if (currentCapacity == 0)
			return 4;
		if (currentCapacity < 1024)
			return Mth.floor(currentCapacity * 1.5) + 1;
		return currentCapacity * 2;
	}

	/**
	 * Saves the current RNG seed to the top of the seed stack and installs a new
	 * one based on the given key.
	 * 
	 * @param key The key to derive the new seed from
	 */
	public void push(long key) {
		if (this.seedStack == null || this.seedStackSize == this.seedStack.length) {
			final var oldCap = this.seedStack == null ? 0 : this.seedStack.length;
			final var newStack = new long[newCapacity(oldCap)];
			if (this.seedStack != null)
				System.arraycopy(this.seedStack, 0, newStack, 0, this.seedStackSize);
			this.seedStack = newStack;
		}
		this.seedStack[this.seedStackSize++] = this.current;
		this.current = uniformLong(key);
	}

	public void push(String key) {
		push(key.hashCode());
	}

	/**
	 * Restores the RNG seed from the top of the seed stack, removing it in the
	 * process.
	 */
	public void pop() {
		if (this.seedStackSize == 0)
			throw new IllegalStateException("cannot pop from empty rng stack!");
		this.current = this.seedStack[--this.seedStackSize];
	}

	// it's important that this is not called with the current rng seed! Or like, it
	// *can* if its randomly picked, but `uniformLong(current)` will always produce
	// the exact same result no matter what the internal state is.
	public long uniformLong(long key) {
		return scramble(this.current ^ key);
	}

	/**
	 * This is meant to be used with string literals, because strings cache their
	 * hashcode after it's computed for the first time, and string literals are
	 * interned, so that the same literal, when used twice, will not have to walk
	 * the string and recompute the hash again.
	 * 
	 * It is strongly advised not to use dynamically created strings in a hot path,
	 * unless that string instance is reused. {@link #split(long)} should be used if
	 * you have a sequence of items that all need their own random values
	 * 
	 * @param key The key that is used to derive a random number.
	 * @return The value associated with the key for the current seed value.
	 */
	public long uniformLong(String key) {
		// it looks like Java's hashCode() implementations kind of suck, so we scramble
		// them here just to make sure that they are of sufficient quality.
		return uniformLong(key.hashCode());
	}

	public long uniformLong(long key, long min, long max) {
		if (min >= max)
			throw new IllegalArgumentException(String.format(
					"range error: cannot draw uniform number from empty range, bounds are [{}, {}]",
					min, max));
		if (min == max - 1)
			return min;

		// check if finding the distance between min and max would overflow. This can
		// only happen if min is negative, because otherwise the max distance is
		// Long.MAX_VALUE or less.
		if (min < 0 && min + Long.MAX_VALUE >= 0) {
			throw new IllegalArgumentException(String.format(
					"range error: range is too large, the difference between the bounds is greater than the type's max value of {}. The bounds are [{}, {}]",
					Long.MAX_VALUE, min, max));
		}

		final var dist = max - min;
		// largest power of 2 greater than `dist`
		final long mask = (1L << (Long.SIZE - Long.numberOfLeadingZeros(dist - 1L))) - 1L;

		// generate number in [0, 2^ceil(log2(dist))] until it falls within range.
		//
		// directly wrapping sampled numbers with modulo doesn't entirely work, since
		// the range of sampled numbers doesn't wrap around our output range perfectly
		// unless the size of the output range is a factor of the input range. Since our
		// input range's size is always a power of 2, the only factors are other powers
		// of two, so we don't have to redraw those.
		long cur = uniformLong(key) & mask;
		while (cur > dist) {
			cur = scramble(cur) & mask;
		}

		return cur + min;
	}

	public long uniformLong(String key, long min, long max) {
		return uniformLong(key.hashCode(), min, max);
	}

	public int uniformInt(long key) {
		final var value = uniformLong(key);
		// assumes that the top and bottom 32 bits are equally well-mixed
		return ((int) (value >>> 32)) ^ (int) value;
	}

	public int uniformInt(String key) {
		return uniformInt(key.hashCode());
	}

	public int uniformInt(long key, int min, int max) {
		if (min >= max)
			throw new IllegalArgumentException(String.format(
					"range error: cannot draw uniform number from empty range, bounds are [{}, {}]",
					min, max));
		if (min == max - 1)
			return min;

		// check if finding the distance between min and max would overflow. This can
		// only happen if min is negative, because otherwise the max distance is
		// Integer.MAX_VALUE or less.
		if (min < 0 && min + Integer.MAX_VALUE >= 0) {
			throw new IllegalArgumentException(String.format(
					"range error: range is too large, the difference between the bounds is greater than the type's max value of {}. The bounds are [{}, {}]",
					Integer.MAX_VALUE, min, max));
		}

		final var dist = max - min;
		// largest power of 2 greater than `dist`
		final int mask = (1 << (Integer.SIZE - Integer.numberOfLeadingZeros(dist - 1))) - 1;

		// generate number in [0, 2^ceil(log2(dist))] until it falls within range.
		//
		// directly wrapping sampled numbers with modulo doesn't entirely work, since
		// the range of sampled numbers doesn't wrap around our output range perfectly
		// unless the size of the output range is a factor of the input range. Since our
		// input range's size is always a power of 2, the only factors are other powers
		// of two, so we don't have to redraw those.
		int cur = uniformInt(key) & mask;
		while (cur > dist) {
			cur = ((int) scramble(cur)) & mask;
		}

		return cur + min;
	}

	public long uniformInt(String key, int min, int max) {
		return uniformInt(key.hashCode(), min, max);
	}

	public double uniformDouble(long key) {
		// this is basically what Java's Random.nextDouble() does.
		return 0x1.0p-53 * (uniformLong(key) >>> 11);
	}

	public double uniformDouble(String key) {
		return uniformDouble(key.hashCode());
	}

	public double uniformDouble(long key, double min, double max) {
		return Mth.lerp(uniformDouble(key), min, max);
	}

	public double uniformDouble(String key, double min, double max) {
		return uniformDouble(key.hashCode(), min, max);
	}

	public double uniformDouble(long key, Interval interval) {
		return Mth.lerp(uniformDouble(key), interval.min, interval.max);
	}

	public double uniformDouble(String key, Interval interval) {
		return uniformDouble(key.hashCode(), interval.min, interval.max);
	}

	public <E extends Enum<?>> E uniformEnum(long key, Class<E> clazz) {
		final E[] variants = clazz.getEnumConstants();
		return variants.length == 0 ? null
				: variants[uniformInt(key, 0, variants.length - 1)];
	}

	public <E extends Enum<?>> E uniformEnum(String key, Class<E> clazz) {
		return uniformEnum(key.hashCode(), clazz);
	}

	public boolean chance(long key, double probability) {
		return uniformDouble(key) <= probability;
	}

	public boolean chance(String key, double probability) {
		return chance(key.hashCode(), probability);
	}

	public Rng rng(long key) {
		return Rng.fromSeed(uniformLong(key));
	}

	public Rng rng(String key) {
		return rng(key.hashCode());
	}

	@Nullable
	public <T> T pick(long key, ImmutableList<T> elements) {
		if (elements.isEmpty())
			return null;
		return elements.get(this.uniformInt(key, 0, elements.size()));
	}

	@Nullable
	public <T> T pick(long key, T[] elements) {
		if (elements.length == 0)
			return null;
		return elements[this.uniformInt(key, 0, elements.length)];
	}

	@Nullable
	public <T> T pick(String key, ImmutableList<T> elements) {
		return pick(key.hashCode(), elements);
	}

	@Nullable
	public <T> T pick(String key, T[] elements) {
		return pick(key.hashCode(), elements);
	}

	//

	public double weightedDouble(long key, ProbabilityDistribution distribution) {
		return distribution.pick(uniformDouble(key));
	}

	public double weightedDouble(String key, ProbabilityDistribution distribution) {
		return weightedDouble(key.hashCode(), distribution);
	}

	public double weightedDouble(long key, ProbabilityDistribution distribution, double min, double max) {
		return Mth.lerp(distribution.pick(uniformDouble(key)), min, max);
	}

	public double weightedDouble(long key, ProbabilityDistribution distribution, Interval interval) {
		return interval.lerp(distribution.pick(uniformDouble(key)));
	}

	public double weightedDouble(long key, double exponent) {
		return Math.pow(uniformDouble(key), exponent);
	}

	public double weightedDouble(String key, double exponent) {
		return weightedDouble(key.hashCode(), exponent);
	}

	public double weightedDouble(long key, double exponent, double min, double max) {
		return Mth.lerp(weightedDouble(key, exponent), min, max);
	}

	public double weightedDouble(String key, double exponent, double min, double max) {
		return weightedDouble(key.hashCode(), exponent, min, max);
	}

	public double weightedDouble(String key, double exponent, Interval interval) {
		return weightedDouble(key.hashCode(), exponent, interval.min, interval.max);
	}

}
