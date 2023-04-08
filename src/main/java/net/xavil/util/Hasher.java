package net.xavil.util;

public interface Hasher {

	Hasher appendLong(long value);

	long currentHash();

	default Hasher append(Hashable hashable) {
		hashable.appendHash(this);
		return this;
	}

	default int currentHashInt() {
		long h = currentHash();
		return ((int) (h >>> 32)) ^ ((int) h);
	}

	default Hasher appendInt(int value) {
		return appendLong(value);
	}

	default Hasher appendFloat(float value) {
		return appendInt(Float.floatToRawIntBits(value));
	}

	default Hasher appendDouble(double value) {
		return appendLong(Double.doubleToRawLongBits(value));
	}

}
