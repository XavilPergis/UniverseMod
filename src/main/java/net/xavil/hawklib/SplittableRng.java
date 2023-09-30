package net.xavil.hawklib;

import net.minecraft.util.Mth;

public final class SplittableRng {
	private long seed;

	private long[] seedStack = null;
	private int seedStackSize;

	public SplittableRng(long seed) {
		this.seed = scramble(seed);
	}

	public void setSeed(long seed) {
		this.seed = scramble(seed);
	}

	public void setSeedRaw(long seed) {
		this.seed = seed;
	}

	public void advance() {
		// equivalent to `advanceWith(0)`
		this.seed = scramble(this.seed);
	}

	public void advanceWith(long key) {
		this.seed = uniformLong(key);
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
		this.seedStack[this.seedStackSize++] = this.seed;
		this.seed = uniformLong(key);
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
		this.seed = this.seedStack[--this.seedStackSize];
	}

	// it's important that this is not called with the current rng seed!
	public long uniformLong(long key) {
		return scramble(this.seed ^ key);
	}

	public int uniformInt(long key) {
		final var value = uniformLong(key);
		// assumes that the top and bottom 32 bits are equally well-mixed
		return ((int) (value >>> 32)) ^ (int) value;
	}

	public double uniformDouble(long key) {
		// this is basically what Java's Random.nextDouble() does.
		return 0x1.0p-53 * (uniformLong(key) >>> 11);
	}

	public double uniformDouble(long key, double min, double max) {
		return Mth.lerp(uniformDouble(key), min, max);
	}

	public double weightedDouble(long key, double exponent) {
		return Math.pow(uniformDouble(key), exponent);
	}

	public double weightedDouble(long key, double exponent, double min, double max) {
		return Mth.lerp(weightedDouble(key, exponent), min, max);
	}

	public <E extends Enum<?>> E uniformEnum(long key, Class<E> clazz) {
		final E[] variants = clazz.getEnumConstants();
		return variants.length == 0 ? null : variants[uniformInt(key) % variants.length];
	}

	public boolean chance(long key, double probability) {
		return uniformDouble(key) <= probability;
	}

	public Rng rng(long key) {
		return Rng.fromSeed(uniformLong(key));
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

	public int uniformInt(String key) {
		return uniformInt(key.hashCode());
	}

	public double uniformDouble(String key) {
		return uniformDouble(key.hashCode());
	}

	public double uniformDouble(String key, double min, double max) {
		return uniformDouble(key.hashCode(), min, max);
	}

	public double weightedDouble(String key, double exponent) {
		return weightedDouble(key.hashCode(), exponent);
	}

	public double weightedDouble(String key, double exponent, double min, double max) {
		return weightedDouble(key.hashCode(), exponent, min, max);
	}

	public <E extends Enum<?>> E uniformEnum(String key, Class<E> clazz) {
		return uniformEnum(key.hashCode(), clazz);
	}

	public boolean chance(String key, double probability) {
		return chance(key.hashCode(), probability);
	}

	public Rng rng(String key) {
		return rng(key.hashCode());
	}

}
