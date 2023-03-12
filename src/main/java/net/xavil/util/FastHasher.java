package net.xavil.util;

public class FastHasher implements Hasher {

	private static final long C1 = 0x87c37b91114253d5L;
	private static final long C2 = 0x4cf5ad432745937fL;
	private long currentHash;

	private static long rol(long x, long r) {
		return (x << r) | (x >>> (64 - r));
	}

	private FastHasher(long seed) {
		this.currentHash = seed;
	}

	public static FastHasher create() {
		return withSeed(0);
	}

	public static FastHasher withSeed(long seed) {
		return new FastHasher(seed);
	}

	@Override
	public FastHasher appendLong(long value) {
		long k = value;
		k *= C1;
		k = rol(k, 31);
		k *= C2;
		this.currentHash ^= k;
		return this;
	}

	@Override
	public long currentHash() {
		long k = this.currentHash;
		k ^= k >> 33;
		k *= 0xff51afd7ed558ccdL;
		k ^= k >> 33;
		k *= 0xc4ceb9fe1a85ec53L;
		k ^= k >> 33;
		return k;
	}

}