package net.xavil.hawklib.collections.iterator;

import net.xavil.hawklib.MaybeInt;

public record SizeHint(int lowerBound, MaybeInt upperBound) {
	public static final SizeHint UNKNOWN = new SizeHint(0, MaybeInt.none());
	public static final SizeHint ZERO = exactly(0);

	public static SizeHint exactly(int bound) {
		return new SizeHint(bound, MaybeInt.some(bound));
	}

	public boolean isExact() {
		return this.upperBound.innerEquals(this.lowerBound);
	}

	public SizeHint withLowerBound(int lowerBound) {
		return new SizeHint(lowerBound, upperBound);
	}

	public SizeHint withUpperBound(int upperBound) {
		return withUpperBound(MaybeInt.some(upperBound));
	}

	public SizeHint withUpperBound(MaybeInt upperBound) {
		return new SizeHint(lowerBound, upperBound);
	}
}

