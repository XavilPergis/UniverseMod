package net.xavil.hawklib;

import net.minecraft.util.Mth;

/**
 * A source of pseudo-random numbers that is not sensitive to the ordering or
 * amount of rng calls, good for ensuring the stability of
 * procedurally-generated assets while still allowing for their evolution.
 */
public final class StableRandom {
	public final long seed;

	public StableRandom(long seed) {
		this.seed = scramble(seed);
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
	public static long scramble(long value) {
		long k = value;
		k *= 0xff51afd7ed558ccdL;
		k ^= k >>> 32;
		k *= 0xff51afd7ed558ccdL;
		k += k >>> 32;
		return k;
	}

	public StableRandom split(String key) {
		return new StableRandom(uniformLong(key));
	}

	public StableRandom split(long key) {
		return new StableRandom(uniformLong(key));
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
		return this.seed ^ scramble(key.hashCode());
	}

	public long uniformLong(long key) {
		return this.seed ^ scramble(key);
	}

	public int uniformInt(String key) {
		final var value = uniformLong(key);
		return ((int) (value >>> 32)) ^ (int) value;
	}

	public double uniformDouble(String key) {
		// this is basically what Java's Random.nextDouble() does.
		return 0x1.0p-53 * (uniformLong(key) >>> 11);
	}

	public double uniformDouble(String key, double min, double max) {
		return Mth.lerp(uniformDouble(key), min, max);
	}

	public double weightedDouble(String key, double exponent) {
		return Math.pow(uniformDouble(key), exponent);
	}

	public double weightedDouble(String key, double exponent, double min, double max) {
		return Mth.lerp(weightedDouble(key, exponent), min, max);
	}

	public <E extends Enum<?>> E uniformEnum(String key, Class<E> clazz) {
		final E[] variants = clazz.getEnumConstants();
		return variants.length == 0 ? null : variants[uniformInt(key) % variants.length];
	}

	public boolean chance(String key, double probability) {
		return uniformDouble(key) <= probability;
	}
}
