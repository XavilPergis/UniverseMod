package net.xavil.util.iterator;

import java.util.OptionalInt;

public record SizeHint(int lowerBound, OptionalInt upperBound) {
	public static final SizeHint UNKNOWN = new SizeHint(0, OptionalInt.empty());
	public static final SizeHint ZERO = exactly(0);

	public static SizeHint exactly(int bound) {
		return new SizeHint(bound, OptionalInt.of(bound));
	}

	public SizeHint withLowerBound(int lowerBound) {
		return new SizeHint(lowerBound, upperBound);
	}

	public SizeHint withUpperBound(int upperBound) {
		return withUpperBound(OptionalInt.of(upperBound));
	}

	public SizeHint withUpperBound(OptionalInt upperBound) {
		return new SizeHint(lowerBound, upperBound);
	}
}

