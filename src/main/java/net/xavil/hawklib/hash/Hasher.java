package net.xavil.hawklib.hash;

public interface Hasher {

	Hasher appendLong(long value);

	long currentHash();

	default int currentHashInt() {
		final long hash = currentHash();
		final int hi = (int) (hash >>> 32);
		final int lo = (int) hash;
		return hi ^ lo;
	}

	default Hasher append(Object value) {
		if (value instanceof Hashable hashable) {
			hashable.appendHash(this);
		} else {
			appendLong(value.hashCode());
		}
		return this;
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
